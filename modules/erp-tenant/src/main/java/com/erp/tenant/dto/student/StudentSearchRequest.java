package com.erp.tenant.dto.student;

import com.erp.tenant.entity.Student;
import lombok.Data;

import java.time.LocalDate;

@Data
public class StudentSearchRequest {

    private String firstName;
    private String lastName;
    private String admissionNumber;
    private Long classId;
    private Long sectionId;
    private Student.StudentStatus status;
    private Student.Gender gender;
    private Boolean isActive = true;
    private LocalDate admissionFromDate;
    private LocalDate admissionToDate;

    private int page = 0;
    private int size = 20;
    private String sortBy = "admissionNumber";
    private String sortDirection = "ASC";

}
