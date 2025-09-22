package com.erp.common.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "tenants")
@Getter
@Setter
public class Tenant extends BaseEntity {

    @Column(name = "tenant_name", nullable = false, unique = true)
    private String tenantName;

    @Column(name = "tenant_code", nullable = false, unique = true)
    private String tenantCode;

    @Column(name = "schema_name", nullable = false, unique = true)
    private String schemaName;

    @Column(name = "database_url")
    private String databaseUrl;

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "contact_phone")
    private String contactPhone;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private TenantStatus status = TenantStatus.ACTIVE;

    @Column(name = "subscription_start_date")
    private LocalDateTime subscriptionStartDate;

    @Column(name = "subscription_end_date")
    private LocalDateTime subscriptionEndDate;

    public enum TenantStatus {
        ACTIVE, INACTIVE, SUSPENDED
    }
}
