package com.erp.security.service;

import com.erp.common.annotation.ForceMasterSchema;
import com.erp.common.dto.user.*;
import com.erp.common.entity.Tenant;
import com.erp.common.entity.User;
import com.erp.common.jwt.UserPrincipal;
import com.erp.common.repository.TenantRepository;
import com.erp.security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@ForceMasterSchema
public class UserManagementService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse createSuperAdmin(CreateSuperAdminRequest request, UserPrincipal currentUser) {
        // Only super admins can create other super admins
        if (currentUser.getUserType() != User.UserType.SUPER_ADMIN) {
            throw new AccessDeniedException("Only super admins can create super admin users");
        }

        validateUniqueConstraints(request.getUsername(), request.getEmail(), null);

        User superAdmin = new User();
        superAdmin.setUsername(request.getUsername());
        superAdmin.setEmail(request.getEmail());
        superAdmin.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        superAdmin.setFirstName(request.getFirstName());
        superAdmin.setLastName(request.getLastName());
        superAdmin.setPhone(request.getPhone());
        superAdmin.setUserType(User.UserType.SUPER_ADMIN);
        superAdmin.setTenantId(null); // Super admin belongs to no tenant
        superAdmin.setIsActive(true);
        superAdmin.setCreatedAt(LocalDateTime.now());
        superAdmin.setCreatedBy(currentUser.getId());

        User savedUser = userRepository.save(superAdmin);
        log.info("Super admin created: {} by {}", savedUser.getUsername(), currentUser.getUsername());

