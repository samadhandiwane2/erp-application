package com.erp.tenant.dto.student;

import com.erp.tenant.entity.Guardian;
import com.erp.tenant.entity.Student;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class CreateStudentRequest {

    @NotBlank(message = "First name is required")
    @Size(max = 50)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 50)
    private String lastName;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @NotNull(message = "Gender is required")
    private Student.Gender gender;

    @Email(message = "Invalid email format")
    private String email;

    @Pattern(regexp = "^[+]?[0-9]{10,15}$", message = "Invalid phone number")
    private String phone;

    private String address;
    private String city;
    private String state;

    @Pattern(regexp = "^[0-9]{6}$", message = "Postal code must be 6 digits")
    private String postalCode;

    private String country = "India";

    @NotNull(message = "Admission date is required")
    private LocalDate admissionDate;

    @NotNull(message = "Class is required")
    private Long currentClassId;

    @NotNull(message = "Section is required")
    private Long currentSectionId;

    private Long academicYearId;

    @Pattern(regexp = "^(A|B|AB|O)[+-]?$", message = "Invalid blood group")
    private String bloodGroup;

    private String religion;
    private String category;
    private String nationality = "Indian";
    private String motherTongue;

    @Pattern(regexp = "^[0-9]{12}$", message = "Aadhar must be 12 digits")
    private String aadharNumber;

    private String previousSchool;
    private String medicalConditions;
    private String emergencyContactName;
    private String emergencyContactPhone;

    private List<GuardianInfo> guardians;

    @Data
    public static class GuardianInfo {
        @NotNull
        private Guardian.GuardianType guardianType;

        @NotBlank
        private String firstName;

        @NotBlank
        private String lastName;

        private String relationship;
        private String email;

        @NotBlank
        private String phone;

        private String occupation;
        private String address;
        private Boolean isPrimaryContact = false;
    }

}
