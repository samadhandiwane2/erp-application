package com.erp.admin.controller;

import com.erp.common.context.SchemaContext;
import com.erp.common.context.TenantContext;
import com.erp.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/debug")
@RequiredArgsConstructor
@Slf4j
public class SystemDebugController {

    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/context")
    public ApiResponse<Map<String, Object>> getFullContext() {
        Map<String, Object> context = new HashMap<>();

        // Tenant context
        context.put("tenantId", TenantContext.getCurrentTenantId());
        context.put("tenantCode", TenantContext.getCurrentTenant());
        context.put("tenantSchema", TenantContext.getCurrentSchema());
        context.put("tenantKey", TenantContext.getCurrentTenantKey());

        // Schema context
        context.put("forcedSchema", SchemaContext.getCurrentSchema());
        context.put("hasForcedSchema", SchemaContext.hasForcedSchema());

        // Security context
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            context.put("username", auth.getName());
            context.put("authorities", auth.getAuthorities().toString());
            context.put("isAuthenticated", auth.isAuthenticated());
        }

        // Current database
        try {
            String database = jdbcTemplate.queryForObject("SELECT DATABASE()", String.class);
            context.put("actualDatabase", database);
        } catch (Exception e) {
            context.put("actualDatabaseError", e.getMessage());
        }

        log.info("Full context state: {}", context);
        return ApiResponse.success("Context information retrieved", context);
    }

    @GetMapping("/routing-decision")
    public ApiResponse<Map<String, Object>> explainRoutingDecision() {
        Map<String, Object> routing = new HashMap<>();

        String tenantCode = TenantContext.getCurrentTenant();
        String forcedSchema = SchemaContext.getCurrentSchema();

        routing.put("tenantCode", tenantCode);
        routing.put("forcedSchema", forcedSchema);
        routing.put("hasForcedSchema", SchemaContext.hasForcedSchema());

        // Determine expected datasource key
        String expectedKey;
        if (SchemaContext.hasForcedSchema()) {
            if ("master".equals(forcedSchema)) {
                expectedKey = "master";
            } else if ("tenant".equals(forcedSchema)) {
                expectedKey = tenantCode != null ? "tenant_" + tenantCode.toLowerCase() : "master";
            } else {
                expectedKey = forcedSchema;
            }
        } else if (tenantCode != null) {
            expectedKey = "tenant_" + tenantCode.toLowerCase();
        } else {
            expectedKey = "master";
        }

        routing.put("expectedDatasourceKey", expectedKey);
        routing.put("explanation", generateRoutingExplanation(tenantCode, forcedSchema, expectedKey));

        return ApiResponse.success("Routing decision explained", routing);
    }

    @GetMapping("/test-master")
    public ApiResponse<Map<String, Object>> testMasterSchema() {
        Map<String, Object> result = new HashMap<>();
        result.put("schemaContext", SchemaContext.getCurrentSchema());

        try {
            String database = jdbcTemplate.queryForObject("SELECT DATABASE()", String.class);
            result.put("database", database);

            Long userCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Long.class);
            result.put("userCount", userCount);
            result.put("success", true);
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            log.error("Master schema test failed", e);
        }

        return ApiResponse.success("Master schema test completed", result);
    }

    @GetMapping("/test-tenant")
    public ApiResponse<Map<String, Object>> testTenantSchema() {
        Map<String, Object> result = new HashMap<>();
        result.put("tenantContext", TenantContext.getCurrentTenant());
        result.put("schemaContext", SchemaContext.getCurrentSchema());

        try {
            String database = jdbcTemplate.queryForObject("SELECT DATABASE()", String.class);
            result.put("database", database);

            Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sections", Long.class);
            result.put("sectionCount", count);
            result.put("success", true);
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            log.error("Tenant schema test failed", e);
        }

        return ApiResponse.success("Tenant schema test completed", result);
    }

    private String generateRoutingExplanation(String tenantCode, String forcedSchema, String expectedKey) {
        if (forcedSchema != null && "master".equals(forcedSchema)) {
            return "Schema explicitly forced to MASTER via SchemaContext";
        } else if (forcedSchema != null && "tenant".equals(forcedSchema)) {
            return tenantCode != null
                    ? "Schema explicitly forced to TENANT via SchemaContext, using tenant: " + tenantCode
                    : "Schema forced to TENANT but no tenant context available, defaulting to master";
        } else if (tenantCode != null) {
            return "No forced schema, routing based on TenantContext: " + tenantCode;
        } else {
            return "No forced schema or tenant context, defaulting to master";
        }
    }

}