        return convertToUserResponse(savedUser, null);
    }

    @Transactional
    public UserResponse createTenantUser(CreateUserRequest request, UserPrincipal currentUser) {
        validateCreateUserPermission(request, currentUser);

        // Determine a tenant for the new user
        Long targetTenantId = determineTargetTenant(request, currentUser);
        Tenant tenant = null;

        if (targetTenantId != null) {
            tenant = tenantRepository.findById(targetTenantId)
                    .orElseThrow(() -> new IllegalArgumentException("Tenant not found"));

            if (!tenant.getIsActive()) {
                throw new IllegalArgumentException("Cannot create user for inactive tenant");
            }
        }

        validateUniqueConstraints(request.getUsername(), request.getEmail(), null);

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhone(request.getPhone());
        user.setUserType(request.getUserType());
        user.setTenantId(targetTenantId);
        user.setIsActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setCreatedBy(currentUser.getId());

        User savedUser = userRepository.save(user);
        log.info("User created: {} (type: {}) by {}", savedUser.getUsername(),
                savedUser.getUserType(), currentUser.getUsername());

        return convertToUserResponse(savedUser, tenant);
    }

    @Transactional
    public UserResponse updateUserStatus(Long userId, UserStatusRequest request, UserPrincipal currentUser) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        validateUpdatePermission(user, currentUser);

        // Prevent self-deactivation
        if (user.getId().equals(currentUser.getId()) && !request.getIsActive()) {
            throw new IllegalArgumentException("Cannot deactivate your own account");
        }

        user.setIsActive(request.getIsActive());
        user.setUpdatedAt(LocalDateTime.now());
        user.setUpdatedBy(currentUser.getId());

        // Clear account lockout when activating
        if (request.getIsActive()) {
            user.setAccountLockedUntil(null);
            user.setFailedLoginAttempts(0);
        }

        User savedUser = userRepository.save(user);

        log.info("User {} {} by {} (reason: {})",
                savedUser.getUsername(),
                request.getIsActive() ? "activated" : "deactivated",
                currentUser.getUsername(),
                request.getReason());

        Tenant tenant = null;
        if (savedUser.getTenantId() != null) {
            tenant = tenantRepository.findById(savedUser.getTenantId()).orElse(null);
        }

        return convertToUserResponse(savedUser, tenant);
    }

    @Transactional
    public UserResponse updateUser(Long userId, UpdateUserRequest request, UserPrincipal currentUser) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        validateUpdatePermission(user, currentUser);
        validateUniqueConstraints(null, request.getEmail(), userId);

        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }

        user.setUpdatedAt(LocalDateTime.now());
        user.setUpdatedBy(currentUser.getId());

        User savedUser = userRepository.save(user);
        log.info("User updated: {} by {}", savedUser.getUsername(), currentUser.getUsername());

        Tenant tenant = null;
        if (savedUser.getTenantId() != null) {
            tenant = tenantRepository.findById(savedUser.getTenantId()).orElse(null);
        }

        return convertToUserResponse(savedUser, tenant);
    }

    @Transactional
    public void deleteUser(Long userId, UserPrincipal currentUser) {
        // Only super admins can delete users
        if (currentUser.getUserType() != User.UserType.SUPER_ADMIN) {
            throw new AccessDeniedException("Only super admins can delete users");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Prevent self-deletion
        if (user.getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("Cannot delete your own account");
        }

        userRepository.delete(user);
        log.info("User deleted: {} by {}", user.getUsername(), currentUser.getUsername());
    }

    public Page<UserResponse> searchUsers(UserSearchRequest request, UserPrincipal currentUser) {
        // Apply tenant filtering based on the current user
        if (currentUser.getUserType() != User.UserType.SUPER_ADMIN) {
            request.setTenantId(currentUser.getTenantId());
        }

        Sort sort = Sort.by(Sort.Direction.fromString(request.getSortDirection()), request.getSortBy());
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), sort);

        Page<User> users = userRepository.findUsersWithFilters(
                request.getUsername(),
                request.getEmail(),
                request.getFirstName(),
                request.getLastName(),
                request.getUserType(),
                request.getTenantId(),
                request.getIsActive(),
                pageable
        );

        return users.map(user -> {
            Tenant tenant = null;
            if (user.getTenantId() != null) {
                tenant = tenantRepository.findById(user.getTenantId()).orElse(null);
            }
            return convertToUserResponse(user, tenant);
        });
    }

    public UserResponse getUserById(Long userId, UserPrincipal currentUser) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        validateReadPermission(user, currentUser);

        Tenant tenant = null;
        if (user.getTenantId() != null) {
            tenant = tenantRepository.findById(user.getTenantId()).orElse(null);
        }

        return convertToUserResponse(user, tenant);
    }

    private void validateCreateUserPermission(CreateUserRequest request, UserPrincipal currentUser) {
        if (currentUser.getUserType() == User.UserType.SUPER_ADMIN) {
            // Super admin can create any type of user
            return;
        }

        if (currentUser.getUserType() == User.UserType.TENANT_ADMIN) {
            // Tenant admin can only create users in their own tenant
            if (request.getUserType() == User.UserType.SUPER_ADMIN) {
                throw new AccessDeniedException("Tenant admin cannot create super admin users");
            }

            // If a tenant is specified, it must match the current user's tenant
            if (request.getTenantId() != null && !request.getTenantId().equals(currentUser.getTenantId())) {
                throw new AccessDeniedException("Cannot create user for different tenant");
            }

            return;
        }

        throw new AccessDeniedException("Insufficient permissions to create users");
    }

    private Long determineTargetTenant(CreateUserRequest request, UserPrincipal currentUser) {
        if (currentUser.getUserType() == User.UserType.SUPER_ADMIN) {
            // Super admin can specify tenant explicitly
            if (request.getTenantId() != null) {
                return request.getTenantId();
            }

            if (request.getTenantCode() != null) {
                Tenant tenant = tenantRepository.findByTenantCodeAndIsActiveTrue(request.getTenantCode())
                        .orElseThrow(() -> new IllegalArgumentException("Tenant not found with code: " + request.getTenantCode()));
                return tenant.getId();
            }

            // For super admin users, no tenant
            if (request.getUserType() == User.UserType.SUPER_ADMIN) {
                return null;
            }

            throw new IllegalArgumentException("Tenant must be specified for non-super-admin users");
        }

        // Tenant admin creates users in their own tenant
        return currentUser.getTenantId();
    }

    private void validateUpdatePermission(User user, UserPrincipal currentUser) {
        if (currentUser.getUserType() == User.UserType.SUPER_ADMIN) {
            return; // Super admin can update anyone
        }

        if (currentUser.getUserType() == User.UserType.TENANT_ADMIN) {
            // Tenant admin can only update users in their tenant
            if (!currentUser.getTenantId().equals(user.getTenantId())) {
                throw new AccessDeniedException("Cannot update user from different tenant");
            }

            // Cannot update super admin
            if (user.getUserType() == User.UserType.SUPER_ADMIN) {
                throw new AccessDeniedException("Cannot update super admin user");
            }

            return;
        }

        throw new AccessDeniedException("Insufficient permissions to update user");
    }

    private void validateReadPermission(User user, UserPrincipal currentUser) {
        if (currentUser.getUserType() == User.UserType.SUPER_ADMIN) {
            return; // Super admin can read anyone
        }

        if (currentUser.getUserType() == User.UserType.TENANT_ADMIN) {
            // Tenant admin can read users in their tenant
            if (!currentUser.getTenantId().equals(user.getTenantId())) {
                throw new AccessDeniedException("Cannot view user from different tenant");
            }
            return;
        }

        // Regular users can only view themselves
        if (!user.getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Cannot view other users");
        }
    }

    private void validateUniqueConstraints(String username, String email, Long excludeUserId) {
        if (username != null) {
            Optional<User> existingByUsername = userRepository.findByUsernameAndIsActiveTrue(username);
            if (existingByUsername.isPresent() &&
                    (excludeUserId == null || !existingByUsername.get().getId().equals(excludeUserId))) {
                throw new IllegalArgumentException("Username already exists");
            }
        }

        if (email != null) {
            Optional<User> existingByEmail = userRepository.findByEmailAndIsActiveTrue(email);
            if (existingByEmail.isPresent() &&
                    (excludeUserId == null || !existingByEmail.get().getId().equals(excludeUserId))) {
                throw new IllegalArgumentException("Email already exists");
            }
        }
    }

    private UserResponse convertToUserResponse(User user, Tenant tenant) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .userType(user.getUserType())
                .tenantId(user.getTenantId())
                .tenantCode(tenant != null ? tenant.getTenantCode() : null)
                .tenantName(tenant != null ? tenant.getTenantName() : null)
                .isActive(user.getIsActive())
                .lastLogin(user.getLastLogin())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .failedLoginAttempts(user.getFailedLoginAttempts())
                .accountLockedUntil(user.getAccountLockedUntil())
                .build();
    }
}
