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

        // Clear contexts at the start of each request
        clearAllContexts();

        String requestURI = request.getRequestURI();
        log.debug("=== Processing request: {} {} ===", request.getMethod(), requestURI);

        try {
            // Extract and validate JWT
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                // Extract user information from token
                String username = tokenProvider.getUsernameFromToken(jwt);
                Long tenantId = tokenProvider.getTenantIdFromToken(jwt);
                String tenantCode = tokenProvider.getTenantCodeFromToken(jwt);

                log.debug("JWT validated - User: {}, TenantID: {}, TenantCode: {}",
                        username, tenantId, tenantCode);

                // Set tenant context if user belongs to a tenant
                if (tenantCode != null && tenantId != null) {
                    TenantContext.setCurrentTenant(tenantId, tenantCode, null);
                    log.debug("Set TenantContext - ID: {}, Code: {}", tenantId, tenantCode);
                } else {
                    log.debug("Super admin user (no tenant context)");
                }

                // DO NOT set SchemaContext here - let interceptor handle it based on endpoint

                // Load user details and set security context
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (userDetails != null) {
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("Authentication set for user: {}", username);
                }
            }
        } catch (Exception ex) {
            log.error("Error during JWT authentication", ex);
            // Don't block the request - let it continue without authentication
        }

        try {
            // Continue with the request
            filterChain.doFilter(request, response);
        } finally {
            // Clear contexts after request completes
            clearAllContexts();
            log.debug("=== Request completed, contexts cleared ===");
        }
    }

    private void clearAllContexts() {
        TenantContext.clear();
        SchemaContext.clear();
        SecurityContextHolder.clearContext();
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

}