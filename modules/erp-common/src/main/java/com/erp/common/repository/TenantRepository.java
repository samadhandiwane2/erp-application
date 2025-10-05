package com.erp.common.repository;

import com.erp.common.entity.Tenant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    @Query(value = """
            INSERT INTO erp_master.tenants 
            (tenant_name, tenant_code, schema_name, database_url, contact_email, contact_phone, 
             status, subscription_start_date, subscription_end_date, 
             is_active, created_at, created_by, updated_at, updated_by)
            VALUES (:tenantName, :tenantCode, :schemaName, :databaseUrl, :contactEmail, :contactPhone,
                    :status, :subscriptionStartDate, :subscriptionEndDate,
                    :isActive, :createdAt, :createdBy, :updatedAt, :updatedBy)
            """, nativeQuery = true)
    @Modifying(clearAutomatically = true)
    int insertTenant(@Param("tenantName") String tenantName, @Param("tenantCode") String tenantCode,
                     @Param("schemaName") String schemaName, @Param("databaseUrl") String databaseUrl,
                     @Param("contactEmail") String contactEmail, @Param("contactPhone") String contactPhone,
                     @Param("status") String status, @Param("subscriptionStartDate") LocalDateTime subscriptionStartDate,
                     @Param("subscriptionEndDate") LocalDateTime subscriptionEndDate, @Param("isActive") Boolean isActive,
                     @Param("createdAt") LocalDateTime createdAt, @Param("createdBy") Long createdBy,
                     @Param("updatedAt") LocalDateTime updatedAt, @Param("updatedBy") Long updatedBy);

    @Query(value = "SELECT LAST_INSERT_ID()", nativeQuery = true)
    Long getLastInsertId();

    @Modifying
    @Transactional
    @Query(value = """
            UPDATE erp_master.tenants 
            SET tenant_name = :tenantName,
                contact_email = :contactEmail,
                contact_phone = :contactPhone,
                status = :status,
                subscription_end_date = :subscriptionEndDate,
                updated_at = :updatedAt,
                updated_by = :updatedBy
            WHERE id = :id
            """, nativeQuery = true)
    int updateTenant(@Param("id") Long id, @Param("tenantName") String tenantName,
                     @Param("contactEmail") String contactEmail, @Param("contactPhone") String contactPhone,
                     @Param("status") String status, @Param("subscriptionEndDate") LocalDateTime subscriptionEndDate,
                     @Param("updatedAt") LocalDateTime updatedAt, @Param("updatedBy") Long updatedBy);

    @Modifying
    @Transactional
    @Query(value = """
            UPDATE erp_master.tenants 
            SET status = :status,
                updated_at = :updatedAt,
                updated_by = :updatedBy
            WHERE id = :id
            """, nativeQuery = true)
    int updateTenantStatus(@Param("id") Long id, @Param("status") String status,
                           @Param("updatedAt") LocalDateTime updatedAt, @Param("updatedBy") Long updatedBy);

    @Modifying
    @Transactional
    @Query(value = """
            UPDATE erp_master.tenants 
            SET is_active = :isActive,
                status = :status,
                updated_at = :updatedAt,
                updated_by = :updatedBy
            WHERE id = :id
            """, nativeQuery = true)
    int softDeleteTenant(@Param("id") Long id, @Param("isActive") Boolean isActive,
                         @Param("status") String status, @Param("updatedAt") LocalDateTime updatedAt,
                         @Param("updatedBy") Long updatedBy);

}