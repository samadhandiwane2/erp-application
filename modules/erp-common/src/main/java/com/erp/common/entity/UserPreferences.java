package com.erp.common.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_preferences")
@Getter
@Setter
public class UserPreferences {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "timezone")
    private String timezone = "UTC";

    @Column(name = "language")
    private String language = "en";

    @Column(name = "email_notifications")
    private Boolean emailNotifications = true;

    @Column(name = "marketing_emails")
    private Boolean marketingEmails = false;

    @Column(name = "security_alerts")
    private Boolean securityAlerts = true;

    @Column(name = "theme")
    private String theme = "light";

    @Column(name = "date_format")
    private String dateFormat = "yyyy-MM-dd";

    @Column(name = "time_format")
    private String timeFormat = "HH:mm";

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

}
