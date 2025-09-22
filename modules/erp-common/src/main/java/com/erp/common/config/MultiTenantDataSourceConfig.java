package com.erp.common.config;

import com.erp.common.entity.Tenant;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.List;

@Configuration
@Slf4j
public class MultiTenantDataSourceConfig {

    private final Map<Object, Object> dataSources = new ConcurrentHashMap<>();

    @Value("${spring.datasource.url}")
    private String masterDbUrl;

    @Value("${spring.datasource.username}")
    private String dbUsername;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    private TenantRoutingDataSource routingDataSource;

    @Bean
    @Primary
    public DataSource dataSource() {
        routingDataSource = new TenantRoutingDataSource();

        // Create and set master datasource
        DataSource masterDataSource = createDataSource("erp_master");

        // IMPORTANT: Add master to the map with "master" key
        dataSources.put("master", masterDataSource);

        // Set as default
        routingDataSource.setDefaultTargetDataSource(masterDataSource);
        routingDataSource.setTargetDataSources(dataSources);
        routingDataSource.afterPropertiesSet();

        log.info("Initialized routing datasource with master database");

        return routingDataSource;
    }

    public void loadExistingTenants(List<Tenant> tenants) {
        try {
            for (Tenant tenant : tenants) {
                addTenantDataSource(tenant.getTenantCode(), tenant.getSchemaName());
            }
            log.info("Loaded {} tenant data sources", tenants.size());
        } catch (Exception e) {
            log.error("Error loading existing tenants", e);
        }
    }

    public void addTenantDataSource(String tenantCode, String schemaName) {
        try {
            // Create datasource for tenant schema
            DataSource tenantDataSource = createDataSource(schemaName);

            // Add with tenant_code as key (matching TenantContext format)
            String tenantKey = "tenant_" + tenantCode.toLowerCase();
            dataSources.put(tenantKey, tenantDataSource);

            // Also add with just the tenant code for backward compatibility
            dataSources.put(tenantCode, tenantDataSource);

            // Also add with the schema name directly
            dataSources.put(schemaName, tenantDataSource);

            if (routingDataSource != null) {
                routingDataSource.setTargetDataSources(dataSources);
                routingDataSource.afterPropertiesSet();
            }

            log.info("Added datasource for tenant: {} (keys: {}, {}, {}) with schema: {}",
                    tenantCode, tenantKey, tenantCode, schemaName, schemaName);
        } catch (Exception e) {
            log.error("Failed to add tenant datasource for: {}", tenantCode, e);
            throw new RuntimeException("Failed to add tenant datasource", e);
        }
    }

    public void removeTenantDataSource(String tenantCode) {
        String tenantKey = "tenant_" + tenantCode.toLowerCase();
        String schemaName = "tenant_" + tenantCode.toLowerCase();

        // Close datasource if it's HikariDataSource
        Object dataSource = dataSources.get(tenantKey);
        if (dataSource instanceof HikariDataSource) {
            ((HikariDataSource) dataSource).close();
        }

        // Remove all possible keys
        dataSources.remove(tenantKey);
        dataSources.remove(tenantCode);
        dataSources.remove(schemaName);

        if (routingDataSource != null) {
            routingDataSource.setTargetDataSources(dataSources);
            routingDataSource.afterPropertiesSet();
        }

        log.info("Removed datasource for tenant: {}", tenantCode);
    }

    public DataSource getTenantDataSource(String schemaName) {
        // Try to find existing datasource
        Object dataSource = dataSources.get(schemaName);
        if (dataSource != null) {
            return (DataSource) dataSource;
        }

        // If not found, create new one
        log.info("Creating new datasource for schema: {}", schemaName);
        return createDataSource(schemaName);
    }

    public DataSource getMasterDataSource() {
        return (DataSource) dataSources.get("master");
    }

    private DataSource createDataSource(String schemaName) {
        HikariConfig config = new HikariConfig();

        // Build JDBC URL
        String jdbcUrl;
        if ("erp_master".equals(schemaName)) {
            jdbcUrl = masterDbUrl; // Use the configured master URL
        } else {
            // Extract base URL and append schema name
            String baseUrl = masterDbUrl.substring(0, masterDbUrl.lastIndexOf('/') + 1);
            jdbcUrl = baseUrl + schemaName + "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
        }

        config.setJdbcUrl(jdbcUrl);
        config.setUsername(dbUsername);
        config.setPassword(dbPassword);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");

        // Connection pool settings
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setPoolName("HikariPool-" + schemaName);

        // CRITICAL: Disable autocommit for proper transaction management
        config.setAutoCommit(false);

        // MySQL optimizations
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("useLocalSessionState", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        config.addDataSourceProperty("cacheResultSetMetadata", "true");
        config.addDataSourceProperty("cacheServerConfiguration", "true");
        config.addDataSourceProperty("elideSetAutoCommits", "true");
        config.addDataSourceProperty("maintainTimeStats", "false");

        log.info("Created datasource for schema: {} with URL: {}", schemaName, jdbcUrl);

        return new HikariDataSource(config);
    }

}
