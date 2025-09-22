package com.erp.admin.controller;

import com.erp.common.dto.*;
import com.erp.common.dto.user.*;
import com.erp.common.jwt.UserPrincipal;
import com.erp.security.service.UserManagementService;
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
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminUserController {

    private final UserManagementService userManagementService;

    @PostMapping("/super-admin")
    public ResponseEntity<ApiResponse<UserResponse>> createSuperAdmin(@Valid @RequestBody CreateSuperAdminRequest request, @AuthenticationPrincipal UserPrincipal currentUser) {

        log.info("Creating super admin user: {} by {}", request.getUsername(), currentUser.getUsername());

        try {
            UserResponse userResponse = userManagementService.createSuperAdmin(request, currentUser);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Super admin created successfully", userResponse));
        } catch (Exception e) {
            log.error("Failed to create super admin: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "USER_CREATION_FAILED"));
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> createUser(@Valid @RequestBody CreateUserRequest request, @AuthenticationPrincipal UserPrincipal currentUser) {

        log.info("Creating user: {} (type: {}) by {}", request.getUsername(), request.getUserType(), currentUser.getUsername());
        try {
            UserResponse userResponse = userManagementService.createTenantUser(request, currentUser);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("User created successfully", userResponse));
        } catch (Exception e) {
            log.error("Failed to create user: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "USER_CREATION_FAILED"));
        }
    }

    @PutMapping("/{userId}/status")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserStatus(@PathVariable Long userId, @Valid @RequestBody UserStatusRequest request,
                                                                      @AuthenticationPrincipal UserPrincipal currentUser) {

        log.info("Updating user status: {} to {} by {}",
                userId, request.getIsActive(), currentUser.getUsername());
        try {
            UserResponse userResponse = userManagementService.updateUserStatus(userId, request, currentUser);
            return ResponseEntity.ok(ApiResponse.success("User status updated successfully", userResponse));
        } catch (Exception e) {
            log.error("Failed to update user status: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "USER_UPDATE_FAILED"));
        }
    }

    @PutMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(@PathVariable Long userId, @Valid @RequestBody UpdateUserRequest request,
                                                                @AuthenticationPrincipal UserPrincipal currentUser) {

        log.info("Updating user: {} by {}", userId, currentUser.getUsername());
        try {
            UserResponse userResponse = userManagementService.updateUser(userId, request, currentUser);
            return ResponseEntity.ok(ApiResponse.success("User updated successfully", userResponse));
        } catch (Exception e) {
            log.error("Failed to update user: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "USER_UPDATE_FAILED"));
        }
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<String>> deleteUser(@PathVariable Long userId, @AuthenticationPrincipal UserPrincipal currentUser) {

        log.info("Deleting user: {} by {}", userId, currentUser.getUsername());

        try {
            userManagementService.deleteUser(userId, currentUser);
            return ResponseEntity.ok(ApiResponse.success("User deleted successfully", null));
        } catch (Exception e) {
            log.error("Failed to delete user: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "USER_DELETION_FAILED"));
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<UserResponse>>> searchUsers(@RequestParam(required = false) String username, @RequestParam(required = false) String email,
                                                                       @RequestParam(required = false) String firstName, @RequestParam(required = false) String lastName,
                                                                       @RequestParam(required = false) String userType, @RequestParam(required = false) Long tenantId,
                                                                       @RequestParam(required = false) String tenantCode, @RequestParam(required = false) Boolean isActive,
                                                                       @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
                                                                       @RequestParam(defaultValue = "createdAt") String sortBy, @RequestParam(defaultValue = "DESC") String sortDirection,
                                                                       @AuthenticationPrincipal UserPrincipal currentUser) {

        try {
            UserSearchRequest searchRequest = new UserSearchRequest();
            searchRequest.setUsername(username);
            searchRequest.setEmail(email);
            searchRequest.setFirstName(firstName);
            searchRequest.setLastName(lastName);
            searchRequest.setUserType(userType != null ? com.erp.common.entity.User.UserType.valueOf(userType) : null);
            searchRequest.setTenantId(tenantId);
            searchRequest.setTenantCode(tenantCode);
            searchRequest.setIsActive(isActive);
            searchRequest.setPage(page);
            searchRequest.setSize(size);
            searchRequest.setSortBy(sortBy);
            searchRequest.setSortDirection(sortDirection);

            Page<UserResponse> users = userManagementService.searchUsers(searchRequest, currentUser);
            return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully", users));
        } catch (Exception e) {
            log.error("Failed to search users: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "USER_SEARCH_FAILED"));
        }
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long userId, @AuthenticationPrincipal UserPrincipal currentUser) {

        try {
            UserResponse userResponse = userManagementService.getUserById(userId, currentUser);
            return ResponseEntity.ok(ApiResponse.success("User retrieved successfully", userResponse));
        } catch (Exception e) {
            log.error("Failed to get user: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "USER_RETRIEVAL_FAILED"));
        }
    }

}
