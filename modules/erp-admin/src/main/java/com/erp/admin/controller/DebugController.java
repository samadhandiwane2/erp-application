package com.erp.admin.controller;

import com.erp.common.context.SchemaContext;
import com.erp.common.context.TenantContext;
import com.erp.common.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/debug")
@Slf4j
public class DebugController {

    @GetMapping("/context")
    public ApiResponse<Map<String, Object>> getContext() {
        Map<String, Object> context = new HashMap<>();

        // Get tenant context
        context.put("tenantId", TenantContext.getCurrentTenantId());
        context.put("tenantCode", TenantContext.getCurrentTenant());
        context.put("tenantSchema", TenantContext.getCurrentSchema());

        // Get schema context
        context.put("forcedSchema", SchemaContext.getCurrentSchema());
        context.put("hasForcedSchema", SchemaContext.hasForcedSchema());

        // Get security context
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            context.put("username", auth.getName());
            context.put("authorities", auth.getAuthorities().toString());
            context.put("isAuthenticated", auth.isAuthenticated());
        }

        // Log for debugging
        log.info("Current context state: {}", context);

        return ApiResponse.success("Context information retrieved", context);
    }

    @GetMapping("/test-routing")
    public ApiResponse<String> testRouting() {
        String tenantCode = TenantContext.getCurrentTenant();
        String forcedSchema = SchemaContext.getCurrentSchema();

        String expectedDataSource;
        if (SchemaContext.hasForcedSchema()) {
            if ("master".equals(forcedSchema)) {
                expectedDataSource = "master";
            } else if ("tenant".equals(forcedSchema)) {
                expectedDataSource = tenantCode != null ? "tenant_" + tenantCode.toLowerCase() : "master";
            } else {
                expectedDataSource = forcedSchema;
            }
        } else if (tenantCode != null) {
            expectedDataSource = "tenant_" + tenantCode.toLowerCase();
        } else {
            expectedDataSource = "master";
        }

        String message = String.format(
                "TenantCode: %s, ForcedSchema: %s, Expected DataSource: %s",
                tenantCode, forcedSchema, expectedDataSource
        );

        log.info(message);
        return ApiResponse.success(message, "Routing test completed");
    }

}
