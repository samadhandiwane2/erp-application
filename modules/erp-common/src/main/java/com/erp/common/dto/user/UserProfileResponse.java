package com.erp.common.dto.user;

import com.erp.common.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {

    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private User.UserType userType;
    private Long tenantId;
    private String tenantCode;
    private String tenantName;
    private LocalDateTime lastLogin;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isActive;

    // Profile completeness indicator
    private Double profileCompleteness;

    // Account status info
    private Boolean isAccountLocked;
    private Integer failedLoginAttempts;

}
