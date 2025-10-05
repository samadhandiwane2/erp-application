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
    public void requestPasswordReset(ForgotPasswordRequest forgotPasswordRequest) {
        String email = forgotPasswordRequest.getEmail().toLowerCase().trim();

        checkRateLimit(email);

        Optional<User> userOpt = userRepository.findByEmailAndIsActiveTrue(email);

        if (userOpt.isPresent()) {
            User user = userOpt.get();

            resetTokenRepository.markAllAsUsedByEmail(email);

            String resetCode = generateResetCode();
            LocalDateTime now = LocalDateTime.now();

            int inserted = resetTokenRepository.insertResetToken(
                    user.getId(),
                    email,
                    resetCode,
                    now.plusMinutes(resetCodeExpiryMinutes),
                    false,
                    now,
                    getClientIpAddress()
            );

            if (inserted == 0) {
                throw new RuntimeException("Failed to create password reset token");
            }

            emailService.sendPasswordResetEmail(email, user.getFirstName(), resetCode);

            log.info("Password reset requested for user: {} from IP: {}", email, getClientIpAddress());
        } else {
            log.warn("Password reset requested for non-existent email: {} from IP: {}",
                    email, getClientIpAddress());
        }
    }

    @Transactional(readOnly = true)
    public void verifyResetCode(VerifyResetCodeRequest verifyRequest) {
        String email = verifyRequest.getEmail().toLowerCase().trim();
        String code = verifyRequest.getCode().trim();

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
    public void resetPassword(ResetPasswordRequest resetRequest) {
        String email = resetRequest.getEmail().toLowerCase().trim();
        String code = resetRequest.getCode().trim();
        String newPassword = resetRequest.getNewPassword();
        String confirmPassword = resetRequest.getConfirmPassword();

        if (!newPassword.equals(confirmPassword)) {
            throw new AuthenticationException("Passwords do not match");
        }

        Optional<PasswordResetToken> tokenOpt = resetTokenRepository
                .findByEmailAndResetCodeAndIsUsedFalseAndExpiresAtAfter(email, code, LocalDateTime.now());

        if (tokenOpt.isEmpty()) {
            log.warn("Invalid or expired reset code attempted for password reset: {} from IP: {}",
                    email, getClientIpAddress());
            throw new AuthenticationException("Invalid or expired reset code");
        }

        PasswordResetToken resetToken = tokenOpt.get();

        Optional<User> userOpt = userRepository.findById(resetToken.getUserId());
        if (userOpt.isEmpty()) {
            throw new AuthenticationException("User not found");
        }

        User user = userOpt.get();

        int updated = userRepository.updateUserPassword(
                user.getId(),
                passwordEncoder.encode(newPassword),
                null,
                0,
                LocalDateTime.now(),
                user.getId()
        );

        if (updated == 0) {
            throw new RuntimeException("Failed to update password");
        }

        LocalDateTime now = LocalDateTime.now();
        int tokenUpdated = resetTokenRepository.updateResetTokenUsed(
                resetToken.getId(),
                true,
                now
        );

        if (tokenUpdated == 0) {
            throw new RuntimeException("Failed to update reset token");
        }

        resetTokenRepository.markAllAsUsedByEmail(email);

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

    @Transactional
    public void cleanupExpiredTokens() {
        int deletedCount = resetTokenRepository.deleteExpiredTokens(LocalDateTime.now());
        if (deletedCount > 0) {
            log.info("Cleaned up {} expired password reset tokens", deletedCount);
        }
    }

}