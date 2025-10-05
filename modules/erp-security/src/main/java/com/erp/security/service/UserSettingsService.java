package com.erp.security.service;

import com.erp.common.annotation.ForceMasterSchema;
import com.erp.common.dto.user.UserSettingsRequest;
import com.erp.common.dto.user.UserSettingsResponse;
import com.erp.common.entity.User;
import com.erp.common.entity.UserPreferences;
import com.erp.security.exception.AuthenticationException;
import com.erp.common.jwt.UserPrincipal;
import com.erp.security.repository.UserPreferencesRepository;
import com.erp.security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
@ForceMasterSchema
public class UserSettingsService {

    private final UserPreferencesRepository preferencesRepository;
    private final UserRepository userRepository;

    private static final Set<String> VALID_TIMEZONES = Set.of(
            "UTC", "America/New_York", "America/Los_Angeles", "America/Chicago",
            "Europe/London", "Europe/Paris", "Europe/Berlin", "Asia/Tokyo",
            "Asia/Shanghai", "Asia/Kolkata", "Australia/Sydney", "Pacific/Auckland"
    );

    @Transactional
    public UserSettingsResponse getUserSettings(UserPrincipal currentUser) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new AuthenticationException("User not found"));

        UserPreferences preferences = preferencesRepository.findByUserId(currentUser.getId())
                .orElseGet(() -> createDefaultPreferences(user));

        log.debug("Settings retrieved for user: {}", user.getUsername());

        return UserSettingsResponse.builder()
                .timezone(preferences.getTimezone())
                .language(preferences.getLanguage())
                .emailNotifications(preferences.getEmailNotifications())
                .marketingEmails(preferences.getMarketingEmails())
                .securityAlerts(preferences.getSecurityAlerts())
                .theme(preferences.getTheme())
                .dateFormat(preferences.getDateFormat())
                .timeFormat(preferences.getTimeFormat())
                .updatedAt(preferences.getUpdatedAt())
                .build();
    }

    @Transactional
    public UserSettingsResponse updateUserSettings(UserSettingsRequest settingsRequest, UserPrincipal currentUser) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new AuthenticationException("User not found"));

        if (!isValidTimezone(settingsRequest.getTimezone())) {
            throw new AuthenticationException("Invalid timezone: " + settingsRequest.getTimezone());
        }

        Optional<UserPreferences> existingPref = preferencesRepository.findByUserId(currentUser.getId());
        LocalDateTime now = LocalDateTime.now();

        if (existingPref.isEmpty()) {
            int inserted = preferencesRepository.insertPreferences(
                    user.getId(),
                    settingsRequest.getTimezone(),
                    settingsRequest.getLanguage(),
                    settingsRequest.getEmailNotifications(),
                    settingsRequest.getMarketingEmails(),
                    settingsRequest.getSecurityAlerts(),
                    settingsRequest.getTheme(),
                    settingsRequest.getDateFormat(),
                    settingsRequest.getTimeFormat(),
                    now,
                    now
            );

            if (inserted == 0) {
                throw new RuntimeException("Failed to create preferences");
            }

            log.info("Settings created for user: {}", user.getUsername());
        } else {
            UserPreferences preferences = existingPref.get();

            boolean hasChanges = !settingsRequest.getTimezone().equals(preferences.getTimezone()) ||
                    !settingsRequest.getLanguage().equals(preferences.getLanguage()) ||
                    !settingsRequest.getEmailNotifications().equals(preferences.getEmailNotifications()) ||
                    !settingsRequest.getMarketingEmails().equals(preferences.getMarketingEmails()) ||
                    !settingsRequest.getSecurityAlerts().equals(preferences.getSecurityAlerts()) ||
                    !settingsRequest.getTheme().equals(preferences.getTheme()) ||
                    !settingsRequest.getDateFormat().equals(preferences.getDateFormat()) ||
                    !settingsRequest.getTimeFormat().equals(preferences.getTimeFormat());

            if (hasChanges) {
                int updated = preferencesRepository.updatePreferences(
                        user.getId(),
                        settingsRequest.getTimezone(),
                        settingsRequest.getLanguage(),
                        settingsRequest.getEmailNotifications(),
                        settingsRequest.getMarketingEmails(),
                        settingsRequest.getSecurityAlerts(),
                        settingsRequest.getTheme(),
                        settingsRequest.getDateFormat(),
                        settingsRequest.getTimeFormat(),
                        now
                );

                if (updated == 0) {
                    throw new RuntimeException("Failed to update preferences");
                }

                log.info("Settings updated for user: {}", user.getUsername());
            } else {
                log.debug("No settings changes detected for user: {}", user.getUsername());
            }
        }

        UserPreferences finalPreferences = preferencesRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new RuntimeException("Failed to fetch preferences"));

        return UserSettingsResponse.builder()
                .timezone(finalPreferences.getTimezone())
                .language(finalPreferences.getLanguage())
                .emailNotifications(finalPreferences.getEmailNotifications())
                .marketingEmails(finalPreferences.getMarketingEmails())
                .securityAlerts(finalPreferences.getSecurityAlerts())
                .theme(finalPreferences.getTheme())
                .dateFormat(finalPreferences.getDateFormat())
                .timeFormat(finalPreferences.getTimeFormat())
                .updatedAt(finalPreferences.getUpdatedAt())
                .build();
    }

    private UserPreferences createDefaultPreferences(User user) {
        LocalDateTime now = LocalDateTime.now();

        int inserted = preferencesRepository.insertPreferences(
                user.getId(),
                "UTC",
                "en",
                true,
                false,
                true,
                "light",
                "yyyy-MM-dd",
                "HH:mm",
                now,
                now
        );

        if (inserted == 0) {
            throw new RuntimeException("Failed to create default preferences");
        }

        return preferencesRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Failed to fetch created preferences"));
    }

    private boolean isValidTimezone(String timezone) {
        if (VALID_TIMEZONES.contains(timezone)) {
            return true;
        }

        try {
            ZoneId.of(timezone);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}