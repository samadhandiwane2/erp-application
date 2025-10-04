package com.erp.admin.controller;

import com.erp.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/debug/database")
@RequiredArgsConstructor
@Slf4j
public class DatabaseDebugController {

    private final DataSource dataSource;

    @GetMapping("/migrations")
    public ApiResponse<Map<String, Object>> checkMigrations() {
        Map<String, Object> result = new HashMap<>();

        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

            // Check master migrations
            Resource[] masterMigrations = resolver.getResources("classpath:db/migration/master/*.sql");
            List<String> masterFiles = new ArrayList<>();
            for (Resource resource : masterMigrations) {
                masterFiles.add(resource.getFilename());
            }
            result.put("masterMigrations", masterFiles);

            // Check tenant migrations
            Resource[] tenantMigrations = resolver.getResources("classpath:db/migration/tenant/*.sql");
            List<String> tenantFiles = new ArrayList<>();
            for (Resource resource : tenantMigrations) {
                tenantFiles.add(resource.getFilename());
            }
            result.put("tenantMigrations", tenantFiles);

        } catch (Exception e) {
            log.error("Error checking migrations", e);
            result.put("error", e.getMessage());
        }

        return ApiResponse.success("Migration files check completed", result);
    }

    @GetMapping("/schema/{schemaName}")
    public ApiResponse<Map<String, Object>> inspectSchema(@PathVariable String schemaName) {
        Map<String, Object> result = new HashMap<>();

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            // Check if schema exists
            ResultSet rs = statement.executeQuery(
                    "SELECT COUNT(*) FROM information_schema.SCHEMATA WHERE SCHEMA_NAME = '" + schemaName + "'"
            );
            rs.next();
            result.put("schemaExists", rs.getInt(1) > 0);

            // List tables
            ResultSet tables = statement.executeQuery(
                    "SELECT table_name FROM information_schema.tables WHERE table_schema = '" + schemaName + "'"
            );
            List<String> tableList = new ArrayList<>();
            while (tables.next()) {
                tableList.add(tables.getString("table_name"));
            }
            result.put("tables", tableList);
            result.put("tableCount", tableList.size());

            // Check flyway history
            try {
                statement.execute("USE `" + schemaName + "`");
                ResultSet history = statement.executeQuery(
                        "SELECT version, description, success, installed_on FROM flyway_schema_history ORDER BY installed_rank"
                );
                List<Map<String, Object>> migrations = new ArrayList<>();
                while (history.next()) {
                    Map<String, Object> migration = new HashMap<>();
                    migration.put("version", history.getString("version"));
                    migration.put("description", history.getString("description"));
                    migration.put("success", history.getBoolean("success"));
                    migration.put("installedOn", history.getTimestamp("installed_on"));
                    migrations.add(migration);
                }
                result.put("flywayHistory", migrations);
            } catch (Exception e) {
                result.put("flywayHistoryError", e.getMessage());
            }

        } catch (Exception e) {
            log.error("Error inspecting schema", e);
            result.put("error", e.getMessage());
        }

        return ApiResponse.success("Schema inspection completed", result);
    }

}
