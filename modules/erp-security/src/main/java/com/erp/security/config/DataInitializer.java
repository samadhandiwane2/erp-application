package com.erp.security.config;

import com.erp.common.entity.Tenant;
import com.erp.common.entity.User;
import com.erp.common.repository.TenantRepository;
import com.erp.security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        initializeDefaultData();
    }

    private void initializeDefaultData() {
        // Create Super Admin if not exists
        if (!(userRepository.existsByUsernameAndIsActiveTrue("superadmin") == 1)) {
            User superAdmin = new User();
            superAdmin.setUsername("superadmin");
            superAdmin.setEmail("superadmin@erp.com");
            superAdmin.setPasswordHash(passwordEncoder.encode("SuperAdmin@123"));
            superAdmin.setFirstName("Super");
            superAdmin.setLastName("Admin");
            superAdmin.setUserType(User.UserType.SUPER_ADMIN);
            superAdmin.setTenantId(null); // Super admin belongs to no tenant
            superAdmin.setIsActive(true);
            superAdmin.setCreatedAt(LocalDateTime.now());

            userRepository.save(superAdmin);
            log.info("Super Admin created with username: superadmin, password: SuperAdmin@123");
        }

        // Create a demo tenant if not exists
        if (!(tenantRepository.existsByTenantCodeAndIsActiveTrue("DEMO") == 1)) {
            Tenant demoTenant = new Tenant();
            demoTenant.setTenantName("Demo Company");
            demoTenant.setTenantCode("DEMO");
            demoTenant.setSchemaName("demo_schema");
            demoTenant.setContactEmail("demo@company.com");
            demoTenant.setContactPhone("+1-555-0123");
            demoTenant.setStatus(Tenant.TenantStatus.ACTIVE);
            demoTenant.setSubscriptionStartDate(LocalDateTime.now());
            demoTenant.setSubscriptionEndDate(LocalDateTime.now().plusYears(1));
            demoTenant.setIsActive(true);
            demoTenant.setCreatedAt(LocalDateTime.now());

            Tenant savedTenant = tenantRepository.save(demoTenant);
            log.info("Demo tenant created with code: DEMO");

            // Create tenant admin for demo tenant
            if (!(userRepository.existsByUsernameAndIsActiveTrue("demo.admin") == 1)) {
                User tenantAdmin = new User();
                tenantAdmin.setUsername("demo.admin");
                tenantAdmin.setEmail("admin@demo.com");
                tenantAdmin.setPasswordHash(passwordEncoder.encode("DemoAdmin@123"));
                tenantAdmin.setFirstName("Demo");
                tenantAdmin.setLastName("Admin");
                tenantAdmin.setUserType(User.UserType.TENANT_ADMIN);
                tenantAdmin.setTenantId(savedTenant.getId());
                tenantAdmin.setIsActive(true);
                tenantAdmin.setCreatedAt(LocalDateTime.now());

                userRepository.save(tenantAdmin);
                log.info("Demo tenant admin created with username: demo.admin, password: DemoAdmin@123");
            }

            // Create tenant manager for demo tenant
            if (!(userRepository.existsByUsernameAndIsActiveTrue("demo.manager") == 1)) {
                User tenantManager = new User();
                tenantManager.setUsername("demo.manager");
                tenantManager.setEmail("manager@demo.com");
                tenantManager.setPasswordHash(passwordEncoder.encode("DemoManager@123"));
                tenantManager.setFirstName("Demo");
                tenantManager.setLastName("Manager");
                tenantManager.setUserType(User.UserType.TENANT_MANAGER);
                tenantManager.setTenantId(savedTenant.getId());
                tenantManager.setIsActive(true);
                tenantManager.setCreatedAt(LocalDateTime.now());

                userRepository.save(tenantManager);
                log.info("Demo tenant manager created with username: demo.manager, password: DemoManager@123");
            }

            // Create tenant user for demo tenant
            if (!(userRepository.existsByUsernameAndIsActiveTrue("demo.user") == 1)) {
                User tenantUser = new User();
                tenantUser.setUsername("demo.user");
                tenantUser.setEmail("user@demo.com");
                tenantUser.setPasswordHash(passwordEncoder.encode("DemoUser@123"));
                tenantUser.setFirstName("Demo");
                tenantUser.setLastName("User");
                tenantUser.setUserType(User.UserType.TENANT_USER);
                tenantUser.setTenantId(savedTenant.getId());
                tenantUser.setIsActive(true);
                tenantUser.setCreatedAt(LocalDateTime.now());

                userRepository.save(tenantUser);
                log.info("Demo tenant user created with username: demo.user, password: DemoUser@123");
            }
        }

        log.info("Data initialization completed!");
        log.info("=== LOGIN CREDENTIALS ===");
        log.info("Super Admin: superadmin / SuperAdmin@123 (no tenant code needed)");
        log.info("Tenant Admin: demo.admin / NewDemoAdmin@456 (tenant code: DEMO)");
        log.info("Tenant Manager: demo.manager / DemoManager@123 (tenant code: DEMO)");
        log.info("Tenant User: demo.user / DemoUser@123 (tenant code: DEMO)");
        log.info("========================");
    }
}
