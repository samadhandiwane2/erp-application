package com.erp.tenant.controller;

import com.erp.common.dto.ApiResponse;
import com.erp.common.jwt.UserPrincipal;
import com.erp.tenant.dto.academicYear.AcademicYearResponse;
import com.erp.tenant.dto.academicYear.CreateAcademicYearRequest;
import com.erp.tenant.dto.academicYear.UpdateAcademicYearRequest;
import com.erp.tenant.service.AcademicYearService;
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
@RequestMapping("/api/tenant/academic-years")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'TENANT_MANAGER')")
public class AcademicYearController {

    private final AcademicYearService academicYearService;

    @PostMapping
    public ResponseEntity<ApiResponse<AcademicYearResponse>> createAcademicYear(@Valid @RequestBody CreateAcademicYearRequest request,
                                                                                @AuthenticationPrincipal UserPrincipal currentUser) {

        log.info("Creating academic year: {} by user: {}",
                request.getYearName(), currentUser.getUsername());

        try {
            AcademicYearResponse response = academicYearService.createAcademicYear(request, currentUser);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Academic year created successfully", response));
        } catch (Exception e) {
            log.error("Failed to create academic year: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "ACADEMIC_YEAR_CREATION_FAILED"));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AcademicYearResponse>> updateAcademicYear(@PathVariable Long id, @Valid @RequestBody UpdateAcademicYearRequest request,
                                                                                @AuthenticationPrincipal UserPrincipal currentUser) {

        log.info("Updating academic year: {} by user: {}", id, currentUser.getUsername());

        try {
            AcademicYearResponse response = academicYearService.updateAcademicYear(id, request, currentUser);
            return ResponseEntity.ok(ApiResponse.success("Academic year updated successfully", response));
        } catch (Exception e) {
            log.error("Failed to update academic year: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "ACADEMIC_YEAR_UPDATE_FAILED"));
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'TENANT_MANAGER', 'TENANT_USER')")
    public ResponseEntity<ApiResponse<AcademicYearResponse>> getAcademicYear(@PathVariable Long id) {
        try {
            AcademicYearResponse response = academicYearService.getAcademicYearById(id);
            return ResponseEntity.ok(ApiResponse.success("Academic year retrieved successfully", response));
        } catch (Exception e) {
            log.error("Failed to get academic year: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "ACADEMIC_YEAR_NOT_FOUND"));
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'TENANT_MANAGER', 'TENANT_USER')")
    public ResponseEntity<ApiResponse<List<AcademicYearResponse>>> getAllAcademicYears() {
        try {
            List<AcademicYearResponse> response = academicYearService.getAllAcademicYears();
            return ResponseEntity.ok(ApiResponse.success("Academic years retrieved successfully", response));
        } catch (Exception e) {
            log.error("Failed to get academic years: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "ACADEMIC_YEARS_RETRIEVAL_FAILED"));
        }
    }

    @GetMapping("/current")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'TENANT_MANAGER', 'TENANT_USER')")
    public ResponseEntity<ApiResponse<AcademicYearResponse>> getCurrentAcademicYear() {
        try {
            AcademicYearResponse response = academicYearService.getCurrentAcademicYear();
            return ResponseEntity.ok(ApiResponse.success("Current academic year retrieved successfully", response));
        } catch (Exception e) {
            log.error("Failed to get current academic year: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "CURRENT_ACADEMIC_YEAR_NOT_FOUND"));
        }
    }

}
