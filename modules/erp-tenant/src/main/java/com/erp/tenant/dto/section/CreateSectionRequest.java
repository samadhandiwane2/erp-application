package com.erp.tenant.dto.section;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class CreateSectionRequest {

    @NotNull(message = "Class ID is required")
    private Long classId;

    @NotBlank(message = "Section name is required")
    private String sectionName;

    @NotBlank(message = "Section code is required")
    private String sectionCode;

    @Min(value = 1, message = "Maximum students must be at least 1")
    private Integer maxStudents = 30;

    private String roomNumber;

}
