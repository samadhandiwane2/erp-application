package com.erp.tenant.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "student_promotions")
@Getter
@Setter
public class StudentPromotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "academic_year_id", nullable = false)
    private Long academicYearId; // Current academic year

    @Column(name = "next_academic_year_id", nullable = false)
    private Long nextAcademicYearId; // Next academic year

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "from_class_id", nullable = false)
    private Long fromClassId;

    @Column(name = "from_section_id", nullable = false)
    private Long fromSectionId;

    @Column(name = "to_class_id")
    private Long toClassId;

    @Column(name = "to_section_id")
    private Long toSectionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "promotion_type", nullable = false)
    private PromotionType promotionType;

    @Column(name = "promotion_date", nullable = false)
    private LocalDate promotionDate;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "approved_date")
    private LocalDate approvedDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private Long createdBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum PromotionType {
        REGULAR_PROMOTION,    // Normal year-end promotion
        MID_YEAR_PROMOTION,   // Exceptional promotion mid-year
        DETENTION,           // Student repeats the class
        DOUBLE_PROMOTION,    // Skip a class
        SECTION_CHANGE,      // Change section within same class
        CONDITIONAL          // Promoted with conditions
    }
}
