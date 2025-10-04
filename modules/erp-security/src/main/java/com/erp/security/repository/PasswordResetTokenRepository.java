package com.erp.security.repository;

import com.erp.common.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    @Query(value = "SELECT * FROM erp_master.password_reset_tokens WHERE email = :email AND reset_code = :resetCode AND is_used = false AND expires_at > :currentTime LIMIT 1", nativeQuery = true)
    Optional<PasswordResetToken> findByEmailAndResetCodeAndIsUsedFalseAndExpiresAtAfter(@Param("email") String email, @Param("resetCode") String resetCode, @Param("currentTime") LocalDateTime currentTime);

    @Query(value = "SELECT * FROM erp_master.password_reset_tokens WHERE email = :email AND reset_code = :resetCode LIMIT 1", nativeQuery = true)
    Optional<PasswordResetToken> findByEmailAndResetCode(@Param("email") String email, @Param("resetCode") String resetCode);

    @Query(value = "SELECT COUNT(*) FROM erp_master.password_reset_tokens WHERE email = :email AND created_at > :since", nativeQuery = true)
    long countByEmailAndCreatedAtAfter(@Param("email") String email, @Param("since") LocalDateTime since);

    @Query(value = "SELECT * FROM erp_master.password_reset_tokens WHERE email = :email AND is_used = false AND expires_at > :currentTime", nativeQuery = true)
    List<PasswordResetToken> findByEmailAndIsUsedFalseAndExpiresAtAfter(@Param("email") String email, @Param("currentTime") LocalDateTime currentTime);

    @Modifying
    @Query(value = "UPDATE erp_master.password_reset_tokens SET is_used = true WHERE email = :email AND is_used = false", nativeQuery = true)
    int markAllAsUsedByEmail(@Param("email") String email);

    @Modifying
    @Query(value = "DELETE FROM erp_master.password_reset_tokens WHERE expires_at < :currentTime", nativeQuery = true)
    int deleteExpiredTokens(@Param("currentTime") LocalDateTime currentTime);

    @Query(value = "SELECT * FROM erp_master.password_reset_tokens WHERE expires_at < :currentTime", nativeQuery = true)
    List<PasswordResetToken> findExpiredTokens(@Param("currentTime") LocalDateTime currentTime);

}