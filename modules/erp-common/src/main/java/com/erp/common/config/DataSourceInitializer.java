package com.erp.common.config;

import com.erp.common.context.SchemaContext;
import com.erp.common.context.TenantContext;
import com.erp.common.entity.Tenant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Component
@Order(1) // Run first
@RequiredArgsConstructor
@Slf4j
public class DataSourceInitializer implements CommandLineRunner {

    private final MultiTenantDataSourceConfig dataSourceConfig;

    @Value("${spring.datasource.url}")
    private String masterDbUrl;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Override
    public void run(String... args) throws Exception {
        // Ensure clean context at startup
        TenantContext.clear();
        SchemaContext.clear();

        log.info("Initializing tenant data sources directly from database...");

        List<Tenant> tenants = loadTenantsDirectly();

        if (!tenants.isEmpty()) {
            dataSourceConfig.loadExistingTenants(tenants);
            log.info("Loaded {} active tenant data sources", tenants.size());
        } else {
            log.info("No active tenants found");
        }

        // Clear context again after initialization
        TenantContext.clear();
        SchemaContext.clear();
    }

    private List<Tenant> loadTenantsDirectly() {
        List<Tenant> tenants = new ArrayList<>();

        try {
            // Create a direct connection to master database, bypassing all routing
            DataSource masterDs = dataSourceConfig.getMasterDataSource();
            if (masterDs == null) {
                log.warn("Master datasource not available yet");
                return tenants;
            }

            try (Connection conn = masterDs.getConnection();
                 Statement stmt = conn.createStatement()) {

                // Explicitly use master database
                stmt.execute("USE erp_master");

                String query = "SELECT id, tenant_code, tenant_name, schema_name " +
                        "FROM tenants WHERE is_active = true";

                ResultSet rs = stmt.executeQuery(query);

                while (rs.next()) {
                    Tenant tenant = new Tenant();
                    tenant.setId(rs.getLong("id"));
                    tenant.setTenantCode(rs.getString("tenant_code"));
                    tenant.setTenantName(rs.getString("tenant_name"));
                    tenant.setSchemaName(rs.getString("schema_name"));
                    tenants.add(tenant);

                    log.debug("Found tenant: {} with schema: {}",
                            tenant.getTenantCode(), tenant.getSchemaName());
                }
            }
        } catch (Exception e) {
            log.error("Error loading tenants directly from database", e);
        }

        return tenants;
    }

}
