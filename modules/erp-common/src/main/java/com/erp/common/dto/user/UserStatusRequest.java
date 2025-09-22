package com.erp.common.dto.user;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserStatusRequest {

    @NotNull(message = "Status is required")
    private Boolean isActive;

    private String reason; // Optional reason for status change

}
