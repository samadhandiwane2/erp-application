package com.erp.admin.service;

import com.erp.common.annotation.ForceMasterSchema;
import com.erp.common.config.MultiTenantDataSourceConfig;
import com.erp.common.dto.tenant.CreateTenantRequest;
import com.erp.common.dto.tenant.TenantResponse;
import com.erp.common.dto.tenant.TenantSearchRequest;
import com.erp.common.dto.tenant.UpdateTenantRequest;
import com.erp.common.entity.Tenant;
import com.erp.common.entity.User;
import com.erp.common.jwt.UserPrincipal;
import com.erp.common.repository.TenantRepository;
import com.erp.common.service.DatabaseInitializationService;
import com.erp.common.service.FlywayMigrationService;
import com.erp.security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@ForceMasterSchema
public class TenantManagementService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SchemaManagementService schemaManagementService;
    private final FlywayMigrationService flywayMigrationService;
    private final MultiTenantDataSourceConfig dataSourceConfig;
    private final TenantDataSeederService tenantDataSeederService;
    private final DatabaseInitializationService databaseInitializationService;

    public TenantResponse createTenant(CreateTenantRequest request, UserPrincipal currentUser) {

        // Validate tenant code uniqueness
        if (tenantRepository.existsByTenantCodeAndIsActiveTrue(request.getTenantCode()) == 1) {
            throw new RuntimeException("Tenant code already exists: " + request.getTenantCode());
        }
        // Validate admin username uniqueness
        if (userRepository.existsByUsernameAndIsActiveTrue(request.getAdminUsername()) == 1) {
            throw new RuntimeException("Admin username already exists: " + request.getAdminUsername());
        }
        // Validate admin email uniqueness
        if (userRepository.existsByEmailAndIsActiveTrue(request.getAdminEmail()) == 1) {
            throw new RuntimeException("Admin email already exists: " + request.getAdminEmail());
        }

        String schemaName = null;
        try {
            // Generate schema name
            schemaName = generateSchemaName(request.getTenantCode());

            // Step 1: Create tenant entity in master database
            Tenant tenant = new Tenant();
            tenant.setTenantName(request.getTenantName());
            tenant.setTenantCode(request.getTenantCode());
            tenant.setSchemaName(schemaName);
            tenant.setContactEmail(request.getContactEmail());
            tenant.setContactPhone(request.getContactPhone());
            tenant.setStatus(Tenant.TenantStatus.ACTIVE);
            tenant.setSubscriptionStartDate(LocalDateTime.now());
            tenant.setSubscriptionEndDate(LocalDateTime.now().plusMonths(request.getSubscriptionMonths()));
            tenant.setIsActive(true);
            tenant.setCreatedBy(currentUser.getId());
            tenant.setCreatedAt(LocalDateTime.now());

            Tenant savedTenant = tenantRepository.save(tenant);
            log.info("Step 1: Created tenant record: {} with ID: {}", savedTenant.getTenantCode(), savedTenant.getId());

            // Step 2: Create database schema and run migrations using DatabaseInitializationService
            // This replaces the manual schema creation and migration calls
            databaseInitializationService.initializeNewTenantSchema(tenant.getTenantCode());
            log.info("Step 2: Created and migrated database schema: {}", schemaName);

            // Step 3: Add datasource for the new tenant
            dataSourceConfig.addTenantDataSource(request.getTenantCode(), schemaName);
            log.info("Step 3: Added datasource to pool for tenant: {}", request.getTenantCode());

            // Step 4: Get the tenant datasource
            DataSource tenantDataSource = dataSourceConfig.getTenantDataSource(schemaName);
            log.info("Step 4: Retrieved tenant datasource for schema: {}", schemaName);

            // Step 5: Verify tables exist before seeding (optional)
            if (verifyTablesExist(tenantDataSource)) {
                // Step 6: Seed initial data
                tenantDataSeederService.seedInitialData(tenantDataSource, schemaName);
                log.info("Step 5: Seeded initial data for schema: {}", schemaName);
            } else {
                log.warn("Tables not created properly for schema: {}, skipping data seeding", schemaName);
            }

            // Step 7: Create tenant admin user
            String tempPassword = generateTemporaryPassword();
            User tenantAdmin = createTenantAdmin(request, savedTenant, currentUser, tempPassword);
            log.info("Step 6: Created tenant admin: {} with temporary password", tenantAdmin.getUsername());

            // Log the temporary password (in production, send via email)
            log.info("==============================================");
            log.info("TEMPORARY PASSWORD for {}: {}", tenantAdmin.getUsername(), tempPassword);
            log.info("==============================================");

            // Step 8: Create response
            TenantResponse response = convertToResponse(savedTenant);
            response.setTenantAdmin(convertToTenantAdminInfo(tenantAdmin));

            log.info("Successfully created tenant: {} with admin: {}", savedTenant.getTenantCode(), tenantAdmin.getUsername());

            return response;

        } catch (Exception e) {
            log.error("Failed to create tenant: {}", request.getTenantCode(), e);

            // Cleanup on failure
            if (schemaName != null) {
                try {
                    log.info("Attempting cleanup for failed tenant creation...");

                    // Remove datasource first
                    dataSourceConfig.removeTenantDataSource(request.getTenantCode());
                    log.info("Removed datasource for tenant: {}", request.getTenantCode());

                    // Then drop schema
                    schemaManagementService.dropTenantSchema(schemaName);
                    log.info("Dropped schema: {}", schemaName);

                } catch (Exception cleanupError) {
                    log.error("Failed to cleanup after tenant creation failure", cleanupError);
                }
            }

            throw new RuntimeException("Failed to create tenant: " + e.getMessage(), e);
        }
    }

    /**
     * NEW METHOD: Update existing tenant schema to latest migrations
     * Use this when you add V4, V5, etc. migration files
     */
    public void updateTenantSchema(String tenantCode) {
        try {
            log.info("Updating tenant schema for tenant: {}", tenantCode);

            // Validate tenant exists
            Tenant tenant = tenantRepository.findByTenantCodeAndIsActiveTrue(tenantCode)
                    .orElseThrow(() -> new RuntimeException("Tenant not found: " + tenantCode));

            // Update schema using DatabaseInitializationService
            databaseInitializationService.updateTenantSchema(tenantCode);

            log.info("Successfully updated tenant schema for: {}", tenantCode);

        } catch (Exception e) {
            log.error("Failed to update tenant schema for: {}", tenantCode, e);
            throw new RuntimeException("Tenant schema update failed for: " + tenantCode, e);
        }
    }

    /**
     * NEW METHOD: Migrate all existing tenants to latest schema version
     * Use this when you want to apply new migrations to all tenants at once
     */
    public void migrateAllTenantsToLatest() {
        try {
            log.info("Starting migration of all tenant schemas to latest version");
            databaseInitializationService.migrateAllExistingTenantSchemas();
            log.info("Successfully migrated all tenant schemas");

        } catch (Exception e) {
            log.error("Failed to migrate all tenant schemas", e);
            throw new RuntimeException("Bulk tenant migration failed", e);
        }
    }

    /**
     * NEW METHOD: Get database initialization status
     */
    public DatabaseInitializationService.DatabaseInitializationStatus getDatabaseStatus() {
        return databaseInitializationService.getInitializationStatus();
    }

    private boolean verifyTablesExist(DataSource dataSource) {
        try (var connection = dataSource.getConnection();
             var statement = connection.createStatement()) {

            // Check if a critical table exists
            var resultSet = statement.executeQuery(
                    "SELECT COUNT(*) FROM information_schema.tables " +
                            "WHERE table_schema = DATABASE() " +
                            "AND table_name = 'academic_years'"
            );

            if (resultSet.next()) {
                return resultSet.getInt(1) > 0;
            }
            return false;

        } catch (Exception e) {
            log.error("Error verifying tables existence", e);
            return false;
        }
    }

    public TenantResponse updateTenant(Long tenantId, UpdateTenantRequest request, UserPrincipal currentUser) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found: " + tenantId));

        // Update fields
        if (request.getTenantName() != null) {
            tenant.setTenantName(request.getTenantName());
        }
        if (request.getContactEmail() != null) {
            tenant.setContactEmail(request.getContactEmail());
        }
        if (request.getContactPhone() != null) {
            tenant.setContactPhone(request.getContactPhone());
        }
        if (request.getStatus() != null) {
            tenant.setStatus(request.getStatus());
        }
        if (request.getSubscriptionEndDate() != null) {
            tenant.setSubscriptionEndDate(request.getSubscriptionEndDate());
        }

        tenant.setUpdatedBy(currentUser.getId());
        tenant.setUpdatedAt(LocalDateTime.now());

        Tenant updatedTenant = tenantRepository.save(tenant);

        return convertToResponse(updatedTenant);
    }

    public void suspendTenant(Long tenantId, UserPrincipal currentUser) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found: " + tenantId));

        tenant.setStatus(Tenant.TenantStatus.SUSPENDED);
        tenant.setUpdatedBy(currentUser.getId());
        tenant.setUpdatedAt(LocalDateTime.now());

        tenantRepository.save(tenant);

        // Optionally disable all tenant users
        userRepository.findByTenantIdAndIsActiveTrue(tenantId)
                .forEach(user -> {
                    user.setIsActive(false);
                    user.setUpdatedBy(currentUser.getId());
                    user.setUpdatedAt(LocalDateTime.now());
                    userRepository.save(user);
                });
    }

    public void deleteTenant(Long tenantId, UserPrincipal currentUser) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found: " + tenantId));

        try {
            // Soft delete tenant
            tenant.setIsActive(false);
            tenant.setStatus(Tenant.TenantStatus.INACTIVE);
            tenant.setUpdatedBy(currentUser.getId());
            tenant.setUpdatedAt(LocalDateTime.now());

            tenantRepository.save(tenant);

            // Disable all tenant users
            userRepository.findByTenantIdAndIsActiveTrue(tenantId)
                    .forEach(user -> {
                        user.setIsActive(false);
                        user.setUpdatedBy(currentUser.getId());
                        user.setUpdatedAt(LocalDateTime.now());
                        userRepository.save(user);
                    });

            // Remove datasource
            dataSourceConfig.removeTenantDataSource(tenant.getTenantCode());

            log.info("Tenant deleted (soft): {}", tenant.getTenantCode());

        } catch (Exception e) {
            log.error("Failed to delete tenant: {}", tenantId, e);
            throw new RuntimeException("Failed to delete tenant: " + e.getMessage(), e);
        }
    }

    public TenantResponse getTenantById(Long tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found: " + tenantId));

        return convertToResponse(tenant);
    }

    public Page<TenantResponse> searchTenants(TenantSearchRequest request) {
        Sort sort = Sort.by(
                "ASC".equalsIgnoreCase(request.getSortDirection()) ?
                        Sort.Direction.ASC : Sort.Direction.DESC,
                request.getSortBy()
        );

        PageRequest pageRequest = PageRequest.of(request.getPage(), request.getSize(), sort);

        Page<Tenant> tenants = tenantRepository.findTenantsWithFilters(
                request.getTenantName(),
                request.getTenantCode(),
                request.getContactEmail(),
                request.getStatus().name(),
                request.getIsActive(),
                request.getSubscriptionStartAfter(),
                request.getSubscriptionEndBefore(),
                pageRequest
        );

        return tenants.map(this::convertToResponse);
    }

    private String generateSchemaName(String tenantCode) {
        return "tenant_" + tenantCode.toLowerCase();
    }

    private User createTenantAdmin(CreateTenantRequest request, Tenant tenant, UserPrincipal currentUser, String tempPassword) {
        User tenantAdmin = new User();
        tenantAdmin.setUsername(request.getAdminUsername());
        tenantAdmin.setEmail(request.getAdminEmail());
        tenantAdmin.setPasswordHash(passwordEncoder.encode(tempPassword));
        tenantAdmin.setFirstName(request.getAdminFirstName());
        tenantAdmin.setLastName(request.getAdminLastName());
        tenantAdmin.setPhone(request.getAdminPhone());
        tenantAdmin.setUserType(User.UserType.TENANT_ADMIN);
        tenantAdmin.setTenantId(tenant.getId());
        tenantAdmin.setPasswordChangeRequired(true);
        tenantAdmin.setIsActive(true);
        tenantAdmin.setCreatedBy(currentUser.getId());
        tenantAdmin.setCreatedAt(LocalDateTime.now());

        return userRepository.save(tenantAdmin);
    }

    private String generateTemporaryPassword() {
        return "TempPass@" + UUID.randomUUID().toString().substring(0, 8);
    }

    private TenantResponse convertToResponse(Tenant tenant) {
        TenantResponse response = new TenantResponse();
        response.setId(tenant.getId());
        response.setTenantName(tenant.getTenantName());
        response.setTenantCode(tenant.getTenantCode());
        response.setSchemaName(tenant.getSchemaName());
        response.setContactEmail(tenant.getContactEmail());
        response.setContactPhone(tenant.getContactPhone());
        response.setStatus(tenant.getStatus());
        response.setSubscriptionStartDate(tenant.getSubscriptionStartDate());
        response.setSubscriptionEndDate(tenant.getSubscriptionEndDate());
        response.setCreatedAt(tenant.getCreatedAt());
        response.setUpdatedAt(tenant.getUpdatedAt());
        response.setIsActive(tenant.getIsActive());

        // Use findFirst or handle multiple admins
        Optional<User> tenantAdmin = userRepository.findFirstByTenantIdAndUserTypeAndIsActiveTrueOrderByCreatedAtDesc(
                tenant.getId(), User.UserType.TENANT_ADMIN.name()
        );
        tenantAdmin.ifPresent(admin -> response.setTenantAdmin(convertToTenantAdminInfo(admin)));

        return response;
    }

    private TenantResponse.TenantAdminInfo convertToTenantAdminInfo(User user) {
        TenantResponse.TenantAdminInfo adminInfo = new TenantResponse.TenantAdminInfo();
        adminInfo.setId(user.getId());
        adminInfo.setUsername(user.getUsername());
        adminInfo.setEmail(user.getEmail());
        adminInfo.setFirstName(user.getFirstName());
        adminInfo.setLastName(user.getLastName());
        adminInfo.setPhone(user.getPhone());
        adminInfo.setLastLogin(user.getLastLogin());
        return adminInfo;
    }

}
