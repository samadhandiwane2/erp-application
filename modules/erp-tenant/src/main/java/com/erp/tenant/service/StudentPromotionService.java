package com.erp.tenant.service;

import com.erp.common.annotation.ForceTenantSchema;
import com.erp.common.jwt.UserPrincipal;
import com.erp.tenant.dto.student.PromotionRequest;
import com.erp.tenant.dto.student.BulkPromotionRequest;
import com.erp.tenant.dto.student.PromotionResponse;
import com.erp.tenant.entity.*;
import com.erp.tenant.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@ForceTenantSchema
public class StudentPromotionService {

    private final StudentRepository studentRepository;
    private final StudentClassHistoryRepository classHistoryRepository;
    private final StudentPromotionRepository promotionRepository;

    @Transactional
    public PromotionResponse promoteStudent(PromotionRequest request, UserPrincipal currentUser) {
        log.info("Promoting student: {} from class: {} to class: {}",
                request.getStudentId(), request.getFromClassId(), request.getToClassId());

        // Get student
        Student student = studentRepository.findByIdAndIsActiveTrue(request.getStudentId())
                .orElseThrow(() -> new RuntimeException("Student not found"));

        // Close current class history
        StudentClassHistory currentHistory = classHistoryRepository
                .findByStudentIdAndAcademicYearIdAndEndDateIsNull(
                        request.getStudentId(), request.getCurrentAcademicYearId())
                .orElseThrow(() -> new RuntimeException("Current class history not found"));

        currentHistory.setEndDate(LocalDate.now());
        currentHistory.setPromotionStatus(mapPromotionType(request.getPromotionType()));
        currentHistory.setFinalPercentage(request.getFinalPercentage());
        currentHistory.setFinalGrade(request.getFinalGrade());
        currentHistory.setAttendancePercentage(request.getAttendancePercentage());
        currentHistory.setTeacherRemarks(request.getRemarks());
        currentHistory.setPromotedToClassId(request.getToClassId());
        currentHistory.setPromotionDate(LocalDate.now());

        classHistoryRepository.save(currentHistory);

        // Create promotion record
        StudentPromotion promotion = new StudentPromotion();
        promotion.setAcademicYearId(request.getCurrentAcademicYearId());
        promotion.setNextAcademicYearId(request.getNextAcademicYearId());
        promotion.setStudentId(request.getStudentId());
        promotion.setFromClassId(request.getFromClassId());
        promotion.setFromSectionId(request.getFromSectionId());
        promotion.setToClassId(request.getToClassId());
        promotion.setToSectionId(request.getToSectionId());
        promotion.setPromotionType(request.getPromotionType());
        promotion.setPromotionDate(LocalDate.now());
        promotion.setReason(request.getRemarks());
        promotion.setApprovedBy(currentUser.getId());
        promotion.setApprovedDate(LocalDate.now());
        promotion.setCreatedBy(currentUser.getId());

        StudentPromotion savedPromotion = promotionRepository.save(promotion);

        // Update student's current class if not detention
        if (request.getPromotionType() != StudentPromotion.PromotionType.DETENTION) {
            student.setCurrentClassId(request.getToClassId());
            student.setCurrentSectionId(request.getToSectionId());
            student.setAcademicYearId(request.getNextAcademicYearId());

            // Generate new roll number for new class
            String newRollNumber = generateRollNumber(request.getToClassId(), request.getToSectionId());
            student.setRollNumber(newRollNumber);

            studentRepository.save(student);

            // Create new class history for next academic year
            StudentClassHistory newHistory = new StudentClassHistory();
            newHistory.setStudentId(request.getStudentId());
            newHistory.setAcademicYearId(request.getNextAcademicYearId());
            newHistory.setClassId(request.getToClassId());
            newHistory.setSectionId(request.getToSectionId());
            newHistory.setRollNumber(newRollNumber);
            newHistory.setStartDate(LocalDate.now());
            newHistory.setPromotionStatus(StudentClassHistory.PromotionStatus.IN_PROGRESS);
            newHistory.setCreatedBy(currentUser.getId());

            classHistoryRepository.save(newHistory);
        }

        return mapToPromotionResponse(savedPromotion, student);
    }

    @Transactional
    public List<PromotionResponse> bulkPromoteStudents(BulkPromotionRequest request, UserPrincipal currentUser) {
        log.info("Bulk promoting students from class: {} to class: {}",
                request.getFromClassId(), request.getToClassId());

        List<PromotionResponse> responses = new ArrayList<>();

        // Get all eligible students
        List<Student> students = studentRepository.findByCurrentClassIdAndIsActiveTrue(request.getFromClassId());

        for (Student student : students) {
            // Check if student meets promotion criteria
            if (shouldPromoteStudent(student, request)) {
                PromotionRequest promotionRequest = new PromotionRequest();
                promotionRequest.setStudentId(student.getId());
                promotionRequest.setCurrentAcademicYearId(request.getCurrentAcademicYearId());
                promotionRequest.setNextAcademicYearId(request.getNextAcademicYearId());
                promotionRequest.setFromClassId(request.getFromClassId());
                promotionRequest.setFromSectionId(student.getCurrentSectionId());
                promotionRequest.setToClassId(request.getToClassId());
                promotionRequest.setToSectionId(determineSectionForStudent(student, request.getToClassId()));
                promotionRequest.setPromotionType(StudentPromotion.PromotionType.REGULAR_PROMOTION);

                try {
                    PromotionResponse response = promoteStudent(promotionRequest, currentUser);
                    responses.add(response);
                } catch (Exception e) {
                    log.error("Failed to promote student: {}", student.getId(), e);
                }
            }
        }

        return responses;
    }

