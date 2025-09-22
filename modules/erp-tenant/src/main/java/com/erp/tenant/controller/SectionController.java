package com.erp.tenant.controller;

import com.erp.common.annotation.ForceTenantSchema;
import com.erp.common.dto.ApiResponse;
import com.erp.common.jwt.UserPrincipal;
import com.erp.tenant.dto.section.CreateSectionRequest;
import com.erp.tenant.dto.section.SectionResponse;
import com.erp.tenant.dto.section.UpdateSectionRequest;
import com.erp.tenant.service.SectionService;
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
@RequestMapping("/api/tenant/sections")
@RequiredArgsConstructor
@ForceTenantSchema
@Slf4j
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'TENANT_MANAGER')")
public class SectionController {

    private final SectionService sectionService;

    @PostMapping
    public ResponseEntity<ApiResponse<SectionResponse>> createSection(@Valid @RequestBody CreateSectionRequest request,
                                                                      @AuthenticationPrincipal UserPrincipal currentUser) {

        log.info("Creating section: {} for class: {} by user: {}",
                request.getSectionName(), request.getClassId(), currentUser.getUsername());

        try {
            SectionResponse response = sectionService.createSection(request, currentUser);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Section created successfully", response));
        } catch (Exception e) {
            log.error("Failed to create section: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "SECTION_CREATION_FAILED"));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SectionResponse>> updateSection(@PathVariable Long id, @Valid @RequestBody UpdateSectionRequest request,
                                                                      @AuthenticationPrincipal UserPrincipal currentUser) {

        log.info("Updating section: {} by user: {}", id, currentUser.getUsername());

        try {
            SectionResponse response = sectionService.updateSection(id, request, currentUser);
            return ResponseEntity.ok(ApiResponse.success("Section updated successfully", response));
        } catch (Exception e) {
            log.error("Failed to update section: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "SECTION_UPDATE_FAILED"));
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'TENANT_MANAGER', 'TENANT_USER')")
    public ResponseEntity<ApiResponse<SectionResponse>> getSection(@PathVariable Long id) {
        try {
            SectionResponse response = sectionService.getSectionById(id);
            return ResponseEntity.ok(ApiResponse.success("Section retrieved successfully", response));
        } catch (Exception e) {
            log.error("Failed to get section: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "SECTION_NOT_FOUND"));
        }
    }

    @GetMapping("/class/{classId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'TENANT_MANAGER', 'TENANT_USER')")
    public ResponseEntity<ApiResponse<List<SectionResponse>>> getSectionsByClass(@PathVariable Long classId) {
        try {
            List<SectionResponse> response = sectionService.getSectionsByClassId(classId);
            return ResponseEntity.ok(ApiResponse.success("Sections retrieved successfully", response));
        } catch (Exception e) {
            log.error("Failed to get sections for class: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "SECTIONS_RETRIEVAL_FAILED"));
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'TENANT_MANAGER', 'TENANT_USER')")
    public ResponseEntity<ApiResponse<List<SectionResponse>>> getAllSections() {
        try {
            List<SectionResponse> response = sectionService.getAllSections();
            return ResponseEntity.ok(ApiResponse.success("Sections retrieved successfully", response));
        } catch (Exception e) {
            log.error("Failed to get sections: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "SECTIONS_RETRIEVAL_FAILED"));
        }
    }

}
