package com.erp.tenant.dto.student;

import com.erp.tenant.entity.StudentPromotion;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class PromotionResponse {

    private Long promotionId;
    private Long studentId;
    private String studentName;
    private String admissionNumber;
    private Long fromClassId;
    private Long fromSectionId;
    private Long toClassId;
    private Long toSectionId;
    private StudentPromotion.PromotionType promotionType;
    private LocalDate promotionDate;
    private String status;
    private String message;

}