    @Transactional(readOnly = true)
    public List<StudentClassHistory> getStudentHistory(Long studentId) {
        return classHistoryRepository.findByStudentIdOrderByStartDateDesc(studentId);
    }

    @Transactional(readOnly = true)
    public List<StudentPromotion> getStudentPromotions(Long studentId) {
        return promotionRepository.findByStudentIdOrderByPromotionDateDesc(studentId);
    }

    @Transactional
    public void recordClassHistory(Long studentId, Long academicYearId, Long classId, Long sectionId, UserPrincipal currentUser) {
        // Check if history already exists
        if (classHistoryRepository.existsByStudentIdAndAcademicYearId(studentId, academicYearId)) {
            log.warn("Class history already exists for student: {} in academic year: {}", studentId, academicYearId);
            return;
        }

        Student student = studentRepository.findByIdAndIsActiveTrue(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        StudentClassHistory history = new StudentClassHistory();
        history.setStudentId(studentId);
        history.setAcademicYearId(academicYearId);
        history.setClassId(classId);
        history.setSectionId(sectionId);
        history.setRollNumber(student.getRollNumber());
        history.setStartDate(LocalDate.now());
        history.setPromotionStatus(StudentClassHistory.PromotionStatus.IN_PROGRESS);
        history.setCreatedBy(currentUser.getId());

        classHistoryRepository.save(history);
    }

    private boolean shouldPromoteStudent(Student student, BulkPromotionRequest request) {
        // Get student's current academic performance
        StudentClassHistory history = classHistoryRepository
                .findByStudentIdAndAcademicYearIdAndEndDateIsNull(
                        student.getId(), request.getCurrentAcademicYearId())
                .orElse(null);

        if (history == null) {
            return false;
        }

        // Check minimum attendance
        if (request.getMinimumAttendance() != null &&
                history.getAttendancePercentage() != null &&
                history.getAttendancePercentage().compareTo(request.getMinimumAttendance()) < 0) {
            return false;
        }

        // Check minimum percentage
        if (request.getMinimumPercentage() != null &&
                history.getFinalPercentage() != null &&
                history.getFinalPercentage().compareTo(request.getMinimumPercentage()) < 0) {
            return false;
        }

        // Check if student is in excluded list
        if (request.getExcludeStudentIds() != null &&
                request.getExcludeStudentIds().contains(student.getId())) {
            return false;
        }

        return true;
    }

    private Long determineSectionForStudent(Student student, Long toClassId) {
        // Logic to determine which section student goes to
        // For now, keep the same section ID if available, otherwise assign section A (ID: 1)
        return student.getCurrentSectionId() != null ? student.getCurrentSectionId() : 1L;
    }

    private String generateRollNumber(Long classId, Long sectionId) {
        long count = studentRepository.countActiveStudentsByClassId(classId) + 1;
        return String.format("%d-%d-%03d", classId, sectionId, count);
    }

    private StudentClassHistory.PromotionStatus mapPromotionType(StudentPromotion.PromotionType type) {
        switch (type) {
            case REGULAR_PROMOTION:
            case MID_YEAR_PROMOTION:
            case DOUBLE_PROMOTION:
                return StudentClassHistory.PromotionStatus.PROMOTED;
            case DETENTION:
                return StudentClassHistory.PromotionStatus.DETAINED;
            case CONDITIONAL:
                return StudentClassHistory.PromotionStatus.CONDITIONAL_PROMOTION;
            default:
                return StudentClassHistory.PromotionStatus.IN_PROGRESS;
        }
    }

    private PromotionResponse mapToPromotionResponse(StudentPromotion promotion, Student student) {
        return PromotionResponse.builder()
                .promotionId(promotion.getId())
                .studentId(promotion.getStudentId())
                .studentName(student.getFirstName() + " " + student.getLastName())
                .admissionNumber(student.getAdmissionNumber())
                .fromClassId(promotion.getFromClassId())
                .fromSectionId(promotion.getFromSectionId())
                .toClassId(promotion.getToClassId())
                .toSectionId(promotion.getToSectionId())
                .promotionType(promotion.getPromotionType())
                .promotionDate(promotion.getPromotionDate())
                .status("SUCCESS")
                .build();
    }

}
