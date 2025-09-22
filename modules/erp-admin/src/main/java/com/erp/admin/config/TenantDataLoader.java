package com.erp.admin.config;

import com.erp.common.config.MultiTenantDataSourceConfig;
import com.erp.common.context.SchemaContext;
import com.erp.common.context.TenantContext;
import com.erp.common.entity.Tenant;
import com.erp.common.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class TenantDataLoader {

    private final TenantRepository tenantRepository;
    private final MultiTenantDataSourceConfig dataSourceConfig;

    /*@EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        try {
            // Clear any existing context before loading tenants
            TenantContext.clear();
            SchemaContext.clear();

            // Force master schema for loading tenants
            SchemaContext.useMasterSchema();

            log.info("Loading existing tenant data sources...");
            List<Tenant> tenants = tenantRepository.findByIsActiveTrueOrderByCreatedAtDesc();

            if (!tenants.isEmpty()) {
                dataSourceConfig.loadExistingTenants(tenants);
                log.info("Loaded {} active tenant data sources", tenants.size());
            } else {
                log.info("No active tenants found");
            }
        } catch (Exception e) {
            log.error("Error during tenant data loading", e);
        } finally {
            // Clear context after loading
            SchemaContext.clear();
            TenantContext.clear();
        }
    }*/

}
