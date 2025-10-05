package com.erp.security.repository;

import com.erp.common.entity.UserPreferences;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserPreferencesRepository extends JpaRepository<UserPreferences, Long> {

    @Query(value = "SELECT * FROM erp_master.user_preferences WHERE user_id = :userId LIMIT 1", nativeQuery = true)
    Optional<UserPreferences> findByUserId(@Param("userId") Long userId);

    @Query(value = "SELECT EXISTS(SELECT 1 FROM erp_master.user_preferences WHERE user_id = :userId)", nativeQuery = true)
    boolean existsByUserId(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO erp_master.user_preferences 
            (user_id, timezone, language, email_notifications, marketing_emails, 
             security_alerts, theme, date_format, time_format, created_at, updated_at)
            VALUES (:userId, :timezone, :language, :emailNotifications, :marketingEmails,
                    :securityAlerts, :theme, :dateFormat, :timeFormat, :createdAt, :updatedAt)
            """, nativeQuery = true)
    int insertPreferences(@Param("userId") Long userId, @Param("timezone") String timezone,
                          @Param("language") String language, @Param("emailNotifications") Boolean emailNotifications,
                          @Param("marketingEmails") Boolean marketingEmails, @Param("securityAlerts") Boolean securityAlerts,
                          @Param("theme") String theme, @Param("dateFormat") String dateFormat,
                          @Param("timeFormat") String timeFormat, @Param("createdAt") LocalDateTime createdAt,
                          @Param("updatedAt") LocalDateTime updatedAt);

    @Modifying
    @Transactional
    @Query(value = """
            UPDATE erp_master.user_preferences 
            SET timezone = :timezone,
                language = :language,
                email_notifications = :emailNotifications,
                marketing_emails = :marketingEmails,
                security_alerts = :securityAlerts,
                theme = :theme,
                date_format = :dateFormat,
                time_format = :timeFormat,
                updated_at = :updatedAt
            WHERE user_id = :userId
            """, nativeQuery = true)
    int updatePreferences(@Param("userId") Long userId, @Param("timezone") String timezone,
                          @Param("language") String language, @Param("emailNotifications") Boolean emailNotifications,
                          @Param("marketingEmails") Boolean marketingEmails, @Param("securityAlerts") Boolean securityAlerts,
                          @Param("theme") String theme, @Param("dateFormat") String dateFormat,
                          @Param("timeFormat") String timeFormat, @Param("updatedAt") LocalDateTime updatedAt);

}