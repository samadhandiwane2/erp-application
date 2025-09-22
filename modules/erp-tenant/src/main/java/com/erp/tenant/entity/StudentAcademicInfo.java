package com.erp.tenant.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "student_academic_info")
@Getter
@Setter
public class StudentAcademicInfo {

    @Id
    @Column(name = "student_id")
    private Long studentId;

    @Column(name = "admission_test_score")
    private Integer admissionTestScore;

    @Column(name = "admission_test_rank")
    private Integer admissionTestRank;

    @Column(name = "learning_disability", columnDefinition = "TEXT")
    private String learningDisability;

    @Column(name = "special_needs", columnDefinition = "TEXT")
    private String specialNeeds;

    @Column(name = "counseling_required")
    private Boolean counselingRequired = false;

    @Column(name = "remedial_required")
    private Boolean remedialRequired = false;

    @Column(name = "gifted_student")
    private Boolean giftedStudent = false;

    @Column(name = "sports_quota")
    private Boolean sportsQuota = false;

    @Column(name = "scholarship_holder")
    private Boolean scholarshipHolder = false;

    @Column(name = "scholarship_details", columnDefinition = "TEXT")
    private String scholarshipDetails;

    @Column(name = "extracurricular_activities", columnDefinition = "TEXT")
    private String extracurricularActivities;

    @Column(name = "achievements", columnDefinition = "TEXT")
    private String achievements;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by")
    private Long updatedBy;

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

}
