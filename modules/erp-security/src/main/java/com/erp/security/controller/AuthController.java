package com.erp.security.controller;

import com.erp.common.dto.ApiResponse;
import com.erp.common.dto.auth.LoginRequest;
import com.erp.common.dto.auth.LoginResponse;
import com.erp.security.service.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthenticationService authenticationService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("Login attempt for username: {}", loginRequest.getUsername());

        try {
            LoginResponse loginResponse = authenticationService.authenticate(loginRequest);
            return ResponseEntity.ok(ApiResponse.success("Login successful", loginResponse));
        } catch (Exception e) {
            log.error("Login failed for username: {}", loginRequest.getUsername(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "LOGIN_FAILED"));
        }
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(@RequestBody RefreshTokenRequest request) {
        try {
            LoginResponse loginResponse = authenticationService.refreshToken(request.getRefreshToken());
            return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", loginResponse));
        } catch (Exception e) {
            log.error("Token refresh failed", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "TOKEN_REFRESH_FAILED"));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout() {
        // TODO: Implement token blacklisting using Redis
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
    }

    public static class RefreshTokenRequest {
        private String refreshToken;

        public String getRefreshToken() {
            return refreshToken;
        }

        public void setRefreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
        }
    }
}
