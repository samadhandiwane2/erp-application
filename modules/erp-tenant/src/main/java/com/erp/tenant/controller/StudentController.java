package com.erp.tenant.controller;

import com.erp.common.dto.ApiResponse;
import com.erp.common.jwt.UserPrincipal;
import com.erp.tenant.dto.student.*;
import com.erp.tenant.entity.Student;
import com.erp.tenant.service.StudentManagementService;
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
@RequestMapping("/api/tenant/students")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'TENANT_MANAGER')")
public class StudentController {

    private final StudentManagementService studentManagementService;

    @PostMapping
    public ResponseEntity<ApiResponse<StudentResponse>> createStudent(@Valid @RequestBody CreateStudentRequest request, @AuthenticationPrincipal UserPrincipal currentUser) {

        log.info("Creating student: {} {} by user: {}",
                request.getFirstName(), request.getLastName(), currentUser.getUsername());

        try {
            StudentResponse response = studentManagementService.createStudent(request, currentUser);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Student created successfully", response));
        } catch (Exception e) {
            log.error("Failed to create student: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "STUDENT_CREATION_FAILED"));
        }
    }

    @PutMapping("/{studentId}")
    public ResponseEntity<ApiResponse<StudentResponse>> updateStudent(@PathVariable Long studentId, @Valid @RequestBody UpdateStudentRequest request,
                                                                      @AuthenticationPrincipal UserPrincipal currentUser) {

        log.info("Updating student: {} by user: {}", studentId, currentUser.getUsername());

        try {
            StudentResponse response = studentManagementService.updateStudent(studentId, request, currentUser);
            return ResponseEntity.ok(ApiResponse.success("Student updated successfully", response));
        } catch (Exception e) {
            log.error("Failed to update student: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "STUDENT_UPDATE_FAILED"));
        }
    }

    @GetMapping("/{studentId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'TENANT_MANAGER', 'TENANT_USER')")
    public ResponseEntity<ApiResponse<StudentResponse>> getStudent(@PathVariable Long studentId) {

        log.debug("Fetching student: {}", studentId);

        try {
            StudentResponse response = studentManagementService.getStudentById(studentId);
            return ResponseEntity.ok(ApiResponse.success("Student retrieved successfully", response));
        } catch (Exception e) {
            log.error("Failed to get student: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "STUDENT_RETRIEVAL_FAILED"));
        }
    }

    @GetMapping("/admission/{admissionNumber}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'TENANT_MANAGER', 'TENANT_USER')")
    public ResponseEntity<ApiResponse<StudentResponse>> getStudentByAdmissionNumber(@PathVariable String admissionNumber) {

        log.debug("Fetching student by admission number: {}", admissionNumber);

        try {
            StudentResponse response = studentManagementService.getStudentByAdmissionNumber(admissionNumber);
            return ResponseEntity.ok(ApiResponse.success("Student retrieved successfully", response));
        } catch (Exception e) {
            log.error("Failed to get student: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "STUDENT_RETRIEVAL_FAILED"));
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'TENANT_MANAGER', 'TENANT_USER')")
    public ResponseEntity<ApiResponse<Page<StudentResponse>>> searchStudents(@RequestParam(required = false) String firstName,
                                                                             @RequestParam(required = false) String lastName,
                                                                             @RequestParam(required = false) String admissionNumber,
                                                                             @RequestParam(required = false) Long classId,
                                                                             @RequestParam(required = false) Long sectionId,
                                                                             @RequestParam(required = false) String status,
                                                                             @RequestParam(required = false) String gender,
                                                                             @RequestParam(required = false) Boolean isActive,
                                                                             @RequestParam(defaultValue = "0") int page,
                                                                             @RequestParam(defaultValue = "20") int size,
                                                                             @RequestParam(defaultValue = "admissionNumber") String sortBy,
                                                                             @RequestParam(defaultValue = "ASC") String sortDirection) {

        try {
            StudentSearchRequest searchRequest = new StudentSearchRequest();
            searchRequest.setFirstName(firstName);
            searchRequest.setLastName(lastName);
            searchRequest.setAdmissionNumber(admissionNumber);
            searchRequest.setClassId(classId);
            searchRequest.setSectionId(sectionId);

            if (status != null) {
                searchRequest.setStatus(Student.StudentStatus.valueOf(status));
            }
            if (gender != null) {
                searchRequest.setGender(Student.Gender.valueOf(gender));
            }

            searchRequest.setIsActive(isActive != null ? isActive : true);
            searchRequest.setPage(page);
            searchRequest.setSize(size);
            searchRequest.setSortBy(sortBy);
            searchRequest.setSortDirection(sortDirection);

            Page<StudentResponse> students = studentManagementService.searchStudents(searchRequest);
            return ResponseEntity.ok(ApiResponse.success("Students retrieved successfully", students));
        } catch (Exception e) {
            log.error("Failed to search students: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "STUDENT_SEARCH_FAILED"));
        }
    }

    @DeleteMapping("/{studentId}")
    public ResponseEntity<ApiResponse<String>> deleteStudent(@PathVariable Long studentId, @AuthenticationPrincipal UserPrincipal currentUser) {

        log.info("Deleting student: {} by user: {}", studentId, currentUser.getUsername());

        try {
            studentManagementService.deleteStudent(studentId, currentUser);
            return ResponseEntity.ok(ApiResponse.success("Student deleted successfully", null));
        } catch (Exception e) {
            log.error("Failed to delete student: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "STUDENT_DELETION_FAILED"));
        }
    }

    @PostMapping("/{studentId}/guardians")
    public ResponseEntity<ApiResponse<GuardianResponse>> addGuardian(@PathVariable Long studentId, @Valid @RequestBody CreateStudentRequest.GuardianInfo guardianInfo,
                                                                     @AuthenticationPrincipal UserPrincipal currentUser) {

        log.info("Adding guardian to student: {} by user: {}", studentId, currentUser.getUsername());

        try {
            GuardianResponse response = studentManagementService.addGuardian(studentId, guardianInfo, currentUser);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Guardian added successfully", response));
        } catch (Exception e) {
            log.error("Failed to add guardian: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "GUARDIAN_ADDITION_FAILED"));
        }
    }

}
