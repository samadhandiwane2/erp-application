package com.erp.tenant.dto.academicYear;

import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateAcademicYearRequest {

    private String yearName;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean isCurrent;
    private String description;
    private Boolean isActive;

}
