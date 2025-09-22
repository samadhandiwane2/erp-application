package com.erp.tenant.controller;

import com.erp.common.dto.ApiResponse;
import com.erp.common.jwt.UserPrincipal;
import com.erp.tenant.dto.classes.ClassResponse;
import com.erp.tenant.dto.classes.CreateClassRequest;
import com.erp.tenant.dto.classes.UpdateClassRequest;
import com.erp.tenant.service.ClassService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tenant/classes")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'TENANT_MANAGER')")
public class ClassController {

    private final ClassService classService;

    @PostMapping
    public ResponseEntity<ApiResponse<ClassResponse>> createClass(@Valid @RequestBody CreateClassRequest request, @AuthenticationPrincipal UserPrincipal currentUser) {

        log.info("Creating class: {} by user: {}",
                request.getClassName(), currentUser.getUsername());

        try {
            ClassResponse response = classService.createClass(request, currentUser);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Class created successfully", response));
        } catch (Exception e) {
            log.error("Failed to create class: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "CLASS_CREATION_FAILED"));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ClassResponse>> updateClass(@PathVariable Long id, @Valid @RequestBody UpdateClassRequest request,
                                                                  @AuthenticationPrincipal UserPrincipal currentUser) {

        log.info("Updating class: {} by user: {}", id, currentUser.getUsername());

        try {
            ClassResponse response = classService.updateClass(id, request, currentUser);
            return ResponseEntity.ok(ApiResponse.success("Class updated successfully", response));
        } catch (Exception e) {
            log.error("Failed to update class: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "CLASS_UPDATE_FAILED"));
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'TENANT_MANAGER', 'TENANT_USER')")
    public ResponseEntity<ApiResponse<ClassResponse>> getClass(@PathVariable Long id) {
        try {
            ClassResponse response = classService.getClassById(id);
            return ResponseEntity.ok(ApiResponse.success("Class retrieved successfully", response));
        } catch (Exception e) {
            log.error("Failed to get class: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "CLASS_NOT_FOUND"));
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'TENANT_MANAGER', 'TENANT_USER')")
    public ResponseEntity<ApiResponse<List<ClassResponse>>> getAllClasses() {
        try {
            List<ClassResponse> response = classService.getAllClasses();
            return ResponseEntity.ok(ApiResponse.success("Classes retrieved successfully", response));
        } catch (Exception e) {
            log.error("Failed to get classes: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "CLASSES_RETRIEVAL_FAILED"));
        }
    }

}
