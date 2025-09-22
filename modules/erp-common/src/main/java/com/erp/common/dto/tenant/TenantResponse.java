package com.erp.common.dto.tenant;

import com.erp.common.entity.Tenant;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TenantResponse {
    private Long id;
    private String tenantName;
    private String tenantCode;
    private String schemaName;
    private String contactEmail;
    private String contactPhone;
    private Tenant.TenantStatus status;
    private LocalDateTime subscriptionStartDate;
    private LocalDateTime subscriptionEndDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isActive;
    private TenantAdminInfo tenantAdmin;

    @Data
    public static class TenantAdminInfo {
        private Long id;
        private String username;
        private String email;
        private String firstName;
        private String lastName;
        private String phone;
        private LocalDateTime lastLogin;
    }

}
