package com.erp.common.dto.user;

import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class UpdateUserRequest {

    private String firstName;
    private String lastName;

    @Email(message = "Email should be valid")
    private String email;

    private String phone;

}
