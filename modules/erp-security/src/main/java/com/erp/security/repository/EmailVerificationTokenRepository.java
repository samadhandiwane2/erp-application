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

    @Query(value = "SELECT * FROM erp_master.email_verification_tokens WHERE verification_token = :verificationToken AND is_verified = false AND expires_at > :currentTime LIMIT 1", nativeQuery = true)
    Optional<EmailVerificationToken> findByVerificationTokenAndIsVerifiedFalseAndExpiresAtAfter(@Param("verificationToken") String verificationToken, @Param("currentTime") LocalDateTime currentTime);

    @Query(value = "SELECT * FROM erp_master.email_verification_tokens WHERE user_id = :userId AND is_verified = false AND expires_at > :currentTime LIMIT 1", nativeQuery = true)
    Optional<EmailVerificationToken> findByUserIdAndIsVerifiedFalseAndExpiresAtAfter(@Param("userId") Long userId, @Param("currentTime") LocalDateTime currentTime);

    @Query(value = "SELECT * FROM erp_master.email_verification_tokens WHERE user_id = :userId AND is_verified = false", nativeQuery = true)
    List<EmailVerificationToken> findByUserIdAndIsVerifiedFalse(@Param("userId") Long userId);

    @Query(value = "SELECT EXISTS(SELECT 1 FROM erp_master.email_verification_tokens WHERE new_email = :newEmail AND is_verified = false AND expires_at > :currentTime)", nativeQuery = true)
    boolean existsByNewEmailAndIsVerifiedFalseAndExpiresAtAfter(@Param("newEmail") String newEmail, @Param("currentTime") LocalDateTime currentTime);

    @Modifying
    @Query(value = "UPDATE erp_master.email_verification_tokens SET is_verified = true WHERE user_id = :userId AND is_verified = false", nativeQuery = true)
    int invalidateAllPendingTokensByUser(@Param("userId") Long userId);

    @Modifying
    @Query(value = "DELETE FROM erp_master.email_verification_tokens WHERE expires_at < :currentTime", nativeQuery = true)
    int deleteExpiredTokens(@Param("currentTime") LocalDateTime currentTime);

    @Query(value = "SELECT COUNT(*) FROM erp_master.email_verification_tokens WHERE user_id = :userId AND created_at > :since", nativeQuery = true)
    long countByUserIdAndCreatedAtAfter(@Param("userId") Long userId, @Param("since") LocalDateTime since);

}