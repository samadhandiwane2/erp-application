package com.erp.common.config;

import com.erp.common.context.SchemaContext;
import com.erp.common.context.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@Order(1) // Ensure this runs early
@Slf4j
public class TenantInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {

        String requestURI = request.getRequestURI();
        String method = request.getMethod();

        log.debug("Interceptor processing: {} {}", method, requestURI);
        log.debug("Current contexts - Tenant: {}, Schema: {}",
                TenantContext.getCurrentTenant(), SchemaContext.getCurrentSchema());

        // Determine and set schema based on endpoint
        setSchemaForEndpoint(requestURI);

        log.debug("After routing - Tenant: {}, Schema: {}",
                TenantContext.getCurrentTenant(), SchemaContext.getCurrentSchema());

        return true;
    }

    private void setSchemaForEndpoint(String uri) {
        // MASTER SCHEMA endpoints (User authentication and management)
        if (isAuthEndpoint(uri) ||
                isUserManagementEndpoint(uri) ||
                isAdminEndpoint(uri) ||
                isPublicEndpoint(uri)) {

            log.debug("Master schema endpoint: {}", uri);
            SchemaContext.useMasterSchema();
            return;
        }

        // TENANT SCHEMA endpoints (Business operations)
        if (isTenantBusinessEndpoint(uri)) {
            String tenantCode = TenantContext.getCurrentTenant();

            if (tenantCode == null) {
                log.warn("Tenant business endpoint {} accessed without tenant context", uri);
                // Still set to tenant mode - will fail at datasource level if truly needed
                SchemaContext.useTenantSchema();
            } else {
                log.debug("Tenant schema endpoint: {} for tenant: {}", uri, tenantCode);
                SchemaContext.useTenantSchema();
            }
            return;
        }

        // DEFAULT: Use master schema for safety
        log.debug("Default to master schema for: {}", uri);
        SchemaContext.useMasterSchema();
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // Contexts will be cleared by filter's finally block
        log.trace("Interceptor afterCompletion for: {}", request.getRequestURI());
    }

    // Endpoint classification methods
    private boolean isAuthEndpoint(String uri) {
        return uri.startsWith("/api/auth/");
    }

    private boolean isUserManagementEndpoint(String uri) {
        return uri.startsWith("/api/user/") ||
                uri.startsWith("/api/tenant-admin/users") ||
                uri.startsWith("/api/admin/users");
    }

    private boolean isAdminEndpoint(String uri) {
        return uri.startsWith("/api/admin/") ||
                uri.startsWith("/api/tenant-admin/");
    }

    private boolean isPublicEndpoint(String uri) {
        return uri.startsWith("/api/public/") ||
                uri.startsWith("/actuator/") ||
                uri.startsWith("/api/health/") ||
                uri.startsWith("/swagger-ui/") ||
                uri.startsWith("/v3/api-docs/");
    }

    private boolean isTenantBusinessEndpoint(String uri) {
        return uri.startsWith("/api/tenant/") ||
                uri.startsWith("/api/school/") ||
                uri.startsWith("/api/student/") ||
                uri.startsWith("/api/staff/") ||
                uri.startsWith("/api/academic/") ||
                uri.startsWith("/api/attendance/") ||
                uri.startsWith("/api/exam/") ||
                uri.startsWith("/api/fee/") ||
                uri.startsWith("/api/library/") ||
                uri.startsWith("/api/transport/") ||
                uri.startsWith("/api/reports/");
    }
}