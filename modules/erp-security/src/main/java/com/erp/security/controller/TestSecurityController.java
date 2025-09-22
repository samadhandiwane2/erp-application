package com.erp.security.controller;

import com.erp.common.dto.ApiResponse;
import com.erp.common.context.TenantContext;
import com.erp.common.jwt.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestSecurityController {

    @GetMapping("/public")
    public ResponseEntity<ApiResponse<String>> publicEndpoint() {
        return ResponseEntity.ok(ApiResponse.success("This is a public endpoint accessible to everyone"));
    }

    @GetMapping("/authenticated")
    public ResponseEntity<ApiResponse<Map<String, Object>>> authenticatedEndpoint(@AuthenticationPrincipal UserPrincipal userPrincipal) {

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("username", userPrincipal.getUsername());
        userInfo.put("email", userPrincipal.getEmail());
        userInfo.put("userType", userPrincipal.getUserType());
        userInfo.put("tenantId", userPrincipal.getTenantId());
        userInfo.put("tenantCode", userPrincipal.getTenantCode());
        userInfo.put("currentTenantFromContext", TenantContext.getCurrentTenantId());

        return ResponseEntity.ok(ApiResponse.success("Authenticated user info", userInfo));
    }

    @GetMapping("/super-admin")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<String>> superAdminEndpoint(@AuthenticationPrincipal UserPrincipal userPrincipal) {

        return ResponseEntity.ok(ApiResponse.success(
                "Hello Super Admin: " + userPrincipal.getUsername() + "! You have access to this endpoint."));
    }

    @GetMapping("/tenant-admin")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<ApiResponse<String>> tenantAdminEndpoint(@AuthenticationPrincipal UserPrincipal userPrincipal) {

        return ResponseEntity.ok(ApiResponse.success(
                "Hello " + userPrincipal.getUserType() + ": " + userPrincipal.getUsername() +
                        "! You have tenant admin access. Tenant: " + userPrincipal.getTenantCode()));
    }

    @GetMapping("/manager")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'TENANT_MANAGER')")
    public ResponseEntity<ApiResponse<String>> managerEndpoint(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        return ResponseEntity.ok(ApiResponse.success(
                "Hello " + userPrincipal.getUserType() + ": " + userPrincipal.getUsername() +
                        "! You have manager level access. Tenant: " + userPrincipal.getTenantCode()));
    }

    @GetMapping("/user")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'TENANT_MANAGER', 'TENANT_USER')")
    public ResponseEntity<ApiResponse<String>> userEndpoint(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        return ResponseEntity.ok(ApiResponse.success(
                "Hello " + userPrincipal.getUserType() + ": " + userPrincipal.getUsername() +
                        "! You have user level access. Tenant: " + userPrincipal.getTenantCode()));
    }
}
