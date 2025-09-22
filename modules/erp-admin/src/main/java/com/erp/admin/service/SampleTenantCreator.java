package com.erp.admin.service;

import com.erp.common.dto.tenant.CreateTenantRequest;
import com.erp.common.jwt.UserPrincipal;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")  // Only run in development
public class SampleTenantCreator implements CommandLineRunner {

    private final TenantManagementService tenantManagementService;

    public SampleTenantCreator(TenantManagementService tenantManagementService) {
        this.tenantManagementService = tenantManagementService;
    }

    @Override
    public void run(String... args) throws Exception {
        // Create a sample tenant for testing
        createSampleTenant();
    }

    private void createSampleTenant() {
        try {
            CreateTenantRequest request = new CreateTenantRequest();
            request.setTenantName("Springfield Elementary School");
            request.setTenantCode("SPRING");
            request.setContactEmail("admin@springfield.edu");
            request.setContactPhone("+1234567890");
            request.setSubscriptionMonths(12);

            // Admin details
            request.setAdminUsername("spring.admin");
            request.setAdminEmail("admin@springfield.edu");
            request.setAdminFirstName("John");
            request.setAdminLastName("Smith");
            request.setAdminPhone("+1234567890");

            // Create a mock super admin principal for testing
            UserPrincipal mockSuperAdmin = new UserPrincipal(
                    1L, "superadmin", "superadmin@erp.com", "password",
                    com.erp.common.entity.User.UserType.SUPER_ADMIN,
                    null, null, true, true
            );

            tenantManagementService.createTenant(request, mockSuperAdmin);

        } catch (Exception e) {
            // Tenant might already exist, that's okay for demo
            System.out.println("Sample tenant creation skipped: " + e.getMessage());
        }
    }

}
