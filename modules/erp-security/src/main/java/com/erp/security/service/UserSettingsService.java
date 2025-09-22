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
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
@ForceMasterSchema
public class UserSettingsService {

    private final UserPreferencesRepository preferencesRepository;
    private final UserRepository userRepository;

    // Valid timezone IDs
    private static final Set<String> VALID_TIMEZONES = Set.of(
            "UTC", "America/New_York", "America/Los_Angeles", "America/Chicago",
            "Europe/London", "Europe/Paris", "Europe/Berlin", "Asia/Tokyo",
            "Asia/Shanghai", "Asia/Kolkata", "Australia/Sydney", "Pacific/Auckland"
    );

    @Transactional(readOnly = true)
    public UserSettingsResponse getUserSettings(UserPrincipal currentUser) {
        // Verify user exists
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
    public UserSettingsResponse updateUserSettings(UserSettingsRequest request, UserPrincipal currentUser) {
        // Verify user exists
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new AuthenticationException("User not found"));

        // Validate timezone
        if (!isValidTimezone(request.getTimezone())) {
            throw new AuthenticationException("Invalid timezone: " + request.getTimezone());
        }

        UserPreferences preferences = preferencesRepository.findByUserId(currentUser.getId())
                .orElseGet(() -> createDefaultPreferences(user));

        // Update preferences
        boolean hasChanges = false;

        if (!request.getTimezone().equals(preferences.getTimezone())) {
            preferences.setTimezone(request.getTimezone());
            hasChanges = true;
        }

        if (!request.getLanguage().equals(preferences.getLanguage())) {
            preferences.setLanguage(request.getLanguage());
            hasChanges = true;
        }

        if (!request.getEmailNotifications().equals(preferences.getEmailNotifications())) {
            preferences.setEmailNotifications(request.getEmailNotifications());
            hasChanges = true;
        }

        if (!request.getMarketingEmails().equals(preferences.getMarketingEmails())) {
            preferences.setMarketingEmails(request.getMarketingEmails());
            hasChanges = true;
        }

        if (!request.getSecurityAlerts().equals(preferences.getSecurityAlerts())) {
            preferences.setSecurityAlerts(request.getSecurityAlerts());
            hasChanges = true;
        }

        if (!request.getTheme().equals(preferences.getTheme())) {
            preferences.setTheme(request.getTheme());
            hasChanges = true;
        }

        if (!request.getDateFormat().equals(preferences.getDateFormat())) {
            preferences.setDateFormat(request.getDateFormat());
            hasChanges = true;
        }

        if (!request.getTimeFormat().equals(preferences.getTimeFormat())) {
            preferences.setTimeFormat(request.getTimeFormat());
            hasChanges = true;
        }

        if (hasChanges) {
            preferences.setUpdatedAt(LocalDateTime.now());
            preferencesRepository.save(preferences);

            log.info("Settings updated for user: {}", user.getUsername());
        } else {
            log.debug("No settings changes detected for user: {}", user.getUsername());
        }

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

    private UserPreferences createDefaultPreferences(User user) {
        UserPreferences preferences = new UserPreferences();
        preferences.setUserId(user.getId());
        preferences.setUser(user);
        preferences.setTimezone("UTC");
        preferences.setLanguage("en");
        preferences.setEmailNotifications(true);
        preferences.setMarketingEmails(false);
        preferences.setSecurityAlerts(true);
        preferences.setTheme("light");
        preferences.setDateFormat("yyyy-MM-dd");
        preferences.setTimeFormat("HH:mm");
        preferences.setCreatedAt(LocalDateTime.now());
        preferences.setUpdatedAt(LocalDateTime.now());

        return preferencesRepository.save(preferences);
    }

    private boolean isValidTimezone(String timezone) {
        // First check our predefined list
        if (VALID_TIMEZONES.contains(timezone)) {
            return true;
        }

        // Also check against Java's available timezone IDs
        try {
            ZoneId.of(timezone);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

}
