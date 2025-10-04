package com.erp.admin.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

@Service
@RequiredArgsConstructor
@Slf4j
public class SchemaManagementService {

    private final DataSource dataSource;

    @Value("${spring.datasource.username}")
    private String dbUsername;

    public void createTenantSchema(String schemaName) {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            // Create schema/database
            String createSchemaSQL = "CREATE DATABASE IF NOT EXISTS `" + schemaName + "` " +
                    "CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci";
            statement.executeUpdate(createSchemaSQL);

            // REMOVED GRANT STATEMENT - Not needed if using same user for all schemas
            // The application will use the same database user for all tenant schemas

            log.info("Created schema: {}", schemaName);

        } catch (Exception e) {
            log.error("Failed to create schema: {}", schemaName, e);
            throw new RuntimeException("Failed to create tenant schema: " + schemaName, e);
        }
    }

    public void dropTenantSchema(String schemaName) {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            String dropSchemaSQL = "DROP DATABASE IF EXISTS `" + schemaName + "`";
            statement.executeUpdate(dropSchemaSQL);

            log.info("Dropped schema: {}", schemaName);

        } catch (Exception e) {
            log.error("Failed to drop schema: {}", schemaName, e);
            throw new RuntimeException("Failed to drop tenant schema: " + schemaName, e);
        }
    }

    public boolean schemaExists(String schemaName) {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            String checkSQL = "SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA " +
                    "WHERE SCHEMA_NAME = '" + schemaName + "'";
            ResultSet resultSet = statement.executeQuery(checkSQL);
            return resultSet.next();

        } catch (Exception e) {
            log.error("Failed to check schema existence: {}", schemaName, e);
            return false;
        }
    }

}
