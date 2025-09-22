package com.erp.admin.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/debug/flyway")
@RequiredArgsConstructor
@Slf4j
public class FlywayDebugController {

    private final DataSource dataSource;

    @GetMapping("/check-migrations")
    public Map<String, Object> checkMigrations() {
        Map<String, Object> result = new HashMap<>();

        try {
            // Check for migration files in classpath
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

            // Check master migrations
            Resource[] masterMigrations = resolver.getResources("classpath:db/migration/master/*.sql");
            List<String> masterFiles = new ArrayList<>();
            for (Resource resource : masterMigrations) {
                masterFiles.add(resource.getFilename());
                log.info("Found master migration: {}", resource.getFilename());
            }
            result.put("masterMigrations", masterFiles);

            // Check tenant migrations
            Resource[] tenantMigrations = resolver.getResources("classpath:db/migration/tenant/*.sql");
            List<String> tenantFiles = new ArrayList<>();
            for (Resource resource : tenantMigrations) {
                tenantFiles.add(resource.getFilename());
                log.info("Found tenant migration: {}", resource.getFilename());
            }
            result.put("tenantMigrations", tenantFiles);

            // Try alternative paths
            Resource[] altMigrations = resolver.getResources("classpath*:db/migration/**/*.sql");
            List<String> altFiles = new ArrayList<>();
            for (Resource resource : altMigrations) {
                altFiles.add(resource.getURL().toString());
                log.info("Found migration at: {}", resource.getURL());
            }
            result.put("allMigrations", altFiles);

        } catch (Exception e) {
            log.error("Error checking migrations", e);
            result.put("error", e.getMessage());
        }

        return result;
    }

    @GetMapping("/check-schema/{schemaName}")
    public Map<String, Object> checkSchema(@PathVariable String schemaName) {
        Map<String, Object> result = new HashMap<>();

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            // Check if schema exists
            ResultSet rs = statement.executeQuery(
                    "SELECT COUNT(*) FROM information_schema.SCHEMATA WHERE SCHEMA_NAME = '" + schemaName + "'"
            );
            rs.next();
            result.put("schemaExists", rs.getInt(1) > 0);

            // List tables in schema
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
                        "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank"
                );
                List<Map<String, Object>> migrations = new ArrayList<>();
                while (history.next()) {
                    Map<String, Object> migration = new HashMap<>();
                    migration.put("version", history.getString("version"));
                    migration.put("description", history.getString("description"));
                    migration.put("success", history.getBoolean("success"));
                    migrations.add(migration);
                }
                result.put("flywayHistory", migrations);
            } catch (Exception e) {
                result.put("flywayHistoryError", e.getMessage());
            }

        } catch (Exception e) {
            log.error("Error checking schema", e);
            result.put("error", e.getMessage());
        }

        return result;
    }

    @PostMapping("/manual-migrate/{schemaName}")
    public Map<String, Object> manualMigrate(@PathVariable String schemaName) {
        Map<String, Object> result = new HashMap<>();

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            statement.execute("USE `" + schemaName + "`");

            // Manually execute the migration SQL
            // This is just for debugging - normally Flyway should handle this

            // Read the SQL file content (you'd need to load this from resources)
            String createTablesSql = """
                    CREATE TABLE IF NOT EXISTS academic_years (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        year_name VARCHAR(50) NOT NULL,
                        start_date DATE NOT NULL,
                        end_date DATE NOT NULL,
                        is_current BOOLEAN DEFAULT FALSE,
                        description TEXT,
                        created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        created_by BIGINT,
                        updated_by BIGINT,
                        is_active BOOLEAN DEFAULT TRUE
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
                    
                    CREATE TABLE IF NOT EXISTS classes (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        class_name VARCHAR(50) NOT NULL,
                        class_code VARCHAR(20) NOT NULL,
                        grade_level INT NOT NULL,
                        description TEXT,
                        max_students INT DEFAULT 50,
                        created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        created_by BIGINT,
                        updated_by BIGINT,
                        is_active BOOLEAN DEFAULT TRUE,
                        UNIQUE KEY uk_class_code (class_code)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
                    """;

            // Execute each statement
            for (String sql : createTablesSql.split(";")) {
                if (!sql.trim().isEmpty()) {
                    statement.execute(sql.trim());
                    log.info("Executed: {}", sql.substring(0, Math.min(50, sql.length())) + "...");
                }
            }

            result.put("success", true);
            result.put("message", "Manual migration executed for testing");

        } catch (Exception e) {
            log.error("Error in manual migration", e);
            result.put("error", e.getMessage());
        }

        return result;
    }

}
