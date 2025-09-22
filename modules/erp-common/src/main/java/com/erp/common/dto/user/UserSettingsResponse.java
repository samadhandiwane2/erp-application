package com.erp.common.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSettingsResponse {

    private String timezone;
    private String language;
    private Boolean emailNotifications;
    private Boolean marketingEmails;
    private Boolean securityAlerts;
    private String theme;
    private String dateFormat;
    private String timeFormat;
    private LocalDateTime updatedAt;

}
