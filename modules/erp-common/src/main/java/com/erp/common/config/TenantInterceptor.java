package com.erp.common.config;

import com.erp.common.context.SchemaContext;
import com.erp.common.context.TenantContext;
import com.erp.common.entity.Tenant;
import com.erp.common.jwt.JwtTokenProvider;
import com.erp.common.repository.TenantRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
@Slf4j
public class TenantInterceptor implements HandlerInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final TenantRepository tenantRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        String requestURI = request.getRequestURI();
        log.debug("Processing request: {} {}", request.getMethod(), requestURI);

        // ============================================
        // MASTER SCHEMA ENDPOINTS (User & Auth data)
        // ============================================

        // Authentication endpoints - ALWAYS use master
        if (isAuthEndpoint(requestURI)) {
            log.debug("AUTH endpoint → forcing MASTER schema");
            SchemaContext.useMasterSchema();
            TenantContext.clear();
            return true;
        }

        // User profile/settings endpoints - use master (users table is in master)
        if (isUserEndpoint(requestURI)) {
            log.debug("USER endpoint → forcing MASTER schema");
            SchemaContext.useMasterSchema();
            // Still extract tenant context for reference, but force master schema
            extractAndSetTenantContext(request);
            return true;
        }

        // Admin endpoints - use master
        if (isSuperAdminEndpoint(requestURI)) {
            log.debug("ADMIN endpoint → forcing MASTER schema");
            SchemaContext.useMasterSchema();
            TenantContext.clear();
            return true;
        }

        // Tenant admin endpoints (user management) - use master
        if (isTenantAdminEndpoint(requestURI)) {
            log.debug("TENANT-ADMIN endpoint → forcing MASTER schema");
            SchemaContext.useMasterSchema();
            extractAndSetTenantContext(request);
            return true;
        }

        // Public endpoints - use master
        if (isPublicEndpoint(requestURI)) {
            log.debug("PUBLIC endpoint → forcing MASTER schema");
            SchemaContext.useMasterSchema();
            TenantContext.clear();
            return true;
        }

        // Test endpoints - determine by path
        if (isTestEndpoint(requestURI)) {
            log.debug("TEST endpoint → forcing MASTER schema");
            SchemaContext.useMasterSchema();
            extractAndSetTenantContext(request);
            return true;
        }

        // ============================================
        // TENANT SCHEMA ENDPOINTS (Business data)
        // ============================================

        // For tenant business endpoints, use tenant schema
        if (isTenantBusinessEndpoint(requestURI)) {
            log.debug("TENANT BUSINESS endpoint → using TENANT schema");
            // Don't force schema - let it use tenant context
            SchemaContext.clear();
            extractAndSetTenantContext(request);

            // Validate tenant context was set
            if (TenantContext.getCurrentTenant() == null) {
                log.error("No tenant context for business endpoint: {}", requestURI);
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Tenant context required");
                return false;
            }
            return true;
        }

        // ============================================
        // DEFAULT BEHAVIOR
        // ============================================

        // For any other endpoint, try to extract tenant but default to master
        log.debug("DEFAULT handling for: {}", requestURI);
        extractAndSetTenantContext(request);

        // If no specific routing, default to master for safety
        if (!SchemaContext.hasForcedSchema()) {
            SchemaContext.useMasterSchema();
        }

        return true;
    }

    private void extractAndSetTenantContext(HttpServletRequest request) {
        try {
            String token = extractTokenFromRequest(request);
            if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {

                Long tenantId = jwtTokenProvider.getTenantIdFromToken(token);
                String tenantCode = jwtTokenProvider.getTenantCodeFromToken(token);

                if (tenantId != null && tenantCode != null) {
                    // Temporarily use master to query tenant info
                    String currentSchema = SchemaContext.getCurrentSchema();
                    SchemaContext.useMasterSchema();

                    try {
                        Tenant tenant = tenantRepository.findByTenantCodeAndIsActiveTrue(tenantCode)
                                .orElseThrow(() -> new RuntimeException("Tenant not found: " + tenantCode));

                        // Set tenant context with proper key format
                        String tenantKey = "tenant_" + tenantCode.toLowerCase();
                        TenantContext.setCurrentTenant(tenantId, tenantKey, tenant.getSchemaName());

                        log.debug("Set tenant context - ID: {}, Key: {}, Schema: {}",
                                tenantId, tenantKey, tenant.getSchemaName());
                    } finally {
                        // Restore original schema context
                        if (currentSchema != null) {
                            SchemaContext.setSchema(currentSchema);
                        } else {
                            SchemaContext.clear();
                        }
                    }
                } else {
                    // User without tenant (super admin)
                    log.debug("No tenant in token (super admin user)");
                    TenantContext.clear();
                }
            }
        } catch (Exception e) {
            log.error("Error extracting tenant context: {}", e.getMessage());
            TenantContext.clear();
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // Clear all contexts after request completion
        TenantContext.clear();
        SchemaContext.clear();
        log.debug("Cleared all contexts after request completion");
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    // ============================================
    // ENDPOINT CLASSIFICATION METHODS
    // ============================================

    private boolean isAuthEndpoint(String uri) {
        return uri.startsWith("/api/auth/");
    }

    private boolean isUserEndpoint(String uri) {
        // User profile, settings, preferences - all in master DB
        return uri.startsWith("/api/user/");
    }

    private boolean isSuperAdminEndpoint(String uri) {
        // Super admin operations
        return uri.startsWith("/api/admin/");
    }

    private boolean isTenantAdminEndpoint(String uri) {
        // Tenant admin operations (user management)
        return uri.startsWith("/api/tenant-admin/");
    }

    private boolean isPublicEndpoint(String uri) {
        return uri.startsWith("/api/public/") ||
                uri.startsWith("/actuator/");
    }

    private boolean isTestEndpoint(String uri) {
        return uri.startsWith("/api/test/");
    }

    private boolean isTenantBusinessEndpoint(String uri) {
        // All business operations that work with tenant-specific data
        return uri.startsWith("/api/tenant/") ||      // Generic tenant operations
                uri.startsWith("/api/school/") ||      // School management
                uri.startsWith("/api/student/") ||     // Student management
                uri.startsWith("/api/staff/") ||       // Staff management
                uri.startsWith("/api/academic/") ||    // Academic operations
                uri.startsWith("/api/attendance/") ||  // Attendance
                uri.startsWith("/api/exam/") ||        // Examinations
                uri.startsWith("/api/fee/") ||         // Fee management
                uri.startsWith("/api/library/") ||     // Library
                uri.startsWith("/api/transport/") ||   // Transport
                uri.startsWith("/api/reports/");       // Reports
    }

}
