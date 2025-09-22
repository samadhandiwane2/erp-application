package com.erp.tenant.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "students")
@Getter
@Setter
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "admission_number", nullable = false, unique = true, length = 20)
    private String admissionNumber;

    @Column(name = "roll_number", length = 20)
    private String rollNumber;

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "middle_name", length = 50)
    private String middleName;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false)
    private Gender gender;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "alternate_phone", length = 20)
    private String alternatePhone;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "city", length = 50)
    private String city;

    @Column(name = "state", length = 50)
    private String state;

    @Column(name = "postal_code", length = 10)
    private String postalCode;

    @Column(name = "country", length = 50)
    private String country = "India";

    @Column(name = "admission_date", nullable = false)
    private LocalDate admissionDate;

    @Column(name = "admission_class_id")
    private Long admissionClassId; // Class at the time of admission

    @Column(name = "current_class_id")
    private Long currentClassId;

    @Column(name = "current_section_id")
    private Long currentSectionId;

    @Column(name = "academic_year_id")
    private Long academicYearId;

    @Column(name = "house", length = 50)
    private String house; // School house for sports/activities

    @Column(name = "blood_group", length = 5)
    private String bloodGroup;

    @Column(name = "religion", length = 50)
    private String religion;

    @Column(name = "caste", length = 50)
    private String caste;

    @Column(name = "category", length = 20)
    private String category; // General, OBC, SC, ST

    @Column(name = "nationality", length = 50)
    private String nationality = "Indian";

    @Column(name = "mother_tongue", length = 50)
    private String motherTongue;

    @Column(name = "aadhar_number", length = 12)
    private String aadharNumber;

    @Column(name = "birth_certificate_number", length = 50)
    private String birthCertificateNumber;

    @Column(name = "previous_school", columnDefinition = "TEXT")
    private String previousSchool;

    @Column(name = "previous_class", length = 50)
    private String previousClass;

    @Column(name = "transfer_certificate_number", length = 50)
    private String transferCertificateNumber;

    @Column(name = "transfer_certificate_date")
    private LocalDate transferCertificateDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "student_status")
    private StudentStatus studentStatus = StudentStatus.ACTIVE;

    @Column(name = "medical_conditions", columnDefinition = "TEXT")
    private String medicalConditions;

    @Column(name = "allergies", columnDefinition = "TEXT")
    private String allergies;

    @Column(name = "emergency_contact_name", length = 100)
    private String emergencyContactName;

    @Column(name = "emergency_contact_phone", length = 20)
    private String emergencyContactPhone;

    @Column(name = "emergency_contact_relation", length = 50)
    private String emergencyContactRelation;

    @Column(name = "transport_mode", length = 50)
    private String transportMode; // Bus, Van, Self

    @Column(name = "route_id")
    private Long routeId; // If using school transport

    @Column(name = "bus_stop", length = 100)
    private String busStop;

    @Column(name = "profile_photo_url")
    private String profilePhotoUrl;

    @Column(name = "birth_certificate_url")
    private String birthCertificateUrl;

    @Column(name = "transfer_certificate_url")
    private String transferCertificateUrl;

    @Column(name = "aadhar_card_url")
    private String aadharCardUrl;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "is_rte") // Right to Education
    private Boolean isRte = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum Gender {
        MALE, FEMALE, OTHER
    }

    public enum StudentStatus {
        ACTIVE,
        INACTIVE,
        TRANSFERRED,
        GRADUATED,
        DROPPED,
        SUSPENDED,
        ALUMNI
    }

}
