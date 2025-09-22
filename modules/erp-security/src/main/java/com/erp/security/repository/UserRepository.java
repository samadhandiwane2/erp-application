package com.erp.security.repository;

import com.erp.common.annotation.ForceMasterSchema;
import com.erp.common.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@ForceMasterSchema
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsernameAndIsActiveTrue(String username);

    Optional<User> findByEmailAndIsActiveTrue(String email);

    Optional<User> findByUsernameAndTenantIdAndIsActiveTrue(String username, Long tenantId);

    Optional<User> findByUsernameAndUserTypeAndIsActiveTrue(String username, User.UserType userType);

    @Query("SELECT u FROM User u WHERE u.tenantId = :tenantId AND u.isActive = true")
    List<User> findByTenantIdAndIsActiveTrue(@Param("tenantId") Long tenantId);

    boolean existsByUsernameAndIsActiveTrue(String username);

    boolean existsByEmailAndIsActiveTrue(String email);

    @Query("SELECT COUNT(u) FROM User u WHERE u.tenantId = :tenantId AND u.userType = :userType AND u.isActive = true")
    long countByTenantIdAndUserTypeAndIsActiveTrue(@Param("tenantId") Long tenantId, @Param("userType") User.UserType userType);

    @Query("""
            SELECT u FROM User u LEFT JOIN Tenant t ON u.tenantId = t.id 
            WHERE (:username IS NULL OR LOWER(u.username) LIKE LOWER(CONCAT('%', :username, '%')))
            AND (:email IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :email, '%')))
            AND (:firstName IS NULL OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :firstName, '%')))
            AND (:lastName IS NULL OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :lastName, '%')))
            AND (:userType IS NULL OR u.userType = :userType)
            AND (:tenantId IS NULL OR u.tenantId = :tenantId)
            AND (:isActive IS NULL OR u.isActive = :isActive)
            """)
    Page<User> findUsersWithFilters(@Param("username") String username,
                                    @Param("email") String email,
                                    @Param("firstName") String firstName,
                                    @Param("lastName") String lastName,
                                    @Param("userType") User.UserType userType,
                                    @Param("tenantId") Long tenantId,
                                    @Param("isActive") Boolean isActive,
                                    Pageable pageable
    );

    Optional<User> findByTenantIdAndUserTypeAndIsActiveTrue(Long tenantId, User.UserType userType);

    Optional<User> findFirstByTenantIdAndUserTypeAndIsActiveTrueOrderByCreatedAtDesc(
            Long tenantId,
            User.UserType userType
    );

    @Modifying
    @Query("UPDATE User u SET u.isActive = false WHERE u.tenantId = :tenantId")
    void deactivateAllUsersByTenantId(@Param("tenantId") Long tenantId);

}
