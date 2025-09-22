package com.erp.tenant.dto.student;

import com.erp.tenant.entity.StudentPromotion;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PromotionRequest {

    @NotNull(message = "Student ID is required")
    private Long studentId;

    @NotNull(message = "Current academic year is required")
    private Long currentAcademicYearId;

    @NotNull(message = "Next academic year is required")
    private Long nextAcademicYearId;

    @NotNull(message = "From class is required")
    private Long fromClassId;

    @NotNull(message = "From section is required")
    private Long fromSectionId;

    private Long toClassId;
    private Long toSectionId;

    @NotNull(message = "Promotion type is required")
    private StudentPromotion.PromotionType promotionType;

    private BigDecimal finalPercentage;
    private String finalGrade;
    private BigDecimal attendancePercentage;
    private String remarks;
    
}
