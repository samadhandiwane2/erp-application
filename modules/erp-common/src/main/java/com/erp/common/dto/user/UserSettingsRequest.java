package com.erp.common.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UserSettingsRequest {

    @NotBlank(message = "Timezone is required")
    private String timezone = "UTC";

    @NotBlank(message = "Language is required")
    @Pattern(regexp = "^(en|es|fr|de|it|pt|ja|zh|ko|ar)$", message = "Language must be a valid language code")
    private String language = "en";

    private Boolean emailNotifications = true;

    private Boolean marketingEmails = false;

    private Boolean securityAlerts = true;

    @Pattern(regexp = "^(light|dark)$", message = "Theme must be 'light' or 'dark'")
    private String theme = "light";

    @Pattern(regexp = "^(yyyy-MM-dd|dd/MM/yyyy|MM/dd/yyyy|dd-MM-yyyy)$", message = "Date format must be valid")
    private String dateFormat = "yyyy-MM-dd";

    @Pattern(regexp = "^(HH:mm|hh:mm a|HH:mm:ss)$", message = "Time format must be valid")
    private String timeFormat = "HH:mm";

}
