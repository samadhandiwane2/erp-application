package com.erp.tenant.dto.student;

import com.erp.tenant.entity.Guardian;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class GuardianResponse {
    private Long id;
    private Guardian.GuardianType guardianType;
    private String firstName;
    private String lastName;
    private String relationship;
    private String email;
    private String phone;
    private String occupation;
    private BigDecimal annualIncome;
    private String address;
    private String officeAddress;
    private String officePhone;
    private Boolean isPrimaryContact;
    private Boolean canPickupChild;
    private String photoUrl;

}
