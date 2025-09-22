package com.erp.common.aspect;

import com.erp.common.context.SchemaContext;
import com.erp.common.context.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE) // Ensure this runs FIRST before any other aspect
public class SchemaRoutingAspect {

    @Around("@annotation(com.erp.common.annotation.ForceMasterSchema) || @within(com.erp.common.annotation.ForceMasterSchema)")
    public Object forceMasterSchema(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();
        String originalSchema = SchemaContext.getCurrentSchema();

        log.info("=== FORCING MASTER SCHEMA for: {} ===", methodName);
        log.debug("Original schema before forcing: {}", originalSchema);

        try {
            // Force master schema
            SchemaContext.useMasterSchema();
            log.info("Schema set to MASTER for method: {}", methodName);
            Object result = joinPoint.proceed();

            log.info("Successfully executed {} with MASTER schema", methodName);
            return result;

        } catch (Exception e) {
            log.error("Error in method {} with master schema: {}", methodName, e.getMessage());
            throw e;

        } finally {
            // Restore original schema
            if (originalSchema != null) {
                SchemaContext.setSchema(originalSchema);
                log.debug("Restored schema to: {}", originalSchema);
            } else {
                SchemaContext.clear();
                log.debug("Cleared schema context (was null before)");
            }
        }
    }

    @Around("@annotation(com.erp.common.annotation.ForceTenantSchema) || @within(com.erp.common.annotation.ForceTenantSchema)")
    public Object forceTenantSchema(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();
        String originalSchema = SchemaContext.getCurrentSchema();

        log.info("=== FORCING TENANT SCHEMA for: {} ===", methodName);
        log.debug("Original schema before forcing: {}", originalSchema);

        try {
            // Force tenant schema
            SchemaContext.useTenantSchema();

            String tenantCode = TenantContext.getCurrentTenant();
            log.info("Schema set to TENANT for method: {} (tenant: {})", methodName, tenantCode);

            Object result = joinPoint.proceed();

            log.info("Successfully executed {} with TENANT schema", methodName);
            return result;

        } catch (Exception e) {
            log.error("Error in method {} with tenant schema: {}", methodName, e.getMessage());
            throw e;

        } finally {
            // Restore original schema
            if (originalSchema != null) {
                SchemaContext.setSchema(originalSchema);
                log.debug("Restored schema to: {}", originalSchema);
            } else {
                SchemaContext.clear();
                log.debug("Cleared schema context (was null before)");
            }
        }
    }

}
