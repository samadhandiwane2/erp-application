package com.erp.security.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@Slf4j
public class RateLimitingService {

    // In-memory storage for rate limiting (in production, use Redis)
    private final ConcurrentMap<String, UserAttempts> passwordChangeAttempts = new ConcurrentHashMap<>();

    private static final int MAX_PASSWORD_CHANGE_ATTEMPTS = 5; // per hour
    private static final int RATE_LIMIT_WINDOW_HOURS = 1;

    public void checkPasswordChangeRateLimit(Long userId) {
        String key = "password_change_" + userId;
        UserAttempts attempts = passwordChangeAttempts.computeIfAbsent(key, k -> new UserAttempts());

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowStart = now.minusHours(RATE_LIMIT_WINDOW_HOURS);

        // Clean old attempts
        attempts.getAttemptTimes().removeIf(time -> time.isBefore(windowStart));

        // Check if rate limit exceeded
        if (attempts.getAttemptTimes().size() >= MAX_PASSWORD_CHANGE_ATTEMPTS) {
            log.warn("Password change rate limit exceeded for user: {}", userId);
            throw new RuntimeException("Too many password change attempts. Please try again in an hour.");
        }

        // Record this attempt
        attempts.getAttemptTimes().add(now);
    }

    public void clearPasswordChangeAttempts(Long userId) {
        String key = "password_change_" + userId;
        passwordChangeAttempts.remove(key);
    }

    @Getter
    private static class UserAttempts {
        private final java.util.List<LocalDateTime> attemptTimes = new java.util.concurrent.CopyOnWriteArrayList<>();

    }

}
