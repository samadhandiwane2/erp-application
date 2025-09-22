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
public class EmailChangeStatusResponse {

    private String currentEmail;
    private String pendingEmail;
    private Boolean hasPendingChange;
    private LocalDateTime changeRequestedAt;
    private LocalDateTime expiresAt;

}
