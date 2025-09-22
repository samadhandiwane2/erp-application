package com.erp.common.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EmailChangeRequest {

    @NotBlank(message = "New email is required")
    @Email(message = "New email must be valid")
    private String newEmail;

    @NotBlank(message = "Current password is required")
    private String currentPassword;

}
