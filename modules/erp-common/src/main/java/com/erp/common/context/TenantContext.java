package com.erp.common.context;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TenantContext {

    private static final ThreadLocal<String> currentTenant = new ThreadLocal<>();
    private static final ThreadLocal<Long> currentTenantId = new ThreadLocal<>();
    private static final ThreadLocal<String> currentSchema = new ThreadLocal<>();

    /**
     * Set complete tenant context (used when JWT has all tenant info)
     */
    public static void setCurrentTenant(Long tenantId, String tenantCode, String schemaName) {
        // Clean up tenant code if it has "tenant_" prefix
        if (tenantCode != null && tenantCode.startsWith("tenant_")) {
            tenantCode = tenantCode.substring(7).toUpperCase(); // Remove "tenant_" and uppercase
        }

        currentTenantId.set(tenantId);
        currentTenant.set(tenantCode);

        // Derive schema from tenant code if not provided
        if (schemaName != null) {
            currentSchema.set(schemaName);
        } else if (tenantCode != null) {
            currentSchema.set("tenant_" + tenantCode.toLowerCase());
        }

        log.debug("Set tenant context - ID: {}, Code: {}, Schema: {}",
                tenantId, tenantCode, currentSchema.get());
    }

    /**
     * Set schema directly (used by routing)
     */
    public static void setCurrentSchema(String schema) {
        currentSchema.set(schema);
        log.debug("Set schema: {}", schema);
    }

    /**
     * Get current tenant code
     */
    public static String getCurrentTenant() {
        return currentTenant.get();
    }

    /**
     * Get current tenant ID
     */
    public static Long getCurrentTenantId() {
        return currentTenantId.get();
    }

    /**
     * Get current schema for datasource routing
     */
    public static String getCurrentSchema() {
        return currentSchema.get();
    }

    /**
     * Clear all context
     */
    public static void clear() {
        currentTenantId.remove();
        currentTenant.remove();
        currentSchema.remove();
        log.debug("Cleared tenant context");
    }

    /**
     * Check if running in master context
     */
    public static boolean isMasterContext() {
        return "master".equals(currentSchema.get());
    }

    /**
     * Check if running in tenant context
     */
    public static boolean isTenantContext() {
        String schema = currentSchema.get();
        return schema != null && schema.startsWith("tenant_");
    }

    /**
     * Check if we have a tenant context
     */
    public static boolean hasTenantContext() {
        return currentTenant.get() != null;
    }

    /**
     * Get tenant key for datasource lookup
     */
    public static String getCurrentTenantKey() {
        String tenantCode = getCurrentTenant();
        if (tenantCode != null) {
            return "tenant_" + tenantCode.toLowerCase();
        }

        String schema = getCurrentSchema();
        if (schema != null) {
            return schema;
        }

        return "master";
    }

}
