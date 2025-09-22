package com.erp.tenant.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "student_class_history")
@Getter
@Setter
public class StudentClassHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "academic_year_id", nullable = false)
    private Long academicYearId;

    @Column(name = "class_id", nullable = false)
    private Long classId;

    @Column(name = "section_id", nullable = false)
    private Long sectionId;

    @Column(name = "roll_number", length = 20)
    private String rollNumber;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "promotion_status")
    private PromotionStatus promotionStatus;

    @Column(name = "final_percentage", precision = 5, scale = 2)
    private BigDecimal finalPercentage;

    @Column(name = "final_grade", length = 5)
    private String finalGrade;

    @Column(name = "attendance_percentage", precision = 5, scale = 2)
    private BigDecimal attendancePercentage;

    @Column(name = "total_working_days")
    private Integer totalWorkingDays;

    @Column(name = "days_present")
    private Integer daysPresent;

    @Column(name = "rank_in_class")
    private Integer rankInClass;

    @Column(name = "teacher_remarks", columnDefinition = "TEXT")
    private String teacherRemarks;

    @Column(name = "promoted_to_class_id")
    private Long promotedToClassId;

    @Column(name = "promotion_date")
    private LocalDate promotionDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private Long createdBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum PromotionStatus {
        PROMOTED,
        DETAINED,
        REPEAT,
        CONDITIONAL_PROMOTION,
        LEFT_SCHOOL,
        IN_PROGRESS
    }

}
