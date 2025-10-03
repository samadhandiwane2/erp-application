package com.erp.common.config;

import com.erp.common.context.SchemaContext;
import com.erp.common.context.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

@Slf4j
public class TenantRoutingDataSource extends AbstractRoutingDataSource {

    @Override
    protected Object determineCurrentLookupKey() {
        try {
            String schemaContext = SchemaContext.getCurrentSchema();
            String tenantCode = TenantContext.getCurrentTenant();

            log.debug("Routing decision - SchemaContext: {}, TenantCode: {}",
                    schemaContext, tenantCode);

            // Priority 1: Explicit schema context (set by interceptor/aspects)
            if (schemaContext != null) {
                if ("master".equals(schemaContext)) {
                    log.debug("â†' Routing to: master (explicit)");
                    return "master";
                }

                if ("tenant".equals(schemaContext)) {
                    if (tenantCode != null) {
                        String key = "tenant_" + tenantCode.toLowerCase();
                        log.debug("â†' Routing to: {} (explicit tenant)", key);
                        return key;
                    } else {
                        log.warn("Tenant schema requested but no tenant context! Falling back to master");
                        return "master";
                    }
                }

                // Direct schema name provided
                log.debug("â†' Routing to: {} (direct schema)", schemaContext);
                return schemaContext;
            }

            // Priority 2: Tenant context (from JWT, but no explicit schema set)
            if (tenantCode != null) {
                String key = "tenant_" + tenantCode.toLowerCase();
                log.debug("â†' Routing to: {} (from tenant context)", key);
                return key;
            }

            // Priority 3: Default to master
            log.debug("â†' Routing to: master (default)");
            return "master";

        } catch (Exception e) {
            log.error("Error determining datasource routing key", e);
            log.warn("â†' Routing to: master (error fallback)");
            return "master";
        }
    }

    @Override
    protected Object resolveSpecifiedLookupKey(Object lookupKey) {
        String key = (String) lookupKey;
        log.trace("Resolving datasource for key: {}", key);
        return super.resolveSpecifiedLookupKey(lookupKey);
    }
}