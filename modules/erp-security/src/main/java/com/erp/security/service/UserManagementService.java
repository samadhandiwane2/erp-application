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
        if (currentUser.getUserType() != User.UserType.SUPER_ADMIN) {
            throw new AccessDeniedException("Only super admins can create super admin users");
        }

        validateUniqueConstraints(request.getUsername(), request.getEmail(), null);

        LocalDateTime now = LocalDateTime.now();
        int inserted = userRepository.insertUser(
                request.getUsername(),
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                request.getFirstName(),
                request.getLastName(),
                request.getPhone(),
                User.UserType.SUPER_ADMIN.name(),
                null,
                false,
                0,
                true,
                now,
                currentUser.getId(),
                now,
                currentUser.getId()
        );

        if (inserted == 0) {
            throw new RuntimeException("Failed to create super admin");
        }

        Long userId = userRepository.getLastInsertId();
        if (userId == null || userId == 0) {
            throw new RuntimeException("Failed to retrieve created user ID");
        }

        User savedUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Failed to fetch created user"));

        log.info("Super admin created: {} by {}", savedUser.getUsername(), currentUser.getUsername());

        return convertToUserResponse(savedUser, null);
    }

    @Transactional
    public UserResponse createTenantUser(CreateUserRequest request, UserPrincipal currentUser) {
        validateCreateUserPermission(request, currentUser);

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

        LocalDateTime now = LocalDateTime.now();
        int inserted = userRepository.insertUser(
                request.getUsername(),
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                request.getFirstName(),
                request.getLastName(),
                request.getPhone(),
                request.getUserType().name(),
                targetTenantId,
                false,
                0,
                true,
                now,
                currentUser.getId(),
                now,
                currentUser.getId()
        );

        if (inserted == 0) {
            throw new RuntimeException("Failed to create user");
        }

        Long userId = userRepository.getLastInsertId();
        if (userId == null || userId == 0) {
            throw new RuntimeException("Failed to retrieve created user ID");
        }

        User savedUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Failed to fetch created user"));

        log.info("User created: {} (type: {}) by {}", savedUser.getUsername(),
                savedUser.getUserType(), currentUser.getUsername());

        return convertToUserResponse(savedUser, tenant);
    }

    @Transactional
    public UserResponse updateUserStatus(Long userId, UserStatusRequest request, UserPrincipal currentUser) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        validateUpdatePermission(user, currentUser);

        if (user.getId().equals(currentUser.getId()) && !request.getIsActive()) {
            throw new IllegalArgumentException("Cannot deactivate your own account");
        }

        LocalDateTime lockedUntil = request.getIsActive() ? null : user.getAccountLockedUntil();
        Integer failedAttempts = request.getIsActive() ? 0 : user.getFailedLoginAttempts();

        int updated = userRepository.updateUserStatusAndSecurity(
                userId,
                request.getIsActive(),
                lockedUntil,
                failedAttempts,
                LocalDateTime.now(),
                currentUser.getId()
        );

        if (updated == 0) {
            throw new RuntimeException("Failed to update user status");
        }

        User updatedUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Failed to fetch updated user"));

        log.info("User {} {} by {} (reason: {})",
                updatedUser.getUsername(),
                request.getIsActive() ? "activated" : "deactivated",
                currentUser.getUsername(),
                request.getReason());

        Tenant tenant = null;
        if (updatedUser.getTenantId() != null) {
            tenant = tenantRepository.findById(updatedUser.getTenantId()).orElse(null);
        }

        return convertToUserResponse(updatedUser, tenant);
    }

    @Transactional
    public UserResponse updateUser(Long userId, UpdateUserRequest request, UserPrincipal currentUser) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        validateUpdatePermission(user, currentUser);
        validateUniqueConstraints(null, request.getEmail(), userId);

        String firstName = request.getFirstName() != null ? request.getFirstName() : user.getFirstName();
        String lastName = request.getLastName() != null ? request.getLastName() : user.getLastName();
        String email = request.getEmail() != null ? request.getEmail() : user.getEmail();
        String phone = request.getPhone() != null ? request.getPhone() : user.getPhone();

        int updated = userRepository.updateUserProfile(
                userId,
                firstName,
                lastName,
                email,
                phone,
                LocalDateTime.now(),
                currentUser.getId()
        );

        if (updated == 0) {
            throw new RuntimeException("Failed to update user");
        }

        User updatedUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Failed to fetch updated user"));

        log.info("User updated: {} by {}", updatedUser.getUsername(), currentUser.getUsername());

        Tenant tenant = null;
        if (updatedUser.getTenantId() != null) {
            tenant = tenantRepository.findById(updatedUser.getTenantId()).orElse(null);
        }

        return convertToUserResponse(updatedUser, tenant);
    }

    @Transactional
    public void deleteUser(Long userId, UserPrincipal currentUser) {
        if (currentUser.getUserType() != User.UserType.SUPER_ADMIN) {
            throw new AccessDeniedException("Only super admins can delete users");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("Cannot delete your own account");
        }

        // Soft delete by setting is_active to false
        int updated = userRepository.updateUserActiveStatus(
                userId,
                false,
                LocalDateTime.now(),
                currentUser.getId()
        );

        if (updated == 0) {
            throw new RuntimeException("Failed to delete user");
        }

        log.info("User deleted: {} by {}", user.getUsername(), currentUser.getUsername());
    }

    public Page<UserResponse> searchUsers(UserSearchRequest request, UserPrincipal currentUser) {
        if (currentUser.getUserType() != User.UserType.SUPER_ADMIN) {
            request.setTenantId(currentUser.getTenantId());
        }

        String sortBy = mapSortField(request.getSortBy());
        Sort sort = Sort.by(Sort.Direction.fromString(request.getSortDirection()), sortBy);
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), sort);

        Page<User> users = userRepository.findUsersWithFilters(
                request.getUsername(),
                request.getEmail(),
                request.getFirstName(),
                request.getLastName(),
                request.getUserType() != null ? request.getUserType().name() : null,
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

    private String mapSortField(String fieldName) {
        return switch (fieldName) {
            case "createdAt" -> "created_at";
            case "updatedAt" -> "updated_at";
            case "firstName" -> "first_name";
            case "lastName" -> "last_name";
            case "userType" -> "user_type";
            case "tenantId" -> "tenant_id";
            case "isActive" -> "is_active";
            case "lastLogin" -> "last_login";
            default -> fieldName;
        };
    }

    private void validateCreateUserPermission(CreateUserRequest request, UserPrincipal currentUser) {
        if (currentUser.getUserType() == User.UserType.SUPER_ADMIN) {
            return;
        }

        if (currentUser.getUserType() == User.UserType.TENANT_ADMIN) {
            if (request.getUserType() == User.UserType.SUPER_ADMIN) {
                throw new AccessDeniedException("Tenant admin cannot create super admin users");
            }

            if (request.getTenantId() != null && !request.getTenantId().equals(currentUser.getTenantId())) {
                throw new AccessDeniedException("Cannot create user for different tenant");
            }

            return;
        }

        throw new AccessDeniedException("Insufficient permissions to create users");
    }

    private Long determineTargetTenant(CreateUserRequest request, UserPrincipal currentUser) {
        if (currentUser.getUserType() == User.UserType.SUPER_ADMIN) {
            if (request.getTenantId() != null) {
                return request.getTenantId();
            }

            if (request.getTenantCode() != null) {
                Tenant tenant = tenantRepository.findByTenantCodeAndIsActiveTrue(request.getTenantCode())
                        .orElseThrow(() -> new IllegalArgumentException("Tenant not found with code: " + request.getTenantCode()));
                return tenant.getId();
            }

            if (request.getUserType() == User.UserType.SUPER_ADMIN) {
                return null;
            }

            throw new IllegalArgumentException("Tenant must be specified for non-super-admin users");
        }

        return currentUser.getTenantId();
    }

    private void validateUpdatePermission(User user, UserPrincipal currentUser) {
        if (currentUser.getUserType() == User.UserType.SUPER_ADMIN) {
            return;
        }

        if (currentUser.getUserType() == User.UserType.TENANT_ADMIN) {
            if (!currentUser.getTenantId().equals(user.getTenantId())) {
                throw new AccessDeniedException("Cannot update user from different tenant");
            }

            if (user.getUserType() == User.UserType.SUPER_ADMIN) {
                throw new AccessDeniedException("Cannot update super admin user");
            }

            return;
        }

        throw new AccessDeniedException("Insufficient permissions to update user");
    }

    private void validateReadPermission(User user, UserPrincipal currentUser) {
        if (currentUser.getUserType() == User.UserType.SUPER_ADMIN) {
            return;
        }

        if (currentUser.getUserType() == User.UserType.TENANT_ADMIN) {
            if (!currentUser.getTenantId().equals(user.getTenantId())) {
                throw new AccessDeniedException("Cannot view user from different tenant");
            }
            return;
        }

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