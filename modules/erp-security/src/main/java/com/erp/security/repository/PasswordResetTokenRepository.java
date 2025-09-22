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

    Optional<PasswordResetToken> findByEmailAndResetCodeAndIsUsedFalseAndExpiresAtAfter(
            String email, String resetCode, LocalDateTime currentTime);

    Optional<PasswordResetToken> findByEmailAndResetCode(String email, String resetCode);

    @Query("SELECT COUNT(p) FROM PasswordResetToken p WHERE p.email = :email AND p.createdAt > :since")
    long countByEmailAndCreatedAtAfter(@Param("email") String email, @Param("since") LocalDateTime since);

    List<PasswordResetToken> findByEmailAndIsUsedFalseAndExpiresAtAfter(String email, LocalDateTime currentTime);

    @Modifying
    @Query("UPDATE PasswordResetToken p SET p.isUsed = true WHERE p.email = :email AND p.isUsed = false")
    int markAllAsUsedByEmail(@Param("email") String email);

    @Modifying
    @Query("DELETE FROM PasswordResetToken p WHERE p.expiresAt < :currentTime")
    int deleteExpiredTokens(@Param("currentTime") LocalDateTime currentTime);

    @Query("SELECT p FROM PasswordResetToken p WHERE p.expiresAt < :currentTime")
    List<PasswordResetToken> findExpiredTokens(@Param("currentTime") LocalDateTime currentTime);

}
