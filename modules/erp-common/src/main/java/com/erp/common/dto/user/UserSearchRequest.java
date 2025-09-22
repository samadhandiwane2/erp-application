package com.erp.common.dto.user;

import com.erp.common.entity.User;
import lombok.Data;

@Data
public class UserSearchRequest {

    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private User.UserType userType;
    private Long tenantId;
    private String tenantCode;
    private Boolean isActive;
    private int page = 0;
    private int size = 20;
    private String sortBy = "createdAt";
    private String sortDirection = "DESC";

}
