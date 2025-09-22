package com.erp.common.repository;

import com.erp.common.annotation.ForceMasterSchema;
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
@ForceMasterSchema
public interface TenantRepository extends JpaRepository<Tenant, Long> {

    Optional<Tenant> findByTenantCodeAndIsActiveTrue(String tenantCode);

    Optional<Tenant> findBySchemaNameAndIsActiveTrue(String schemaName);

    boolean existsByTenantCodeAndIsActiveTrue(String tenantCode);

    boolean existsBySchemaNameAndIsActiveTrue(String schemaName);

    List<Tenant> findByIsActiveTrueOrderByCreatedAtDesc();

    @Query("""
            SELECT t FROM Tenant t 
            WHERE (:tenantName IS NULL OR LOWER(t.tenantName) LIKE LOWER(CONCAT('%', :tenantName, '%')))
            AND (:tenantCode IS NULL OR LOWER(t.tenantCode) LIKE LOWER(CONCAT('%', :tenantCode, '%')))
            AND (:contactEmail IS NULL OR LOWER(t.contactEmail) LIKE LOWER(CONCAT('%', :contactEmail, '%')))
            AND (:status IS NULL OR t.status = :status)
            AND (:isActive IS NULL OR t.isActive = :isActive)
            AND (:subscriptionStartAfter IS NULL OR t.subscriptionStartDate >= :subscriptionStartAfter)
            AND (:subscriptionEndBefore IS NULL OR t.subscriptionEndDate <= :subscriptionEndBefore)
            """)
    Page<Tenant> findTenantsWithFilters(@Param("tenantName") String tenantName,
                                        @Param("tenantCode") String tenantCode,
                                        @Param("contactEmail") String contactEmail,
                                        @Param("status") Tenant.TenantStatus status,
                                        @Param("isActive") Boolean isActive,
                                        @Param("subscriptionStartAfter") LocalDateTime subscriptionStartAfter,
                                        @Param("subscriptionEndBefore") LocalDateTime subscriptionEndBefore,
                                        Pageable pageable
    );

}
