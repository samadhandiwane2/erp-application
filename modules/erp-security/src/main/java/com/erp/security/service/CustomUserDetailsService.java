package com.erp.security.service;

import com.erp.common.annotation.ForceMasterSchema;
import com.erp.common.entity.Tenant;
import com.erp.common.entity.User;
import com.erp.common.jwt.UserPrincipal;
import com.erp.common.repository.TenantRepository;
import com.erp.security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;

    @Override
    // @Transactional(readOnly = true)  // REMOVED
    @ForceMasterSchema
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Loading user by username: {}", username);

        User user = userRepository.findByUsernameAndIsActiveTrue(username)
                .orElseThrow(() -> {
                    log.error("User not found: {}", username);
                    return new UsernameNotFoundException("User not found: " + username);
                });

        String tenantCode = null;
        if (user.getTenantId() != null) {
            Tenant tenant = tenantRepository.findById(user.getTenantId())
                    .orElse(null);
            if (tenant != null) {
                tenantCode = tenant.getTenantCode();
                log.debug("Found tenant code: {} for user: {}", tenantCode, username);
            }
        }

        log.debug("User loaded successfully: {}, tenantCode: {}", username, tenantCode);
        return UserPrincipal.create(user, tenantCode);
    }

    // @Transactional(readOnly = true)  // REMOVED
    @ForceMasterSchema
    public UserDetails loadUserByUsernameAndTenant(String username, String tenantCode)
            throws UsernameNotFoundException {

        log.debug("Loading user by username: {} and tenantCode: {}", username, tenantCode);

        User user;

        if (tenantCode == null) {
            // Super admin login
            user = userRepository.findByUsernameAndUserTypeAndIsActiveTrue(username, User.UserType.SUPER_ADMIN.name())
                    .orElseThrow(() -> {
                        log.error("Super admin not found: {}", username);
                        return new UsernameNotFoundException("Super admin not found: " + username);
                    });
            log.debug("Super admin user loaded: {}", username);
        } else {
            // Tenant user login
            Tenant tenant = tenantRepository.findByTenantCodeAndIsActiveTrue(tenantCode)
                    .orElseThrow(() -> {
                        log.error("Tenant not found: {}", tenantCode);
                        return new UsernameNotFoundException("Tenant not found: " + tenantCode);
                    });

            user = userRepository.findByUsernameAndTenantIdAndIsActiveTrue(username, tenant.getId())
                    .orElseThrow(() -> {
                        log.error("User not found: {} in tenant: {}", username, tenantCode);
                        return new UsernameNotFoundException(
                                "User not found: " + username + " in tenant: " + tenantCode);
                    });
            log.debug("Tenant user loaded: {} from tenant: {}", username, tenantCode);
        }

        return UserPrincipal.create(user, tenantCode);
    }

}