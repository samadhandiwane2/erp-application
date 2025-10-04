package com.erp.security.repository;

import com.erp.common.entity.User;
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
public interface UserRepository extends JpaRepository<User, Long> {

    @Query(value = "SELECT * FROM erp_master.users WHERE id = :id", nativeQuery = true)
    Optional<User> findById(@Param("id") Long id);

    @Query(value = "SELECT * FROM erp_master.users WHERE username = :username AND is_active = true", nativeQuery = true)
    Optional<User> findByUsernameAndIsActiveTrue(@Param("username") String username);

    @Query(value = "SELECT * FROM erp_master.users WHERE email = :email AND is_active = true", nativeQuery = true)
    Optional<User> findByEmailAndIsActiveTrue(@Param("email") String email);

    @Query(value = "SELECT * FROM erp_master.users WHERE username = :username AND tenant_id = :tenantId AND is_active = true", nativeQuery = true)
    Optional<User> findByUsernameAndTenantIdAndIsActiveTrue(@Param("username") String username, @Param("tenantId") Long tenantId);

    @Query(value = "SELECT * FROM erp_master.users WHERE username = :username AND user_type = :userType AND is_active = true", nativeQuery = true)
    Optional<User> findByUsernameAndUserTypeAndIsActiveTrue(@Param("username") String username, @Param("userType") String userType);

    @Query(value = "SELECT * FROM erp_master.users WHERE tenant_id = :tenantId AND is_active = true", nativeQuery = true)
    List<User> findByTenantIdAndIsActiveTrue(@Param("tenantId") Long tenantId);

    @Query(value = "SELECT EXISTS(SELECT 1 FROM erp_master.users WHERE username = :username AND is_active = true)", nativeQuery = true)
    long existsByUsernameAndIsActiveTrue(@Param("username") String username);

    @Query(value = "SELECT EXISTS(SELECT 1 FROM erp_master.users WHERE email = :email AND is_active = true)", nativeQuery = true)
    long existsByEmailAndIsActiveTrue(@Param("email") String email);

    @Query(value = "SELECT COUNT(*) FROM erp_master.users WHERE tenant_id = :tenantId AND user_type = :userType AND is_active = true", nativeQuery = true)
    long countByTenantIdAndUserTypeAndIsActiveTrue(@Param("tenantId") Long tenantId, @Param("userType") String userType);

    @Query(value = """
            SELECT u.* FROM erp_master.users u 
            LEFT JOIN erp_master.tenants t ON u.tenant_id = t.id 
            WHERE (:username IS NULL OR LOWER(u.username) LIKE LOWER(CONCAT('%', :username, '%')))
            AND (:email IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :email, '%')))
            AND (:firstName IS NULL OR LOWER(u.first_name) LIKE LOWER(CONCAT('%', :firstName, '%')))
            AND (:lastName IS NULL OR LOWER(u.last_name) LIKE LOWER(CONCAT('%', :lastName, '%')))
            AND (:userType IS NULL OR u.user_type = :userType)
            AND (:tenantId IS NULL OR u.tenant_id = :tenantId)
            AND (:isActive IS NULL OR u.is_active = :isActive)
            """,
            countQuery = """
                    SELECT COUNT(*) FROM erp_master.users u 
                    WHERE (:username IS NULL OR LOWER(u.username) LIKE LOWER(CONCAT('%', :username, '%')))
                    AND (:email IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :email, '%')))
                    AND (:firstName IS NULL OR LOWER(u.first_name) LIKE LOWER(CONCAT('%', :firstName, '%')))
                    AND (:lastName IS NULL OR LOWER(u.last_name) LIKE LOWER(CONCAT('%', :lastName, '%')))
                    AND (:userType IS NULL OR u.user_type = :userType)
                    AND (:tenantId IS NULL OR u.tenant_id = :tenantId)
                    AND (:isActive IS NULL OR u.is_active = :isActive)
                    """,
            nativeQuery = true)
    Page<User> findUsersWithFilters(@Param("username") String username,
                                    @Param("email") String email,
                                    @Param("firstName") String firstName,
                                    @Param("lastName") String lastName,
                                    @Param("userType") String userType,
                                    @Param("tenantId") Long tenantId,
                                    @Param("isActive") Boolean isActive,
                                    Pageable pageable);

    @Query(value = "SELECT * FROM erp_master.users WHERE tenant_id = :tenantId AND user_type = :userType AND is_active = true LIMIT 1", nativeQuery = true)
    Optional<User> findByTenantIdAndUserTypeAndIsActiveTrue(@Param("tenantId") Long tenantId, @Param("userType") String userType);

    @Query(value = "SELECT * FROM erp_master.users WHERE tenant_id = :tenantId AND user_type = :userType AND is_active = true ORDER BY created_at DESC LIMIT 1", nativeQuery = true)
    Optional<User> findFirstByTenantIdAndUserTypeAndIsActiveTrueOrderByCreatedAtDesc(@Param("tenantId") Long tenantId, @Param("userType") String userType);

    @Modifying
    @Query(value = "UPDATE erp_master.users SET is_active = false WHERE tenant_id = :tenantId", nativeQuery = true)
    void deactivateAllUsersByTenantId(@Param("tenantId") Long tenantId);

    @Modifying
    @Transactional
    @Query(value = """
            UPDATE erp_master.users 
            SET last_login = :lastLogin, 
                failed_login_attempts = :failedAttempts, 
                account_locked_until = :lockedUntil,
                updated_at = :updatedAt,
                updated_by = :updatedBy
            WHERE id = :userId
            """, nativeQuery = true)
    int updateLoginInfo(@Param("userId") Long userId, @Param("lastLogin") LocalDateTime lastLogin,
                        @Param("failedAttempts") Integer failedAttempts, @Param("lockedUntil") LocalDateTime lockedUntil,
                        @Param("updatedAt") LocalDateTime updatedAt, @Param("updatedBy") Long updatedBy);

    @Modifying
    @Transactional
    @Query(value = """
            UPDATE erp_master.users 
            SET failed_login_attempts = :failedAttempts, 
                account_locked_until = :lockedUntil,
                updated_at = :updatedAt,
                updated_by = :updatedBy
            WHERE id = :userId
            """, nativeQuery = true)
    int updateFailedLoginAttempts(@Param("userId") Long userId, @Param("failedAttempts") Integer failedAttempts,
                                  @Param("lockedUntil") LocalDateTime lockedUntil, @Param("updatedAt") LocalDateTime updatedAt,
                                  @Param("updatedBy") Long updatedBy);

}