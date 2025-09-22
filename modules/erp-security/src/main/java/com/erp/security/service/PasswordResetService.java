package com.erp.security.service;

import com.erp.common.annotation.ForceMasterSchema;
import com.erp.common.dto.auth.ForgotPasswordRequest;
import com.erp.common.dto.auth.ResetPasswordRequest;
import com.erp.common.dto.auth.VerifyResetCodeRequest;
import com.erp.common.entity.PasswordResetToken;
import com.erp.common.entity.User;
import com.erp.security.exception.AuthenticationException;
import com.erp.security.repository.PasswordResetTokenRepository;
import com.erp.security.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@ForceMasterSchema
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final HttpServletRequest request;

    @Value("${app.email.reset-code-expiry-minutes:30}")
    private int resetCodeExpiryMinutes;

    @Value("${app.email.max-reset-attempts-per-hour:3}")
    private int maxResetAttemptsPerHour;

    private static final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public void requestPasswordReset(ForgotPasswordRequest request) {
        String email = request.getEmail().toLowerCase().trim();

        // Check rate limiting
        checkRateLimit(email);

        // Find a user by email (return same response whether user exists or not for security)
        Optional<User> userOpt = userRepository.findByEmailAndIsActiveTrue(email);

        if (userOpt.isPresent()) {
            User user = userOpt.get();

            // Invalidate any existing reset codes for this user
            resetTokenRepository.markAllAsUsedByEmail(email);

            // Generate new reset code
            String resetCode = generateResetCode();

            // Create reset token
            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setUserId(user.getId());
            resetToken.setEmail(email);
            resetToken.setResetCode(resetCode);
            resetToken.setExpiresAt(LocalDateTime.now().plusMinutes(resetCodeExpiryMinutes));
            resetToken.setIpAddress(getClientIpAddress());

            resetTokenRepository.save(resetToken);

            // Send email
            emailService.sendPasswordResetEmail(email, user.getFirstName(), resetCode);

            log.info("Password reset requested for user: {} from IP: {}", email, getClientIpAddress());
        } else {
            log.warn("Password reset requested for non-existent email: {} from IP: {}",
                    email, getClientIpAddress());
        }

        // Always return a success response (don't reveal if email exists)
    }

    @Transactional(readOnly = true)
    public void verifyResetCode(VerifyResetCodeRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        String code = request.getCode().trim();

        Optional<PasswordResetToken> tokenOpt = resetTokenRepository
                .findByEmailAndResetCodeAndIsUsedFalseAndExpiresAtAfter(email, code, LocalDateTime.now());

        if (tokenOpt.isEmpty()) {
            log.warn("Invalid or expired reset code attempted for email: {} from IP: {}",
                    email, getClientIpAddress());
            throw new AuthenticationException("Invalid or expired reset code");
        }

        log.info("Reset code verified successfully for email: {}", email);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        String code = request.getCode().trim();
        String newPassword = request.getNewPassword();
        String confirmPassword = request.getConfirmPassword();

        // Validate password confirmation
        if (!newPassword.equals(confirmPassword)) {
            throw new AuthenticationException("Passwords do not match");
        }

        // Find valid reset token
        Optional<PasswordResetToken> tokenOpt = resetTokenRepository
                .findByEmailAndResetCodeAndIsUsedFalseAndExpiresAtAfter(email, code, LocalDateTime.now());

        if (tokenOpt.isEmpty()) {
            log.warn("Invalid or expired reset code attempted for password reset: {} from IP: {}",
                    email, getClientIpAddress());
            throw new AuthenticationException("Invalid or expired reset code");
        }

        PasswordResetToken resetToken = tokenOpt.get();

        // Find and update user
        Optional<User> userOpt = userRepository.findById(resetToken.getUserId());
        if (userOpt.isEmpty()) {
            throw new AuthenticationException("User not found");
        }

        User user = userOpt.get();

        // Update password
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());

        // Clear account lockout if exists
        user.setAccountLockedUntil(null);
        user.setFailedLoginAttempts(0);

        userRepository.save(user);

        // Mark the reset token as used
        resetToken.setIsUsed(true);
        resetToken.setUsedAt(LocalDateTime.now());
        resetTokenRepository.save(resetToken);

        // Invalidate any other reset codes for this user
        resetTokenRepository.markAllAsUsedByEmail(email);

        // Send success email
        emailService.sendPasswordResetSuccessEmail(email, user.getFirstName());

        log.info("Password reset successfully completed for user: {} from IP: {}",
                email, getClientIpAddress());
    }

    private void checkRateLimit(String email) {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        long recentAttempts = resetTokenRepository.countByEmailAndCreatedAtAfter(email, oneHourAgo);

        if (recentAttempts >= maxResetAttemptsPerHour) {
            log.warn("Rate limit exceeded for password reset requests: {} from IP: {}",
                    email, getClientIpAddress());
            throw new AuthenticationException(
                    "Too many password reset requests. Please try again in an hour.");
        }
    }

    private String generateResetCode() {
        // Generate 6-digit number
        int code = 100000 + secureRandom.nextInt(900000);
        return String.valueOf(code);
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

    // Cleanup job method (to be called by a scheduled task)
    @Transactional
    public void cleanupExpiredTokens() {
        int deletedCount = resetTokenRepository.deleteExpiredTokens(LocalDateTime.now());
        if (deletedCount > 0) {
            log.info("Cleaned up {} expired password reset tokens", deletedCount);
        }
    }

}
