package com.erp.security.controller;

import com.erp.common.dto.*;
import com.erp.common.dto.user.*;
import com.erp.common.jwt.UserPrincipal;
import com.erp.security.service.EmailChangeService;
import com.erp.security.service.UserProfileService;
import com.erp.security.service.UserSettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Slf4j
public class UserProfileController {

    private final UserProfileService userProfileService;
    private final EmailChangeService emailChangeService;
    private final UserSettingsService userSettingsService;

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getUserProfile(@AuthenticationPrincipal UserPrincipal currentUser) {

        log.debug("Profile requested by user: {}", currentUser.getUsername());

        try {
            UserProfileResponse profile = userProfileService.getUserProfile(currentUser);
            return ResponseEntity.ok(ApiResponse.success("Profile retrieved successfully", profile));
        } catch (Exception e) {
            log.error("Failed to get user profile for: {}", currentUser.getUsername(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "PROFILE_RETRIEVAL_FAILED"));
        }
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateUserProfile(@Valid @RequestBody UserProfileRequest request, @AuthenticationPrincipal UserPrincipal currentUser) {

        log.info("Profile update requested by user: {}", currentUser.getUsername());

        try {
            UserProfileResponse updatedProfile = userProfileService.updateUserProfile(request, currentUser);
            return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", updatedProfile));
        } catch (Exception e) {
            log.error("Profile update failed for user: {}", currentUser.getUsername(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "PROFILE_UPDATE_FAILED"));
        }
    }

    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse<String>> changePassword(@Valid @RequestBody ChangePasswordRequest request, @AuthenticationPrincipal UserPrincipal currentUser) {

        log.info("Password change requested by user: {}", currentUser.getUsername());

        try {
            userProfileService.changePassword(request, currentUser);
            return ResponseEntity.ok(ApiResponse.success(
                    "Password changed successfully. A confirmation email has been sent.", null));
        } catch (Exception e) {
            log.error("Password change failed for user: {}", currentUser.getUsername(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "PASSWORD_CHANGE_FAILED"));
        }
    }

    @PostMapping("/email-change-request")
    public ResponseEntity<ApiResponse<String>> requestEmailChange(@Valid @RequestBody EmailChangeRequest request, @AuthenticationPrincipal UserPrincipal currentUser) {

        log.info("Email change requested by user: {} to new email: {}",
                currentUser.getUsername(), request.getNewEmail());

        try {
            emailChangeService.requestEmailChange(request, currentUser);
            return ResponseEntity.ok(ApiResponse.success(
                    "Verification email sent to new address. Please check your email and click the verification link.", null));
        } catch (Exception e) {
            log.error("Email change request failed for user: {}", currentUser.getUsername(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "EMAIL_CHANGE_REQUEST_FAILED"));
        }
    }

    @GetMapping("/verify-email")
    public ResponseEntity<ApiResponse<String>> verifyEmailChange(@RequestParam String token) {
        log.info("Email verification requested with token: {}", token.substring(0, 8) + "...");

        try {
            emailChangeService.verifyEmailChange(token);
            return ResponseEntity.ok(ApiResponse.success(
                    "Email address verified and updated successfully. You can now use your new email to log in.", null));
        } catch (Exception e) {
            log.error("Email verification failed for token: {}", token.substring(0, 8) + "...", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "EMAIL_VERIFICATION_FAILED"));
        }
    }

    @GetMapping("/email-change-status")
    public ResponseEntity<ApiResponse<EmailChangeStatusResponse>> getEmailChangeStatus(@AuthenticationPrincipal UserPrincipal currentUser) {

        try {
            EmailChangeStatusResponse status = emailChangeService.getEmailChangeStatus(currentUser);
            return ResponseEntity.ok(ApiResponse.success("Email change status retrieved", status));
        } catch (Exception e) {
            log.error("Failed to get email change status for user: {}", currentUser.getUsername(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "EMAIL_STATUS_RETRIEVAL_FAILED"));
        }
    }

    @GetMapping("/settings")
    public ResponseEntity<ApiResponse<UserSettingsResponse>> getUserSettings(@AuthenticationPrincipal UserPrincipal currentUser) {

        log.debug("Settings requested by user: {}", currentUser.getUsername());

        try {
            UserSettingsResponse settings = userSettingsService.getUserSettings(currentUser);
            return ResponseEntity.ok(ApiResponse.success("Settings retrieved successfully", settings));
        } catch (Exception e) {
            log.error("Failed to get user settings for: {}", currentUser.getUsername(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "SETTINGS_RETRIEVAL_FAILED"));
        }
    }

    @PutMapping("/settings")
    public ResponseEntity<ApiResponse<UserSettingsResponse>> updateUserSettings(@Valid @RequestBody UserSettingsRequest request, @AuthenticationPrincipal UserPrincipal currentUser) {

        log.info("Settings update requested by user: {}", currentUser.getUsername());

        try {
            UserSettingsResponse updatedSettings = userSettingsService.updateUserSettings(request, currentUser);
            return ResponseEntity.ok(ApiResponse.success("Settings updated successfully", updatedSettings));
        } catch (Exception e) {
            log.error("Settings update failed for user: {}", currentUser.getUsername(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "SETTINGS_UPDATE_FAILED"));
        }
    }

}
