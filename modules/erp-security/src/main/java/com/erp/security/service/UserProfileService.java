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
    public UserProfileResponse updateUserProfile(UserProfileRequest request, UserPrincipal currentUser) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new AuthenticationException("User not found"));

        // Update profile fields
        boolean hasChanges = false;

        if (!request.getFirstName().equals(user.getFirstName())) {
            user.setFirstName(request.getFirstName());
            hasChanges = true;
        }

        if (!request.getLastName().equals(user.getLastName())) {
            user.setLastName(request.getLastName());
            hasChanges = true;
        }

        // Handle phone number (can be empty)
        String newPhone = request.getPhone() != null ? request.getPhone().trim() : null;
        if (newPhone != null && newPhone.isEmpty()) {
            newPhone = null;
        }

        if (!java.util.Objects.equals(newPhone, user.getPhone())) {
            user.setPhone(newPhone);
            hasChanges = true;
        }

        if (hasChanges) {
            user.setUpdatedAt(LocalDateTime.now());
            user.setUpdatedBy(currentUser.getId());
            userRepository.save(user);

            log.info("Profile updated by user: {} from IP: {}",
                    user.getUsername(), getClientIpAddress());
        } else {
            log.debug("No profile changes detected for user: {}", user.getUsername());
        }

        Tenant tenant = null;
        if (user.getTenantId() != null) {
            tenant = tenantRepository.findById(user.getTenantId()).orElse(null);
        }

        return buildUserProfileResponse(user, tenant);
    }

    @Transactional
    public void changePassword(ChangePasswordRequest changePasswordRequest, UserPrincipal currentUser) {
        // Check rate limiting
        rateLimitingService.checkPasswordChangeRateLimit(currentUser.getId());

        // Validate password confirmation
        if (!changePasswordRequest.getNewPassword().equals(changePasswordRequest.getConfirmPassword())) {
            throw new AuthenticationException("New password and confirm password do not match");
        }

        // Validate new password is different from current
        if (changePasswordRequest.getCurrentPassword().equals(changePasswordRequest.getNewPassword())) {
            throw new AuthenticationException("New password must be different from current password");
        }

        // Get user from database
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new AuthenticationException("User not found"));

        // Validate current password
        if (!passwordEncoder.matches(changePasswordRequest.getCurrentPassword(), user.getPasswordHash())) {
            log.warn("Invalid current password attempt for user: {} from IP: {}",
                    user.getUsername(), getClientIpAddress());
            throw new AuthenticationException("Current password is incorrect");
        }

        // Update password
        user.setPasswordHash(passwordEncoder.encode(changePasswordRequest.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        user.setUpdatedBy(currentUser.getId());

        // Clear any account lockout (if exists)
        user.setAccountLockedUntil(null);
        user.setFailedLoginAttempts(0);

        userRepository.save(user);

        // Clear rate limiting after successful change
        rateLimitingService.clearPasswordChangeAttempts(currentUser.getId());

        // Send confirmation email
        try {
            emailService.sendPasswordChangeConfirmationEmail(user.getEmail(), user.getFirstName());
        } catch (Exception e) {
            log.error("Failed to send password change confirmation email to: {}", user.getEmail(), e);
            // Don't fail the operation if email fails
        }

        log.info("Password changed successfully for user: {} from IP: {}",
                user.getUsername(), getClientIpAddress());
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
        int totalFields = 5; // username, email, firstName, lastName, phone
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
