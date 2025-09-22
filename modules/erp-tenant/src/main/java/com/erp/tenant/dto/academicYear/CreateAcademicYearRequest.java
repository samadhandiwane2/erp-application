package com.erp.tenant.dto.academicYear;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateAcademicYearRequest {

    @NotBlank(message = "Year name is required")
    private String yearName;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    private Boolean isCurrent = false;

    private String description;

}
