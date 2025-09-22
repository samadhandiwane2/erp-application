package com.erp.security.repository;

import com.erp.common.entity.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    Optional<EmailVerificationToken> findByVerificationTokenAndIsVerifiedFalseAndExpiresAtAfter(
            String verificationToken, LocalDateTime currentTime);

    Optional<EmailVerificationToken> findByUserIdAndIsVerifiedFalseAndExpiresAtAfter(
            Long userId, LocalDateTime currentTime);

    List<EmailVerificationToken> findByUserIdAndIsVerifiedFalse(Long userId);

    boolean existsByNewEmailAndIsVerifiedFalseAndExpiresAtAfter(String newEmail, LocalDateTime currentTime);

    @Modifying
    @Query("UPDATE EmailVerificationToken e SET e.isVerified = true WHERE e.userId = :userId AND e.isVerified = false")
    int invalidateAllPendingTokensByUser(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM EmailVerificationToken e WHERE e.expiresAt < :currentTime")
    int deleteExpiredTokens(@Param("currentTime") LocalDateTime currentTime);

    @Query("SELECT COUNT(e) FROM EmailVerificationToken e WHERE e.userId = :userId AND e.createdAt > :since")
    long countByUserIdAndCreatedAtAfter(@Param("userId") Long userId, @Param("since") LocalDateTime since);

}
