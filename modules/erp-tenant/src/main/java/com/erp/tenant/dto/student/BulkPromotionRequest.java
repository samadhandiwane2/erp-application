package com.erp.tenant.dto.student;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class BulkPromotionRequest {

    @NotNull(message = "Current academic year is required")
    private Long currentAcademicYearId;

    @NotNull(message = "Next academic year is required")
    private Long nextAcademicYearId;

    @NotNull(message = "From class is required")
    private Long fromClassId;

    @NotNull(message = "To class is required")
    private Long toClassId;

    private BigDecimal minimumPercentage;
    private BigDecimal minimumAttendance;

    private List<Long> excludeStudentIds;

    private boolean autoAssignSections = true;

}
