package com.erp.common.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DatabaseInitializationService {

    private final FlywayMigrationService flywayMigrationService;
    private final DataSource dataSource;

    @Value("${app.database.master-schema:erp_master}")
    private String masterSchemaName;

    @Value("${app.database.tenant-schema-prefix:tenant_}")
    private String tenantSchemaPrefix;

    @Value("${app.database.auto-migrate-on-startup:true}")
    private boolean autoMigrateOnStartup;

    /**
     * This method runs after application is fully started
     * It migrates master schema and all existing tenant schemas
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initializeDatabaseOnStartup() {
        if (!autoMigrateOnStartup) {
            log.info("Auto-migration on startup is disabled");
            return;
        }

        log.info("Starting database initialization on application startup...");

        try {
            // IMPORTANT: Clear tenant context to ensure master schema operations
            // TenantContext.clear();

            // 1. Initialize master schema first
            initializeMasterSchema();

            // 2. Find and migrate all existing tenant schemas
            migrateAllExistingTenantSchemas();

            log.info("Database initialization completed successfully");

        } catch (Exception e) {
            log.error("Database initialization failed", e);
            // You might want to decide whether to fail application startup or continue
            // throw new RuntimeException("Database initialization failed", e);
        }
    }

    /**
     * Initialize master database schema
     */
    public void initializeMasterSchema() {
        try {
            log.info("Initializing master schema: {}", masterSchemaName);

            // Create master schema if it doesn't exist
            createSchemaIfNotExists(masterSchemaName);

            // Run master migrations
            flywayMigrationService.runMasterMigrations(dataSource);

            log.info("Master schema initialization completed");

        } catch (Exception e) {
            log.error("Failed to initialize master schema", e);
            throw new RuntimeException("Master schema initialization failed", e);
        }
    }

    /**
     * Find all existing tenant schemas and migrate them
     */
    public void migrateAllExistingTenantSchemas() {
        try {
            List<String> tenantSchemas = findAllTenantSchemas();
            log.info("Found {} existing tenant schemas", tenantSchemas.size());

            for (String schemaName : tenantSchemas) {
                try {
                    log.info("Migrating tenant schema: {}", schemaName);
                    flywayMigrationService.runTenantMigrations(dataSource, schemaName);
                    log.info("Successfully migrated tenant schema: {}", schemaName);

                } catch (Exception e) {
                    log.error("Failed to migrate tenant schema: {}", schemaName, e);
                    // Continue with other schemas even if one fails
                }
            }

        } catch (Exception e) {
            log.error("Error during tenant schemas migration", e);
        }
    }

    /**
     * Initialize a new tenant schema (called during tenant creation)
     * This matches your current tenant creation logic
     */
    public void initializeNewTenantSchema(String tenantId) {
        try {
            String schemaName = tenantSchemaPrefix + tenantId;
            log.info("Initializing new tenant schema: {} for tenant: {}", schemaName, tenantId);

            // 1. Create schema using raw SQL (matching your approach)
            createSchemaIfNotExists(schemaName);

            // 2. Run migrations using your existing method signature
            flywayMigrationService.runTenantMigrations(dataSource, schemaName);

            log.info("New tenant schema initialized successfully: {}", schemaName);

        } catch (Exception e) {
            log.error("Failed to initialize tenant schema for tenant: {}", tenantId, e);
            throw new RuntimeException("Tenant schema initialization failed for: " + tenantId, e);
        }
    }

    /**
     * Update an existing tenant schema (useful for manual updates)
     * This matches your current migration logic
     */
    public void updateTenantSchema(String tenantId) {
        try {
            String schemaName = tenantSchemaPrefix + tenantId;
            log.info("Updating tenant schema: {} for tenant: {}", schemaName, tenantId);

            if (!schemaExists(schemaName)) {
                throw new RuntimeException("Tenant schema does not exist: " + schemaName);
            }

            // Run migrations using your existing method
            flywayMigrationService.runTenantMigrations(dataSource, schemaName);

            log.info("Tenant schema updated successfully: {}", schemaName);

        } catch (Exception e) {
            log.error("Failed to update tenant schema for tenant: {}", tenantId, e);
            throw new RuntimeException("Tenant schema update failed for: " + tenantId, e);
        }
    }

    /**
     * Get database initialization status
     */
    public DatabaseInitializationStatus getInitializationStatus() {
        DatabaseInitializationStatus status = new DatabaseInitializationStatus();

        try {
            // Check master schema
            status.setMasterSchemaExists(schemaExists(masterSchemaName));

            // Find tenant schemas
            List<String> tenantSchemas = findAllTenantSchemas();
            status.setTenantSchemas(tenantSchemas);
            status.setTenantSchemaCount(tenantSchemas.size());

            status.setInitialized(status.isMasterSchemaExists() && status.getTenantSchemaCount() >= 0);

        } catch (Exception e) {
            log.error("Error getting initialization status", e);
            status.setError(e.getMessage());
        }

        return status;
    }

    private void createSchemaIfNotExists(String schemaName) {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            String createSchemaSQL = "CREATE SCHEMA IF NOT EXISTS `" + schemaName +
                    "` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci";

            statement.executeUpdate(createSchemaSQL);
            log.debug("Schema {} created/verified successfully", schemaName);

        } catch (Exception e) {
            log.error("Error creating schema: {}", schemaName, e);
            throw new RuntimeException("Failed to create schema: " + schemaName, e);
        }
    }

    private List<String> findAllTenantSchemas() {
        List<String> tenantSchemas = new ArrayList<>();

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            ResultSet rs = statement.executeQuery(
                    "SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA " +
                            "WHERE SCHEMA_NAME LIKE '" + tenantSchemaPrefix + "%'"
            );

            while (rs.next()) {
                tenantSchemas.add(rs.getString("SCHEMA_NAME"));
            }

        } catch (Exception e) {
            log.error("Error finding tenant schemas", e);
            throw new RuntimeException("Failed to find tenant schemas", e);
        }

        return tenantSchemas;
    }

    /**
     * Check if schema exists - matches your database check pattern
     */
    private boolean schemaExists(String schemaName) {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            ResultSet rs = statement.executeQuery(
                    "SELECT COUNT(*) FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = '" + schemaName + "'"
            );

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            return false;

        } catch (Exception e) {
            log.error("Error checking if schema exists: {}", schemaName, e);
            return false;
        }
    }

    // Inner class for status reporting
    public static class DatabaseInitializationStatus {
        private boolean initialized;
        private boolean masterSchemaExists;
        private List<String> tenantSchemas = new ArrayList<>();
        private int tenantSchemaCount;
        private String error;

        // Getters and setters
        public boolean isInitialized() {
            return initialized;
        }

        public void setInitialized(boolean initialized) {
            this.initialized = initialized;
        }

        public boolean isMasterSchemaExists() {
            return masterSchemaExists;
        }

        public void setMasterSchemaExists(boolean masterSchemaExists) {
            this.masterSchemaExists = masterSchemaExists;
        }

        public List<String> getTenantSchemas() {
            return tenantSchemas;
        }

        public void setTenantSchemas(List<String> tenantSchemas) {
            this.tenantSchemas = tenantSchemas;
        }

        public int getTenantSchemaCount() {
            return tenantSchemaCount;
        }

        public void setTenantSchemaCount(int tenantSchemaCount) {
            this.tenantSchemaCount = tenantSchemaCount;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }
    }

}
