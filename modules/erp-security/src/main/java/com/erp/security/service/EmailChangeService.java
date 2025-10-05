package com.erp.security.service;

import com.erp.common.annotation.ForceMasterSchema;
import com.erp.common.dto.user.EmailChangeRequest;
import com.erp.common.dto.user.EmailChangeStatusResponse;
import com.erp.common.entity.EmailVerificationToken;
import com.erp.common.entity.User;
import com.erp.security.exception.AuthenticationException;
import com.erp.common.jwt.UserPrincipal;
import com.erp.security.repository.EmailVerificationTokenRepository;
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
import java.util.Base64;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@ForceMasterSchema
public class EmailChangeService {

    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository tokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final HttpServletRequest request;

    @Value("${app.email.verification-token-expiry-hours:24}")
    private int verificationTokenExpiryHours;

    @Value("${app.email.max-change-requests-per-day:3}")
    private int maxChangeRequestsPerDay;

    private static final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public void requestEmailChange(EmailChangeRequest request, UserPrincipal currentUser) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new AuthenticationException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            log.warn("Invalid password for email change request by user: {} from IP: {}",
                    user.getUsername(), getClientIpAddress());
            throw new AuthenticationException("Current password is incorrect");
        }

        if (request.getNewEmail().equalsIgnoreCase(user.getEmail())) {
            throw new AuthenticationException("New email must be different from current email");
        }

        if (userRepository.existsByEmailAndIsActiveTrue(request.getNewEmail()) == 1) {
            throw new AuthenticationException("Email address is already in use");
        }

        checkRateLimit(user.getId());

        if (tokenRepository.existsByNewEmailAndIsVerifiedFalseAndExpiresAtAfter(
                request.getNewEmail(), LocalDateTime.now())) {
            throw new AuthenticationException("There is already a pending email change request for this address");
        }

        tokenRepository.invalidateAllPendingTokensByUser(user.getId());

        String verificationToken = generateVerificationToken();
        LocalDateTime now = LocalDateTime.now();

        int inserted = tokenRepository.insertToken(
                user.getId(),
                user.getEmail(),
                request.getNewEmail(),
                verificationToken,
                now.plusHours(verificationTokenExpiryHours),
                getClientIpAddress(),
                false,
                now,
                now
        );

        if (inserted == 0) {
            throw new RuntimeException("Failed to create verification token");
        }

        emailService.sendEmailChangeVerificationEmail(
                request.getNewEmail(), user.getFirstName(), verificationToken);

        emailService.sendEmailChangeNotificationEmail(
                user.getEmail(), user.getFirstName(), request.getNewEmail());

        log.info("Email change requested by user: {} from {} to {} from IP: {}",
                user.getUsername(), user.getEmail(), request.getNewEmail(), getClientIpAddress());
    }

    @Transactional
    public void verifyEmailChange(String token) {
        Optional<EmailVerificationToken> tokenOpt = tokenRepository
                .findByVerificationTokenAndIsVerifiedFalseAndExpiresAtAfter(token, LocalDateTime.now());

        if (tokenOpt.isEmpty()) {
            log.warn("Invalid or expired email verification token: {} from IP: {}",
                    token, getClientIpAddress());
            throw new AuthenticationException("Invalid or expired verification token");
        }

        EmailVerificationToken verificationToken = tokenOpt.get();

        User user = userRepository.findById(verificationToken.getUserId())
                .orElseThrow(() -> new AuthenticationException("User not found"));

        if (userRepository.existsByEmailAndIsActiveTrue(verificationToken.getNewEmail()) == 1) {
            throw new AuthenticationException("Email address is no longer available");
        }

        String oldEmail = user.getEmail();
        LocalDateTime now = LocalDateTime.now();

        int userUpdated = userRepository.updateUserEmail(
                user.getId(),
                verificationToken.getNewEmail(),
                now,
                user.getId()
        );

        if (userUpdated == 0) {
            throw new RuntimeException("Failed to update user email");
        }

        int tokenUpdated = tokenRepository.updateTokenVerified(
                verificationToken.getId(),
                true,
                now,
                now
        );

        if (tokenUpdated == 0) {
            throw new RuntimeException("Failed to update token");
        }

        tokenRepository.invalidateAllPendingTokensByUser(user.getId());

        emailService.sendEmailChangeSuccessEmail(verificationToken.getNewEmail(), user.getFirstName());
        emailService.sendEmailChangeNotificationOldEmail(oldEmail, user.getFirstName(), verificationToken.getNewEmail());

        log.info("Email changed successfully for user: {} from {} to {} from IP: {}",
                user.getUsername(), oldEmail, verificationToken.getNewEmail(), getClientIpAddress());
    }

    @Transactional(readOnly = true)
    public EmailChangeStatusResponse getEmailChangeStatus(UserPrincipal currentUser) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new AuthenticationException("User not found"));

        Optional<EmailVerificationToken> pendingToken = tokenRepository
                .findByUserIdAndIsVerifiedFalseAndExpiresAtAfter(user.getId(), LocalDateTime.now());

        return EmailChangeStatusResponse.builder()
                .currentEmail(user.getEmail())
                .pendingEmail(pendingToken.map(EmailVerificationToken::getNewEmail).orElse(null))
                .hasPendingChange(pendingToken.isPresent())
                .changeRequestedAt(pendingToken.map(EmailVerificationToken::getCreatedAt).orElse(null))
                .expiresAt(pendingToken.map(EmailVerificationToken::getExpiresAt).orElse(null))
                .build();
    }

    private void checkRateLimit(Long userId) {
        LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
        long recentRequests = tokenRepository.countByUserIdAndCreatedAtAfter(userId, oneDayAgo);

        if (recentRequests >= maxChangeRequestsPerDay) {
            log.warn("Rate limit exceeded for email change requests for user: {} from IP: {}",
                    userId, getClientIpAddress());
            throw new AuthenticationException(
                    "Too many email change requests. Please try again tomorrow.");
        }
    }

    private String generateVerificationToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
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
        int deletedCount = tokenRepository.deleteExpiredTokens(LocalDateTime.now());
        if (deletedCount > 0) {
            log.info("Cleaned up {} expired email verification tokens", deletedCount);
        }
    }

}