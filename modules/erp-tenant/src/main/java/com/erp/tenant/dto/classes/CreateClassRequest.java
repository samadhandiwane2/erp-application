package com.erp.tenant.dto.classes;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class CreateClassRequest {

    @NotBlank(message = "Class name is required")
    private String className;

    @NotBlank(message = "Class code is required")
    private String classCode;

    @NotNull(message = "Grade level is required")
    @Min(value = 1, message = "Grade level must be at least 1")
    private Integer gradeLevel;

    private String description;

    @Min(value = 1, message = "Maximum students must be at least 1")
    private Integer maxStudents = 50;

}
