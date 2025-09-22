package com.erp.common.dto.tenant;

import com.erp.common.entity.Tenant;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UpdateTenantRequest {
    @Size(max = 100, message = "Tenant name must not exceed 100 characters")
    private String tenantName;

    @Email(message = "Contact email should be valid")
    private String contactEmail;

    @Pattern(regexp = "^[+]?[1-9][0-9]{7,14}$", message = "Contact phone should be valid")
    private String contactPhone;

    private Tenant.TenantStatus status;

    private LocalDateTime subscriptionEndDate;

}
