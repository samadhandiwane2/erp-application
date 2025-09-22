package com.erp.common.dto.tenant;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateTenantRequest {

    @NotBlank(message = "Tenant name is required")
    @Size(max = 100, message = "Tenant name must not exceed 100 characters")
    private String tenantName;

    @NotBlank(message = "Tenant code is required")
    @Pattern(regexp = "^[A-Z0-9_]{2,20}$", message = "Tenant code must be 2-20 characters, uppercase letters, numbers, and underscores only")
    private String tenantCode;

    @Email(message = "Contact email should be valid")
    private String contactEmail;

    @Pattern(regexp = "^[+]?[1-9][0-9]{7,14}$", message = "Contact phone should be valid")
    private String contactPhone;

    private Integer subscriptionMonths = 12; // Default 1 year

    // Tenant Admin Details
    @NotBlank(message = "Admin username is required")
    @Size(min = 3, max = 50, message = "Admin username must be 3-50 characters")
    private String adminUsername;

    @NotBlank(message = "Admin email is required")
    @Email(message = "Admin email should be valid")
    private String adminEmail;

    @NotBlank(message = "Admin first name is required")
    @Size(max = 50, message = "Admin first name must not exceed 50 characters")
    private String adminFirstName;

    @NotBlank(message = "Admin last name is required")
    @Size(max = 50, message = "Admin last name must not exceed 50 characters")
    private String adminLastName;

    @Pattern(regexp = "^[+]?[1-9][0-9]{7,14}$", message = "Admin phone should be valid")
    private String adminPhone;

}
