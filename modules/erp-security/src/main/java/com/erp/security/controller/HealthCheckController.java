package com.erp.security.controller;

import com.erp.common.config.MultiTenantDataSourceConfig;
import com.erp.common.context.SchemaContext;
import com.erp.common.context.TenantContext;
import com.erp.common.dto.ApiResponse;
import com.erp.common.entity.Tenant;
import com.erp.common.repository.TenantRepository;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
@Slf4j
public class HealthCheckController {

    private final JdbcTemplate jdbcTemplate;
    private final TenantRepository tenantRepository;
    private final MultiTenantDataSourceConfig dataSourceConfig;

    @GetMapping
    public ApiResponse<Map<String, Object>> basicHealth() {
        Map<String, Object> healthInfo = new HashMap<>();
        healthInfo.put("status", "UP");
        healthInfo.put("timestamp", LocalDateTime.now());
        healthInfo.put("service", "ERP Multi-Tenant Application");
        healthInfo.put("version", "1.0.0");

        return ApiResponse.success("Service is healthy", healthInfo);
    }

    @GetMapping("/datasources")
    public ApiResponse<Map<String, Object>> checkDataSources() {
        Map<String, Object> health = new HashMap<>();

        // Test master datasource
        try {
            SchemaContext.useMasterSchema();
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            health.put("master", Map.of(
                    "status", "healthy",
                    "timestamp", LocalDateTime.now()
            ));
        } catch (Exception e) {
            health.put("master", Map.of(
                    "status", "unhealthy",
                    "error", e.getMessage(),
                    "timestamp", LocalDateTime.now()
            ));
            log.error("Master datasource health check failed", e);
        } finally {
            SchemaContext.clear();
        }

        // Test tenant datasources
        try {
            SchemaContext.useMasterSchema();
            List<Tenant> tenants = tenantRepository.findByIsActiveTrueOrderByCreatedAtDesc();

            for (Tenant tenant : tenants) {
                try {
                    TenantContext.setCurrentTenant(
                            tenant.getId(),
                            tenant.getTenantCode(),
                            tenant.getSchemaName()
                    );
                    SchemaContext.useTenantSchema();

                    jdbcTemplate.queryForObject("SELECT 1", Integer.class);
                    health.put("tenant_" + tenant.getTenantCode(), Map.of(
                            "status", "healthy",
                            "schemaName", tenant.getSchemaName(),
                            "timestamp", LocalDateTime.now()
                    ));

                } catch (Exception e) {
                    health.put("tenant_" + tenant.getTenantCode(), Map.of(
                            "status", "unhealthy",
                            "error", e.getMessage(),
                            "timestamp", LocalDateTime.now()
                    ));
                    log.error("Tenant {} datasource health check failed", tenant.getTenantCode(), e);
                } finally {
                    TenantContext.clear();
                    SchemaContext.clear();
                }
            }
        } catch (Exception e) {
            health.put("tenant_check_error", e.getMessage());
            log.error("Error checking tenant datasources", e);
        }

        return ApiResponse.success("DataSource health check completed", health);
    }

    @GetMapping("/connection-pools")
    public ApiResponse<Map<String, Object>> checkConnectionPools() {
        Map<String, Object> poolStats = new HashMap<>();
        Map<Object, Object> dataSources = dataSourceConfig.getDataSources();

        for (Map.Entry<Object, Object> entry : dataSources.entrySet()) {
            if (entry.getValue() instanceof HikariDataSource) {
                HikariDataSource ds = (HikariDataSource) entry.getValue();
                HikariPoolMXBean pool = ds.getHikariPoolMXBean();

                Map<String, Object> stats = new HashMap<>();
                stats.put("active", pool.getActiveConnections());
                stats.put("idle", pool.getIdleConnections());
                stats.put("total", pool.getTotalConnections());
                stats.put("waiting", pool.getThreadsAwaitingConnection());
                stats.put("maxPoolSize", ds.getMaximumPoolSize());

                String status = pool.getThreadsAwaitingConnection() > 0 ? "WARNING" : "OK";
                stats.put("status", status);

                poolStats.put(entry.getKey().toString(), stats);
            }
        }

        return ApiResponse.success("Connection pool statistics", poolStats);
    }

}