package com.erp.common.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.output.MigrateResult;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

@Service
@RequiredArgsConstructor
@Slf4j
public class FlywayMigrationService {

    /**
     * Run tenant migrations - enhanced with better error handling
     */
    public void runTenantMigrations(DataSource tenantDataSource, String schemaName) {
        try {
            log.info("Starting Flyway migrations for schema: {}", schemaName);

            // Validate schema exists
            if (!schemaExists(tenantDataSource, schemaName)) {
                createSchema(tenantDataSource, schemaName);
            }

            // Switch to correct schema
            try (Connection connection = tenantDataSource.getConnection();
                 Statement statement = connection.createStatement()) {
                statement.execute("USE `" + schemaName + "`");
                log.info("Switched to schema: {}", schemaName);
            }

            // Configure Flyway
            Flyway flyway = Flyway.configure()
                    .dataSource(tenantDataSource)
                    .locations("classpath:db/migration/tenant")
                    .schemas(schemaName)
                    .baselineOnMigrate(true)
                    .baselineVersion("0")
                    .table("flyway_schema_history")
                    .validateOnMigrate(false)
                    .cleanDisabled(false)
                    .outOfOrder(true)
                    .load();

            // Log pending migrations
            MigrationInfo[] pending = flyway.info().pending();
            log.info("Pending migrations for schema {}: {}", schemaName, pending.length);
            for (MigrationInfo info : pending) {
                log.info("  - {} : {}", info.getVersion(), info.getDescription());
            }

            // Check for failed migrations manually
            checkAndRepairFailedMigrations(tenantDataSource, schemaName);

            // Run migrations
            MigrateResult result = flyway.migrate();
            log.info("Flyway migration completed for schema {}. Success: {}, Migrations executed: {}",
                    schemaName, result.success, result.migrationsExecuted);

            // Verify tables were created
            verifyTablesCreated(tenantDataSource, schemaName);

        } catch (Exception e) {
            log.error("Failed to run Flyway migrations for schema: {}", schemaName, e);
            throw new RuntimeException("Failed to run database migrations for schema: " + schemaName, e);
        }
    }

    /**
     * Run master migrations - enhanced with better error handling
     */
    public void runMasterMigrations(DataSource masterDataSource) {
        try {
            log.info("Starting Flyway migrations for master database");

            // Configure Flyway for master
            Flyway flyway = Flyway.configure()
                    .dataSource(masterDataSource)
                    .locations("classpath:db/migration/master")
                    .schemas("erp_master")  // Explicitly set master schema
                    .baselineOnMigrate(true)
                    .baselineVersion("0")
                    .table("flyway_schema_history_master")
                    .validateOnMigrate(false)
                    .load();

            // Check for failed migrations in master
            checkAndRepairFailedMigrations(masterDataSource, "erp_master", "flyway_schema_history_master");

            // Run migrations
            MigrateResult result = flyway.migrate();
            log.info("Master Flyway migration completed. Success: {}, Migrations executed: {}",
                    result.success, result.migrationsExecuted);

        } catch (Exception e) {
            log.error("Failed to run Flyway migrations for master database", e);
            throw new RuntimeException("Failed to run master database migrations", e);
        }
    }

    /**
     * Check and repair failed migrations manually
     */
    private void checkAndRepairFailedMigrations(DataSource dataSource, String schemaName) {
        checkAndRepairFailedMigrations(dataSource, schemaName, "flyway_schema_history");
    }

    /**
     * Check and repair failed migrations manually with custom table name
     */
    private void checkAndRepairFailedMigrations(DataSource dataSource, String schemaName, String tableName) {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            // Check if flyway history table exists
            ResultSet tableExists = statement.executeQuery(
                    "SELECT COUNT(*) FROM information_schema.tables " +
                            "WHERE table_schema = '" + schemaName + "' AND table_name = '" + tableName + "'"
            );

            if (tableExists.next() && tableExists.getInt(1) > 0) {
                // Check for failed migrations
                ResultSet failedMigrations = statement.executeQuery(
                        "SELECT version, description FROM `" + schemaName + "`." + tableName + " WHERE success = 0"
                );

                boolean hasFailedMigrations = false;
                while (failedMigrations.next()) {
                    hasFailedMigrations = true;
                    String version = failedMigrations.getString("version");
                    String description = failedMigrations.getString("description");
                    log.warn("Found failed migration in {}: {} - {}", schemaName, version, description);
                }

                if (hasFailedMigrations) {
                    log.info("Removing failed migration entries from schema: {}", schemaName);
                    int deletedRows = statement.executeUpdate(
                            "DELETE FROM `" + schemaName + "`." + tableName + " WHERE success = 0"
                    );
                    log.info("Removed {} failed migration entries from schema: {}", deletedRows, schemaName);
                }
            }

        } catch (Exception e) {
            log.warn("Could not check/repair failed migrations for schema: {}", schemaName, e);
        }
    }

    /**
     * Check if schema exists
     */
    private boolean schemaExists(DataSource dataSource, String schemaName) {
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

    /**
     * Create schema if it doesn't exist
     */
    private void createSchema(DataSource dataSource, String schemaName) {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            String createSchemaSQL = "CREATE SCHEMA IF NOT EXISTS `" + schemaName +
                    "` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci";

            statement.executeUpdate(createSchemaSQL);
            log.info("Schema {} created successfully", schemaName);

        } catch (Exception e) {
            log.error("Error creating schema: {}", schemaName, e);
            throw new RuntimeException("Failed to create schema: " + schemaName, e);
        }
    }

    /**
     * Table verification logic
     */
    private void verifyTablesCreated(DataSource dataSource, String schemaName) {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            // Check table count
            ResultSet rs = statement.executeQuery(
                    "SELECT COUNT(*) as table_count FROM information_schema.tables " +
                            "WHERE table_schema = '" + schemaName + "'"
            );

            if (rs.next()) {
                int tableCount = rs.getInt("table_count");
                log.info("Total tables in schema {}: {}", schemaName, tableCount);

                // List all tables
                ResultSet tableList = statement.executeQuery(
                        "SELECT table_name FROM information_schema.tables " +
                                "WHERE table_schema = '" + schemaName + "' " +
                                "ORDER BY table_name"
                );

                log.info("Tables created in {}:", schemaName);
                while (tableList.next()) {
                    log.info("  - {}", tableList.getString("table_name"));
                }

                // Verify critical tables exist
                verifyCriticalTables(statement, schemaName);
            }

        } catch (Exception e) {
            log.error("Error verifying tables in schema: {}", schemaName, e);
        }
    }

    /**
     * Verify that critical tables exist
     */
    private void verifyCriticalTables(Statement statement, String schemaName) {
        String[] criticalTables = {
                "academic_years", "classes", "sections", "subjects", "students"
        };

        for (String tableName : criticalTables) {
            try {
                ResultSet rs = statement.executeQuery(
                        "SELECT COUNT(*) FROM information_schema.tables " +
                                "WHERE table_schema = '" + schemaName + "' AND table_name = '" + tableName + "'"
                );

                if (rs.next() && rs.getInt(1) == 0) {
                    log.warn("Critical table {} is missing in schema {}", tableName, schemaName);
                } else {
                    log.debug("Critical table {} exists in schema {}", tableName, schemaName);
                }
            } catch (Exception e) {
                log.warn("Error checking critical table {}: {}", tableName, e.getMessage());
            }
        }
    }

}
