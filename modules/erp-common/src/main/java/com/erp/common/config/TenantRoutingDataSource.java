package com.erp.common.config;

import com.erp.common.annotation.ForceMasterSchema;
import com.erp.common.annotation.ForceTenantSchema;
import com.erp.common.context.SchemaContext;
import com.erp.common.context.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

@Slf4j
public class TenantRoutingDataSource extends AbstractRoutingDataSource {

    @Override
    protected Object determineCurrentLookupKey() {
        try {
            // 1. First check if SchemaContext has forced schema (from aspects)
            if (SchemaContext.hasForcedSchema()) {
                String forcedSchema = SchemaContext.getCurrentSchema();
                log.debug("SchemaContext has forced schema: {}", forcedSchema);

                if ("master".equals(forcedSchema)) {
                    log.debug("Using MASTER schema (forced by context)");
                    return "master";
                }

                if ("tenant".equals(forcedSchema)) {
                    String tenantCode = TenantContext.getCurrentTenant();
                    if (tenantCode != null) {
                        String key = "tenant_" + tenantCode.toLowerCase();
                        log.debug("Using TENANT schema (forced): {}", key);
                        return key;
                    }
                    log.warn("Forced to tenant but no tenant context, using master");
                    return "master";
                }

                return forcedSchema;
            }

            // 2. Check if we're being called from a class/method with @ForceMasterSchema
            if (isCalledFromMasterSchemaContext()) {
                log.debug("Detected @ForceMasterSchema in call stack, using MASTER");
                return "master";
            }

            // 3. Check if we're being called from a class/method with @ForceTenantSchema
            if (isCalledFromTenantSchemaContext()) {
                String tenantCode = TenantContext.getCurrentTenant();
                if (tenantCode != null) {
                    String key = "tenant_" + tenantCode.toLowerCase();
                    log.debug("Detected @ForceTenantSchema in call stack, using: {}", key);
                    return key;
                }
            }

            // 4. Check tenant context from JWT
            String tenantCode = TenantContext.getCurrentTenant();
            if (tenantCode != null) {
                String key = "tenant_" + tenantCode.toLowerCase();
                log.debug("Using tenant schema from context: {}", key);
                return key;
            }

            // 5. Default to master
            log.debug("No context found, defaulting to master");
            return "master";

        } catch (Exception e) {
            log.error("Error determining datasource key, defaulting to master", e);
            return "master";
        }
    }

    private boolean isCalledFromMasterSchemaContext() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

        for (StackTraceElement element : stackTrace) {
            try {
                String className = element.getClassName();

                // Skip framework classes
                if (className.startsWith("java.") ||
                        className.startsWith("org.springframework.") ||
                        className.startsWith("com.sun.") ||
                        className.startsWith("org.hibernate.") ||
                        className.startsWith("org.aspectj.")) {
                    continue;
                }

                // Check known master schema classes
                if (className.contains("UserRepository") ||
                        className.contains("UserSettingsService") ||
                        className.contains("TenantRepository") ||
                        className.contains("CustomUserDetailsService") ||
                        className.contains("AuthenticationService") ||
                        className.contains("TenantManagementService")) {
                    log.trace("Found master schema class in stack: {}", className);
                    return true;
                }

                // Try to check for annotation (this might not always work due to proxies)
                try {
                    Class<?> clazz = Class.forName(className);
                    if (clazz.isAnnotationPresent(ForceMasterSchema.class)) {
                        log.trace("Found @ForceMasterSchema on class: {}", className);
                        return true;
                    }
                } catch (Exception e) {
                    // Class not found or not accessible, continue
                }

            } catch (Exception e) {
                // Continue checking other stack elements
            }
        }

        return false;
    }

    private boolean isCalledFromTenantSchemaContext() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

        for (StackTraceElement element : stackTrace) {
            try {
                String className = element.getClassName();

                // Skip framework classes
                if (className.startsWith("java.") ||
                        className.startsWith("org.springframework.") ||
                        className.startsWith("com.sun.") ||
                        className.startsWith("org.hibernate.") ||
                        className.startsWith("org.aspectj.")) {
                    continue;
                }

                // Check known tenant schema classes
                if (className.contains("SectionService") ||
                        className.contains("AcademicYearService") ||
                        className.contains("SectionController") ||
                        className.contains("StudentService") ||
                        className.contains("ClassService")) {
                    log.trace("Found tenant schema class in stack: {}", className);
                    return true;
                }

                // Try to check for annotation
                try {
                    Class<?> clazz = Class.forName(className);
                    if (clazz.isAnnotationPresent(ForceTenantSchema.class)) {
                        log.trace("Found @ForceTenantSchema on class: {}", className);
                        return true;
                    }
                } catch (Exception e) {
                    // Class not found or not accessible, continue
                }

            } catch (Exception e) {
                // Continue checking other stack elements
            }
        }

        return false;
    }
}
