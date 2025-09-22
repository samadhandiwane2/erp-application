package com.erp.tenant.controller;

import com.erp.common.dto.ApiResponse;
import com.erp.common.jwt.UserPrincipal;
import com.erp.tenant.dto.student.BulkPromotionRequest;
import com.erp.tenant.dto.student.PromotionRequest;
import com.erp.tenant.dto.student.PromotionResponse;
import com.erp.tenant.entity.StudentClassHistory;
import com.erp.tenant.entity.StudentPromotion;
import com.erp.tenant.service.StudentPromotionService;
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
@RequestMapping("/api/tenant/students/promotions")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'TENANT_MANAGER')")
public class StudentPromotionController {

    private final StudentPromotionService promotionService;

    @PostMapping("/promote")
    public ResponseEntity<ApiResponse<PromotionResponse>> promoteStudent(@Valid @RequestBody PromotionRequest request, @AuthenticationPrincipal UserPrincipal currentUser) {

        log.info("Promoting student: {} by user: {}", request.getStudentId(), currentUser.getUsername());

        try {
            PromotionResponse response = promotionService.promoteStudent(request, currentUser);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Student promoted successfully", response));
        } catch (Exception e) {
            log.error("Failed to promote student: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "PROMOTION_FAILED"));
        }
    }

    @PostMapping("/bulk-promote")
    public ResponseEntity<ApiResponse<List<PromotionResponse>>> bulkPromoteStudents(@Valid @RequestBody BulkPromotionRequest request,
                                                                                    @AuthenticationPrincipal UserPrincipal currentUser) {

        log.info("Bulk promoting students from class: {} to class: {} by user: {}",
                request.getFromClassId(), request.getToClassId(), currentUser.getUsername());

        try {
            List<PromotionResponse> responses = promotionService.bulkPromoteStudents(request, currentUser);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(
                            String.format("Successfully promoted %d students", responses.size()),
                            responses));
        } catch (Exception e) {
            log.error("Failed to bulk promote students: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "BULK_PROMOTION_FAILED"));
        }
    }

    @GetMapping("/history/{studentId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'TENANT_MANAGER', 'TENANT_USER')")
    public ResponseEntity<ApiResponse<List<StudentClassHistory>>> getStudentHistory(@PathVariable Long studentId) {

        log.debug("Fetching class history for student: {}", studentId);

        try {
            List<StudentClassHistory> history = promotionService.getStudentHistory(studentId);
            return ResponseEntity.ok(ApiResponse.success("Student history retrieved successfully", history));
        } catch (Exception e) {
            log.error("Failed to get student history: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "HISTORY_RETRIEVAL_FAILED"));
        }
    }

    @GetMapping("/records/{studentId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'TENANT_MANAGER', 'TENANT_USER')")
    public ResponseEntity<ApiResponse<List<StudentPromotion>>> getStudentPromotions(@PathVariable Long studentId) {

        log.debug("Fetching promotion records for student: {}", studentId);

        try {
            List<StudentPromotion> promotions = promotionService.getStudentPromotions(studentId);
            return ResponseEntity.ok(ApiResponse.success("Promotion records retrieved successfully", promotions));
        } catch (Exception e) {
            log.error("Failed to get promotion records: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "PROMOTION_RECORDS_RETRIEVAL_FAILED"));
        }
    }

    @PostMapping("/history/record")
    public ResponseEntity<ApiResponse<String>> recordClassHistory(@RequestParam Long studentId, @RequestParam Long academicYearId,
                                                                  @RequestParam Long classId,
                                                                  @RequestParam Long sectionId,
                                                                  @AuthenticationPrincipal UserPrincipal currentUser) {

        log.info("Recording class history for student: {} in class: {} by user: {}",
                studentId, classId, currentUser.getUsername());

        try {
            promotionService.recordClassHistory(studentId, academicYearId, classId, sectionId, currentUser);
            return ResponseEntity.ok(ApiResponse.success("Class history recorded successfully", null));
        } catch (Exception e) {
            log.error("Failed to record class history: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "HISTORY_RECORDING_FAILED"));
        }
    }

}
