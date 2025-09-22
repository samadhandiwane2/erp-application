package com.erp.config;

import com.erp.security.service.EmailChangeService;
import com.erp.security.service.PasswordResetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class SchedulingConfig {

    private final PasswordResetService passwordResetService;
    private final EmailChangeService emailChangeService;

    // Cleanup expired password reset tokens every hour
    @Scheduled(fixedRate = 3600000) // 1 hour
    public void cleanupExpiredPasswordResetTokens() {
        log.debug("Running scheduled cleanup of expired password reset tokens");
        passwordResetService.cleanupExpiredTokens();
    }

    // Cleanup expired email verification tokens every hour
    @Scheduled(fixedRate = 3600000) // 1 hour
    public void cleanupExpiredEmailVerificationTokens() {
        log.debug("Running scheduled cleanup of expired email verification tokens");
        emailChangeService.cleanupExpiredTokens();
    }
}
