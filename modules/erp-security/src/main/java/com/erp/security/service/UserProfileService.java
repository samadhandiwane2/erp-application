package com.erp.security.service;

import com.erp.common.annotation.ForceMasterSchema;
import com.erp.common.dto.user.ChangePasswordRequest;
import com.erp.common.dto.user.UserProfileRequest;
import com.erp.common.dto.user.UserProfileResponse;
import com.erp.common.entity.Tenant;
import com.erp.common.entity.User;
import com.erp.security.exception.AuthenticationException;
import com.erp.common.jwt.UserPrincipal;
import com.erp.common.repository.TenantRepository;
import com.erp.security.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
@ForceMasterSchema
public class UserProfileService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final RateLimitingService rateLimitingService;
    private final HttpServletRequest request;

    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(UserPrincipal currentUser) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new AuthenticationException("User not found"));

        Tenant tenant = null;
        if (user.getTenantId() != null) {
            tenant = tenantRepository.findById(user.getTenantId()).orElse(null);
        }

        log.debug("Profile viewed by user: {}", user.getUsername());

        return buildUserProfileResponse(user, tenant);
    }

    @Transactional
    public UserProfileResponse updateUserProfile(UserProfileRequest profileRequest, UserPrincipal currentUser) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new AuthenticationException("User not found"));

        boolean hasChanges = !profileRequest.getFirstName().equals(user.getFirstName()) ||
                !profileRequest.getLastName().equals(user.getLastName()) ||
                !java.util.Objects.equals(normalizePhone(profileRequest.getPhone()), user.getPhone());

        if (hasChanges) {
            int updated = userRepository.updateUserProfile(
                    user.getId(),
                    profileRequest.getFirstName(),
                    profileRequest.getLastName(),
                    user.getEmail(),
                    normalizePhone(profileRequest.getPhone()),
                    LocalDateTime.now(),
                    currentUser.getId()
            );

            if (updated == 0) {
                throw new RuntimeException("Failed to update profile");
            }

            log.info("Profile updated by user: {} from IP: {}",
                    user.getUsername(), getClientIpAddress());
        } else {
            log.debug("No profile changes detected for user: {}", user.getUsername());
        }

        User updatedUser = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new RuntimeException("Failed to fetch updated user"));

        Tenant tenant = null;
        if (updatedUser.getTenantId() != null) {
            tenant = tenantRepository.findById(updatedUser.getTenantId()).orElse(null);
        }

        return buildUserProfileResponse(updatedUser, tenant);
    }

    @Transactional
    public void changePassword(ChangePasswordRequest changePasswordRequest, UserPrincipal currentUser) {
        rateLimitingService.checkPasswordChangeRateLimit(currentUser.getId());

        if (!changePasswordRequest.getNewPassword().equals(changePasswordRequest.getConfirmPassword())) {
            throw new AuthenticationException("New password and confirm password do not match");
        }

        if (changePasswordRequest.getCurrentPassword().equals(changePasswordRequest.getNewPassword())) {
            throw new AuthenticationException("New password must be different from current password");
        }

        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new AuthenticationException("User not found"));

        if (!passwordEncoder.matches(changePasswordRequest.getCurrentPassword(), user.getPasswordHash())) {
            log.warn("Invalid current password attempt for user: {} from IP: {}",
                    user.getUsername(), getClientIpAddress());
            throw new AuthenticationException("Current password is incorrect");
        }

        int updated = userRepository.updateUserPassword(
                user.getId(),
                passwordEncoder.encode(changePasswordRequest.getNewPassword()),
                null,
                0,
                LocalDateTime.now(),
                currentUser.getId()
        );

        if (updated == 0) {
            throw new RuntimeException("Failed to update password");
        }

        rateLimitingService.clearPasswordChangeAttempts(currentUser.getId());

        try {
            emailService.sendPasswordChangeConfirmationEmail(user.getEmail(), user.getFirstName());
        } catch (Exception e) {
            log.error("Failed to send password change confirmation email to: {}", user.getEmail(), e);
        }

        log.info("Password changed successfully for user: {} from IP: {}",
                user.getUsername(), getClientIpAddress());
    }

    private String normalizePhone(String phone) {
        if (phone == null) return null;
        String normalized = phone.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private UserProfileResponse buildUserProfileResponse(User user, Tenant tenant) {
        return UserProfileResponse.builder()
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
                .lastLogin(user.getLastLogin())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .isActive(user.getIsActive())
                .profileCompleteness(calculateProfileCompleteness(user))
                .isAccountLocked(user.getAccountLockedUntil() != null &&
                        user.getAccountLockedUntil().isAfter(LocalDateTime.now()))
                .failedLoginAttempts(user.getFailedLoginAttempts())
                .build();
    }

    private Double calculateProfileCompleteness(User user) {
        int totalFields = 5;
        int completedFields = 0;

        if (user.getUsername() != null && !user.getUsername().trim().isEmpty()) {
            completedFields++;
        }
        if (user.getEmail() != null && !user.getEmail().trim().isEmpty()) {
            completedFields++;
        }
        if (user.getFirstName() != null && !user.getFirstName().trim().isEmpty()) {
            completedFields++;
        }
        if (user.getLastName() != null && !user.getLastName().trim().isEmpty()) {
            completedFields++;
        }
        if (user.getPhone() != null && !user.getPhone().trim().isEmpty()) {
            completedFields++;
        }

        return Math.round((double) completedFields / totalFields * 100.0 * 100.0) / 100.0;
    }

    private String getClientIpAddress() {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

}