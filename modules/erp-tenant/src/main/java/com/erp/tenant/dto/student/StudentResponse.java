package com.erp.tenant.dto.student;

import com.erp.tenant.entity.Student;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class StudentResponse {
    private Long id;
    private String admissionNumber;
    private String rollNumber;
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private Student.Gender gender;
    private String email;
    private String phone;
    private String address;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private LocalDate admissionDate;
    private Long currentClassId;
    private String currentClassName;
    private Long currentSectionId;
    private String currentSectionName;
    private Long academicYearId;
    private String academicYear;
    private String bloodGroup;
    private String religion;
    private String category;
    private String nationality;
    private String motherTongue;
    private Student.StudentStatus studentStatus;
    private String profilePhotoUrl;
    private String aadharNumber;
    private String previousSchool;
    private String medicalConditions;
    private String emergencyContactName;
    private String emergencyContactPhone;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isActive;
    private List<GuardianResponse> guardians;
    private Integer age;

}
