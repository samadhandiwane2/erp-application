package com.erp.tenant.dto.student;

import com.erp.tenant.entity.Student;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateStudentRequest {

    @Size(max = 50)
    private String firstName;

    @Size(max = 50)
    private String lastName;

    @Email(message = "Invalid email format")
    private String email;

    @Pattern(regexp = "^[+]?[0-9]{10,15}$", message = "Invalid phone number")
    private String phone;

    private String address;
    private String city;
    private String state;

    @Pattern(regexp = "^[0-9]{6}$", message = "Postal code must be 6 digits")
    private String postalCode;

    private Long currentClassId;
    private Long currentSectionId;

    @Pattern(regexp = "^(A|B|AB|O)[+-]?$", message = "Invalid blood group")
    private String bloodGroup;

    private String religion;
    private String category;
    private String motherTongue;

    private Student.StudentStatus studentStatus;

    private String medicalConditions;
    private String emergencyContactName;
    private String emergencyContactPhone;

}
