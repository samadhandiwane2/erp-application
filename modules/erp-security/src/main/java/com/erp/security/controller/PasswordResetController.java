package com.erp.security.controller;

import com.erp.common.dto.ApiResponse;
import com.erp.common.dto.auth.ForgotPasswordRequest;
import com.erp.common.dto.auth.ResetPasswordRequest;
import com.erp.common.dto.auth.VerifyResetCodeRequest;
import com.erp.security.service.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {

        log.info("Password reset requested for email: {}", request.getEmail());
        try {
            passwordResetService.requestPasswordReset(request);
            return ResponseEntity.ok(ApiResponse.success(
                    "If your email is registered, you will receive a reset code shortly.", null));
        } catch (Exception e) {
            log.error("Failed to process password reset request", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "PASSWORD_RESET_REQUEST_FAILED"));
        }
    }

    @PostMapping("/verify-reset-code")
    public ResponseEntity<ApiResponse<String>> verifyResetCode(@Valid @RequestBody VerifyResetCodeRequest request) {

        log.info("Reset code verification requested for email: {}", request.getEmail());
        try {
            passwordResetService.verifyResetCode(request);
            return ResponseEntity.ok(ApiResponse.success(
                    "Reset code verified successfully. You can now reset your password.", null));
        } catch (Exception e) {
            log.error("Reset code verification failed", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "RESET_CODE_VERIFICATION_FAILED"));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {

        log.info("Password reset attempted for email: {}", request.getEmail());
        try {
            passwordResetService.resetPassword(request);
            return ResponseEntity.ok(ApiResponse.success(
                    "Password reset successfully. You can now login with your new password.", null));
        } catch (Exception e) {
            log.error("Password reset failed", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "PASSWORD_RESET_FAILED"));
        }
    }

}
