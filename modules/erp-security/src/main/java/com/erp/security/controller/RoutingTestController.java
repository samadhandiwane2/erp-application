package com.erp.security.controller;

import com.erp.common.context.SchemaContext;
import com.erp.common.context.TenantContext;
import com.erp.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test/routing")
@RequiredArgsConstructor
@Slf4j
public class RoutingTestController {

    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/context-info")
    public ApiResponse<Map<String, String>> getContextInfo() {
        Map<String, String> info = new HashMap<>();

        info.put("tenantContext", TenantContext.getCurrentTenant());
        info.put("tenantId", String.valueOf(TenantContext.getCurrentTenantId()));
        info.put("schemaContext", SchemaContext.getCurrentSchema());
        info.put("tenantKey", TenantContext.getCurrentTenantKey());

        // Try to get current database
        try {
            String database = jdbcTemplate.queryForObject("SELECT DATABASE()", String.class);
            info.put("actualDatabase", database);
        } catch (Exception e) {
            info.put("actualDatabase", "Error: " + e.getMessage());
        }

        log.info("Context Info: {}", info);
        return ApiResponse.success("Context information retrieved", info);
    }

    @GetMapping("/master-test")
    public ApiResponse<Map<String, Object>> testMasterSchema() {
        Map<String, Object> result = new HashMap<>();

        result.put("schemaContext", SchemaContext.getCurrentSchema());

        try {
            String database = jdbcTemplate.queryForObject("SELECT DATABASE()", String.class);
            result.put("database", database);

            Long userCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM users", Long.class);
            result.put("userCount", userCount);

            result.put("success", true);
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            log.error("Master schema test failed", e);
        }

        return ApiResponse.success("Master schema test completed", result);
    }

    @GetMapping("/tenant-test")
    public ApiResponse<Map<String, Object>> testTenantSchema() {
        Map<String, Object> result = new HashMap<>();

        result.put("tenantContext", TenantContext.getCurrentTenant());
        result.put("schemaContext", SchemaContext.getCurrentSchema());

        try {
            String database = jdbcTemplate.queryForObject("SELECT DATABASE()", String.class);
            result.put("database", database);

            // Try to query a tenant-specific table
            // Adjust table name based on your schema
            Long count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM sections", Long.class);
            result.put("sectionCount", count);

            result.put("success", true);
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            log.error("Tenant schema test failed", e);
        }

        return ApiResponse.success("Tenant schema test completed", result);
    }

}