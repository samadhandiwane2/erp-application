package com.erp.common.repository;

import com.erp.common.entity.Tenant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, Long> {

    @Query(value = "SELECT * FROM erp_master.tenants WHERE id = :id", nativeQuery = true)
    Optional<Tenant> findById(@Param("id") Long id);

    @Query(value = "SELECT * FROM erp_master.tenants WHERE tenant_code = :tenantCode AND is_active = true", nativeQuery = true)
    Optional<Tenant> findByTenantCodeAndIsActiveTrue(@Param("tenantCode") String tenantCode);

    @Query(value = "SELECT * FROM erp_master.tenants WHERE schema_name = :schemaName AND is_active = true", nativeQuery = true)
    Optional<Tenant> findBySchemaNameAndIsActiveTrue(@Param("schemaName") String schemaName);

    @Query(value = "SELECT EXISTS(SELECT 1 FROM erp_master.tenants WHERE tenant_code = :tenantCode AND is_active = true)", nativeQuery = true)
    long existsByTenantCodeAndIsActiveTrue(@Param("tenantCode") String tenantCode);

    @Query(value = "SELECT EXISTS(SELECT 1 FROM erp_master.tenants WHERE schema_name = :schemaName AND is_active = true)", nativeQuery = true)
    long existsBySchemaNameAndIsActiveTrue(@Param("schemaName") String schemaName);

    @Query(value = "SELECT * FROM erp_master.tenants WHERE is_active = true ORDER BY created_at DESC", nativeQuery = true)
    List<Tenant> findByIsActiveTrueOrderByCreatedAtDesc();

    @Query(value = """
        SELECT * FROM erp_master.tenants t 
        WHERE (:tenantName IS NULL OR LOWER(t.tenant_name) LIKE LOWER(CONCAT('%', :tenantName, '%')))
        AND (:tenantCode IS NULL OR LOWER(t.tenant_code) LIKE LOWER(CONCAT('%', :tenantCode, '%')))
        AND (:contactEmail IS NULL OR LOWER(t.contact_email) LIKE LOWER(CONCAT('%', :contactEmail, '%')))
        AND (:status IS NULL OR t.status = :status)
        AND (:isActive IS NULL OR t.is_active = :isActive)
        AND (:subscriptionStartAfter IS NULL OR t.subscription_start_date >= :subscriptionStartAfter)
        AND (:subscriptionEndBefore IS NULL OR t.subscription_end_date <= :subscriptionEndBefore)
        """,
            countQuery = """
        SELECT COUNT(*) FROM erp_master.tenants t 
        WHERE (:tenantName IS NULL OR LOWER(t.tenant_name) LIKE LOWER(CONCAT('%', :tenantName, '%')))
        AND (:tenantCode IS NULL OR LOWER(t.tenant_code) LIKE LOWER(CONCAT('%', :tenantCode, '%')))
        AND (:contactEmail IS NULL OR LOWER(t.contact_email) LIKE LOWER(CONCAT('%', :contactEmail, '%')))
        AND (:status IS NULL OR t.status = :status)
        AND (:isActive IS NULL OR t.is_active = :isActive)
        AND (:subscriptionStartAfter IS NULL OR t.subscription_start_date >= :subscriptionStartAfter)
        AND (:subscriptionEndBefore IS NULL OR t.subscription_end_date <= :subscriptionEndBefore)
        """,
            nativeQuery = true)
    Page<Tenant> findTenantsWithFilters(@Param("tenantName") String tenantName,
                                        @Param("tenantCode") String tenantCode,
                                        @Param("contactEmail") String contactEmail,
                                        @Param("status") String status,
                                        @Param("isActive") Boolean isActive,
                                        @Param("subscriptionStartAfter") LocalDateTime subscriptionStartAfter,
                                        @Param("subscriptionEndBefore") LocalDateTime subscriptionEndBefore,
                                        Pageable pageable);

}