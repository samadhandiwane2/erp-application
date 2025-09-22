package com.erp.security.jwt;

import com.erp.common.jwt.JwtTokenProvider;
import com.erp.security.service.CustomUserDetailsService;
import com.erp.common.context.TenantContext;
import com.erp.common.context.SchemaContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // CRITICAL: Clear ALL contexts at the beginning of EVERY request
        // This prevents context leakage between requests
        TenantContext.clear();
        SchemaContext.clear();
        SecurityContextHolder.clearContext();

        log.debug("=== Starting new request: {} ===", request.getRequestURI());
        log.debug("Cleared all contexts at request start");

        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                // Extract user information from token
                String username = tokenProvider.getUsernameFromToken(jwt);
                Long tenantId = tokenProvider.getTenantIdFromToken(jwt);
                String tenantCode = tokenProvider.getTenantCodeFromToken(jwt);

                log.info("JWT Token Info - User: {}, TenantID: {}, TenantCode: {}",
                        username, tenantId, tenantCode);

                // IMPORTANT: Set tenant context for tenant users
                if (tenantCode != null && tenantId != null) {
                    // This is a tenant user - set ONLY TenantContext
                    // DO NOT set SchemaContext here - let aspects handle it
                    TenantContext.setCurrentTenant(tenantId, tenantCode, null);
                    log.info("Set TenantContext for tenant user - ID: {}, Code: {}",
                            tenantId, tenantCode);

                    // Verify context is set
                    log.info("Verification - TenantContext.getCurrentTenant(): {}",
                            TenantContext.getCurrentTenant());
                } else {
                    // This is a super admin user (no tenant)
                    log.info("Super admin user: {} (no tenant context set)", username);
                    // Don't set any context for super admin
                }

                // DO NOT set SchemaContext here - it should only be set by aspects
                // Verify SchemaContext is clear
                log.info("SchemaContext should be null: {}", SchemaContext.getCurrentSchema());

                // Load user details and set security context
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (userDetails != null) {
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );
                    authentication.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("Set authentication for user: {}", username);
                }
            } else {
                log.debug("No valid JWT token found for request: {}", request.getRequestURI());
            }
        } catch (Exception ex) {
            log.error("Error setting user authentication: ", ex);
            // Don't rethrow - let request continue without authentication
        }

        try {
            // Process the request
            filterChain.doFilter(request, response);
        } finally {
            // CRITICAL: Always clear all contexts after request
            log.debug("=== Ending request: {} ===", request.getRequestURI());
            log.debug("Clearing all contexts after request");

            TenantContext.clear();
            SchemaContext.clear();
            SecurityContextHolder.clearContext();

            log.debug("All contexts cleared successfully");
        }
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            String token = bearerToken.substring(7);
            log.debug("Extracted JWT token from Authorization header");
            return token;
        }
        log.debug("No Bearer token found in Authorization header");
        return null;
    }

}
