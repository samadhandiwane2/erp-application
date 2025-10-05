package com.erp.admin.controller;

import com.erp.admin.service.TenantManagementService;
import com.erp.common.dto.ApiResponse;
import com.erp.common.dto.tenant.*;
import com.erp.common.jwt.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/tenants")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class TenantController {

    private final TenantManagementService tenantManagementService;

    @PostMapping
    public ResponseEntity<ApiResponse<TenantResponse>> createTenant(@Valid @RequestBody CreateTenantRequest request, @AuthenticationPrincipal UserPrincipal currentUser) {

        log.info("Creating tenant: {} by super admin: {}", request.getTenantCode(), currentUser.getUsername());

        try {
            TenantResponse response = tenantManagementService.createTenant(request, currentUser);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Tenant created successfully", response));
        } catch (Exception e) {
            log.error("Failed to create tenant: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "TENANT_CREATION_FAILED"));
        }
    }

    @PostMapping("/search")
    public ResponseEntity<ApiResponse<Page<TenantResponse>>> searchTenants(
            @RequestBody TenantSearchRequest searchRequest) {

        try {
            Page<TenantResponse> tenants = tenantManagementService.searchTenants(searchRequest);
            return ResponseEntity.ok(ApiResponse.success("Tenants retrieved successfully", tenants));
        } catch (Exception e) {
            log.error("Failed to search tenants: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "TENANT_SEARCH_FAILED"));
        }
    }

    // Keep the GET endpoint for simple listing without filters
    @GetMapping
    public ResponseEntity<ApiResponse<Page<TenantResponse>>> getTenantsSimple(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        try {
            TenantSearchRequest searchRequest = new TenantSearchRequest();
            searchRequest.setPage(page);
            searchRequest.setSize(size);
            searchRequest.setSortBy("created_at"); // Use snake_case directly
            searchRequest.setSortDirection("DESC");

            Page<TenantResponse> tenants = tenantManagementService.searchTenants(searchRequest);
            return ResponseEntity.ok(ApiResponse.success("Tenants retrieved successfully", tenants));
        } catch (Exception e) {
            log.error("Failed to get tenants: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "TENANT_RETRIEVAL_FAILED"));
        }
    }

    @GetMapping("/{tenantId}")
    public ResponseEntity<ApiResponse<TenantResponse>> getTenant(@PathVariable Long tenantId) {
        try {
            TenantResponse tenant = tenantManagementService.getTenantById(tenantId);
            return ResponseEntity.ok(ApiResponse.success("Tenant retrieved successfully", tenant));
        } catch (Exception e) {
            log.error("Failed to get tenant: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "TENANT_RETRIEVAL_FAILED"));
        }
    }

    @PutMapping("/{tenantId}")
    public ResponseEntity<ApiResponse<TenantResponse>> updateTenant(@PathVariable Long tenantId, @Valid @RequestBody UpdateTenantRequest request,
                                                                    @AuthenticationPrincipal UserPrincipal currentUser) {

        log.info("Updating tenant: {} by super admin: {}", tenantId, currentUser.getUsername());

        try {
            TenantResponse response = tenantManagementService.updateTenant(tenantId, request, currentUser);
            return ResponseEntity.ok(ApiResponse.success("Tenant updated successfully", response));
        } catch (Exception e) {
            log.error("Failed to update tenant: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "TENANT_UPDATE_FAILED"));
        }
    }

    @PutMapping("/{tenantId}/suspend")
    public ResponseEntity<ApiResponse<String>> suspendTenant(@PathVariable Long tenantId, @AuthenticationPrincipal UserPrincipal currentUser) {

        log.info("Suspending tenant: {} by super admin: {}", tenantId, currentUser.getUsername());

        try {
            tenantManagementService.suspendTenant(tenantId, currentUser);
            return ResponseEntity.ok(ApiResponse.success("Tenant suspended successfully", null));
        } catch (Exception e) {
            log.error("Failed to suspend tenant: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "TENANT_SUSPENSION_FAILED"));
        }
    }

    @DeleteMapping("/{tenantId}")
    public ResponseEntity<ApiResponse<String>> deleteTenant(@PathVariable Long tenantId, @AuthenticationPrincipal UserPrincipal currentUser) {

        log.info("Deleting tenant: {} by super admin: {}", tenantId, currentUser.getUsername());

        try {
            tenantManagementService.deleteTenant(tenantId, currentUser);
            return ResponseEntity.ok(ApiResponse.success("Tenant deleted successfully", null));
        } catch (Exception e) {
            log.error("Failed to delete tenant: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "TENANT_DELETION_FAILED"));
        }
    }

    @GetMapping("/list")
    public ResponseEntity<ApiResponse<List<TenantResponse>>> getAllActiveTenants() {
        try {
            List<TenantResponse> tenants = tenantManagementService.getAllActiveTenants();
            return ResponseEntity.ok(ApiResponse.success("Active tenants retrieved successfully", tenants));
        } catch (Exception e) {
            log.error("Failed to list tenants: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "TENANT_LIST_FAILED"));
        }
    }

}
