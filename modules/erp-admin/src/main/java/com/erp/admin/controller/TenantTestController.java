package com.erp.admin.controller;

import com.erp.common.dto.ApiResponse;
import com.erp.common.context.TenantContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/test")
public class TenantTestController {

    @GetMapping("/tenant-context")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTenantContext() {
        Map<String, Object> context = new HashMap<>();
        context.put("tenantId", TenantContext.getCurrentTenantId());
        context.put("tenantKey", TenantContext.getCurrentTenant());
        context.put("schemaName", TenantContext.getCurrentSchema());

        return ResponseEntity.ok(ApiResponse.success("Current tenant context", context));
    }

}