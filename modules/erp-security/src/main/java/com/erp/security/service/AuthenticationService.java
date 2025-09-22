package com.erp.security.service;

import com.erp.common.annotation.ForceMasterSchema;
import com.erp.common.context.SchemaContext;
import com.erp.common.context.TenantContext;
import com.erp.common.dto.auth.LoginRequest;
import com.erp.common.dto.auth.LoginResponse;
import com.erp.common.entity.Tenant;
import com.erp.common.entity.User;
import com.erp.security.exception.AuthenticationException;
import com.erp.common.jwt.JwtTokenProvider;
import com.erp.common.jwt.UserPrincipal;
import com.erp.common.repository.TenantRepository;
import com.erp.security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
// Keep @ForceMasterSchema but we'll also handle it manually as backup
@ForceMasterSchema
public class AuthenticationService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService userDetailsService;

    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final int LOCKOUT_DURATION_MINUTES = 30;

    @Transactional
    public LoginResponse authenticate(LoginRequest loginRequest) {
        // CRITICAL: Clear ALL contexts before login to ensure clean state
        TenantContext.clear();
        SchemaContext.clear();
        log.info("=== LOGIN ATTEMPT START ===");
        log.info("Cleared all contexts for login");

        // MANUALLY FORCE MASTER SCHEMA (in case aspect isn't working)
        SchemaContext.useMasterSchema();
        log.info("Manually forced schema to MASTER for login operation");

        try {
            log.info("Attempting login for user: {} with tenant: {}",
                    loginRequest.getUsername(), loginRequest.getTenantCode());
            log.info("Current contexts - TenantContext: {}, SchemaContext: {}",
                    TenantContext.getCurrentTenant(), SchemaContext.getCurrentSchema());

            // Load user from master database
            UserPrincipal userPrincipal = (UserPrincipal) userDetailsService
                    .loadUserByUsernameAndTenant(loginRequest.getUsername(), loginRequest.getTenantCode());

            User user = userRepository.findById(userPrincipal.getId())
                    .orElseThrow(() -> new AuthenticationException("User not found"));

            // Check if account is locked
            if (isAccountLocked(user)) {
                throw new AuthenticationException("Account is temporarily locked due to multiple failed login attempts");
            }

            // Validate password
            if (!passwordEncoder.matches(loginRequest.getPassword(), userPrincipal.getPassword())) {
                handleFailedLogin(user);
                throw new AuthenticationException("Invalid credentials");
            }

            // Reset failed attempts on successful login
            resetFailedLoginAttempts(user);

            // Update last login
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);

            // Generate tokens
            String accessToken = tokenProvider.generateAccessToken(userPrincipal);
            String refreshToken = tokenProvider.generateRefreshToken(userPrincipal);

            log.info("Login successful for user: {} (tenant: {})",
                    userPrincipal.getUsername(), userPrincipal.getTenantCode());

            // Build response
            return buildLoginResponse(accessToken, refreshToken, userPrincipal);

        } catch (Exception e) {
            log.error("Authentication failed for user: {}", loginRequest.getUsername(), e);
            throw new AuthenticationException("Authentication failed: " + e.getMessage());
        } finally {
            // CRITICAL: Always clear schema context after login operations
            SchemaContext.clear();
            TenantContext.clear();
            log.info("=== LOGIN ATTEMPT END - Cleared all contexts ===");
        }
    }

    @Transactional
    public LoginResponse refreshToken(String refreshToken) {
        // Clear contexts and force master for refresh token operations
        TenantContext.clear();
        SchemaContext.clear();
        SchemaContext.useMasterSchema();

        try {
            if (!tokenProvider.validateToken(refreshToken)) {
                throw new AuthenticationException("Invalid refresh token");
            }

            if (!"refresh".equals(tokenProvider.getTokenTypeFromToken(refreshToken))) {
                throw new AuthenticationException("Invalid token type");
            }

            String username = tokenProvider.getUsernameFromToken(refreshToken);
            UserPrincipal userPrincipal = (UserPrincipal) userDetailsService.loadUserByUsername(username);

            // Generate new access token
            String newAccessToken = tokenProvider.generateAccessToken(userPrincipal);

            return LoginResponse.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(refreshToken) // Keep the same refresh token
                    .tokenType("Bearer")
                    .expiresIn(tokenProvider.getAccessTokenValidityMs() / 1000)
                    .user(buildUserInfo(userPrincipal))
                    .tenant(buildTenantInfo(userPrincipal))
                    .build();
        } finally {
            // Clear contexts after refresh token operations
            SchemaContext.clear();
            TenantContext.clear();
        }
    }

    private boolean isAccountLocked(User user) {
        return user.getAccountLockedUntil() != null &&
                user.getAccountLockedUntil().isAfter(LocalDateTime.now());
    }

    private void handleFailedLogin(User user) {
        // Ensure we're using master schema for user updates
        String currentSchema = SchemaContext.getCurrentSchema();
        if (!"master".equals(currentSchema)) {
            SchemaContext.useMasterSchema();
        }

        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);

        if (attempts >= MAX_LOGIN_ATTEMPTS) {
            user.setAccountLockedUntil(LocalDateTime.now().plusMinutes(LOCKOUT_DURATION_MINUTES));
            log.warn("Account locked for user: {} due to {} failed login attempts",
                    user.getUsername(), attempts);
        }

        userRepository.save(user);
    }

    private void resetFailedLoginAttempts(User user) {
        // Ensure we're using master schema for user updates
        String currentSchema = SchemaContext.getCurrentSchema();
        if (!"master".equals(currentSchema)) {
            SchemaContext.useMasterSchema();
        }

        if (user.getFailedLoginAttempts() > 0 || user.getAccountLockedUntil() != null) {
            user.setFailedLoginAttempts(0);
            user.setAccountLockedUntil(null);
            userRepository.save(user);
        }
    }

    private LoginResponse buildLoginResponse(String accessToken, String refreshToken, UserPrincipal userPrincipal) {
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(tokenProvider.getAccessTokenValidityMs() / 1000)
                .user(buildUserInfo(userPrincipal))
                .tenant(buildTenantInfo(userPrincipal))
                .build();
    }

    private LoginResponse.UserInfo buildUserInfo(UserPrincipal userPrincipal) {
        return LoginResponse.UserInfo.builder()
                .id(userPrincipal.getId())
                .username(userPrincipal.getUsername())
                .email(userPrincipal.getEmail())
                .userType(userPrincipal.getUserType())
                .build();
    }

    private LoginResponse.TenantInfo buildTenantInfo(UserPrincipal userPrincipal) {
        if (userPrincipal.getTenantId() == null) {
            return null; // Super admin has no tenant
        }

        // Ensure we're using master schema for tenant lookup
        String currentSchema = SchemaContext.getCurrentSchema();
        if (!"master".equals(currentSchema)) {
            SchemaContext.useMasterSchema();
        }

        Tenant tenant = tenantRepository.findById(userPrincipal.getTenantId()).orElse(null);
        if (tenant == null) {
            return null;
        }

        return LoginResponse.TenantInfo.builder()
                .id(tenant.getId())
                .tenantName(tenant.getTenantName())
                .tenantCode(tenant.getTenantCode())
                .schemaName(tenant.getSchemaName())
                .build();
    }

}
