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

    @GetMapping
    public ResponseEntity<ApiResponse<Page<TenantResponse>>> searchTenants(@RequestParam(required = false) String tenantName,
                                                                           @RequestParam(required = false) String tenantCode,
                                                                           @RequestParam(required = false) String contactEmail,
                                                                           @RequestParam(required = false) String status,
                                                                           @RequestParam(required = false) Boolean isActive,
                                                                           @RequestParam(defaultValue = "0") int page,
                                                                           @RequestParam(defaultValue = "20") int size,
                                                                           @RequestParam(defaultValue = "createdAt") String sortBy,
                                                                           @RequestParam(defaultValue = "DESC") String sortDirection) {

        try {
            TenantSearchRequest searchRequest = new TenantSearchRequest();
            searchRequest.setTenantName(tenantName);
            searchRequest.setTenantCode(tenantCode);
            searchRequest.setContactEmail(contactEmail);
            if (status != null) {
                searchRequest.setStatus(com.erp.common.entity.Tenant.TenantStatus.valueOf(status));
            }
            searchRequest.setIsActive(isActive);
            searchRequest.setPage(page);
            searchRequest.setSize(size);
            searchRequest.setSortBy(sortBy);
            searchRequest.setSortDirection(sortDirection);

            Page<TenantResponse> tenants = tenantManagementService.searchTenants(searchRequest);
            return ResponseEntity.ok(ApiResponse.success("Tenants retrieved successfully", tenants));
        } catch (Exception e) {
            log.error("Failed to search tenants: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "TENANT_SEARCH_FAILED"));
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

}
