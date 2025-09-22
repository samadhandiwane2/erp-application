package com.erp.common.dto.tenant;

import com.erp.common.entity.Tenant;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TenantSearchRequest {

    private String tenantName;
    private String tenantCode;
    private String contactEmail;
    private Tenant.TenantStatus status;
    private Boolean isActive;
    private LocalDateTime subscriptionStartAfter;
    private LocalDateTime subscriptionEndBefore;

    private int page = 0;
    private int size = 20;
    private String sortBy = "createdAt";
    private String sortDirection = "DESC";

}
