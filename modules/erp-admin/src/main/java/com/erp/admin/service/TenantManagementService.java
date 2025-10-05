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
import com.erp.security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@ForceMasterSchema
public class TenantManagementService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SchemaManagementService schemaManagementService;
    private final MultiTenantDataSourceConfig dataSourceConfig;
    private final TenantDataSeederService tenantDataSeederService;
    private final DatabaseInitializationService databaseInitializationService;

    @Transactional
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
        Long tenantId = null;

        try {
            // Generate schema name
            schemaName = generateSchemaName(request.getTenantCode());

            // Step 1: Insert tenant using native query
            LocalDateTime now = LocalDateTime.now();
            int inserted = tenantRepository.insertTenant(
                    request.getTenantName(),
                    request.getTenantCode(),
                    schemaName,
                    null, // databaseUrl
                    request.getContactEmail(),
                    request.getContactPhone(),
                    Tenant.TenantStatus.ACTIVE.name(),
                    now, // subscriptionStartDate
                    now.plusMonths(request.getSubscriptionMonths()), // subscriptionEndDate
                    true, // isActive
                    now, // createdAt
                    currentUser.getId(), // createdBy
                    now, // updatedAt
                    currentUser.getId() // updatedBy
            );

            if (inserted == 0) {
                throw new RuntimeException("Failed to insert tenant");
            }

            // Get the inserted tenant ID
            tenantId = tenantRepository.getLastInsertId();
            log.info("Step 1: Created tenant record: {} with ID: {}", request.getTenantCode(), tenantId);

            // Fetch the created tenant for response
            Tenant savedTenant = tenantRepository.findById(tenantId)
                    .orElseThrow(() -> new RuntimeException("Failed to fetch created tenant"));

            // Step 2: Create database schema and run migrations
            databaseInitializationService.initializeNewTenantSchema(savedTenant.getTenantCode());
            log.info("Step 2: Created and migrated database schema: {}", schemaName);

            // Step 3: Add datasource for the new tenant
            dataSourceConfig.addTenantDataSource(request.getTenantCode(), schemaName);
            log.info("Step 3: Added datasource to pool for tenant: {}", request.getTenantCode());

            // Step 4: Get the tenant datasource
            DataSource tenantDataSource = dataSourceConfig.getTenantDataSource(schemaName);
            log.info("Step 4: Retrieved tenant datasource for schema: {}", schemaName);

            // Step 5: Verify tables exist before seeding
            if (verifyTablesExist(tenantDataSource)) {
                // Step 6: Seed initial data
                tenantDataSeederService.seedInitialData(tenantDataSource, schemaName);
                log.info("Step 5: Seeded initial data for schema: {}", schemaName);
            } else {
                log.warn("Tables not created properly for schema: {}, skipping data seeding", schemaName);
            }

            // Step 7: Create tenant admin user
            String tempPassword = generateTemporaryPassword();
            Long adminUserId = createTenantAdmin(request, savedTenant, currentUser, tempPassword);

            User tenantAdmin = userRepository.findById(adminUserId)
                    .orElseThrow(() -> new RuntimeException("Failed to fetch created admin user"));

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

    @Transactional
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

    public DatabaseInitializationService.DatabaseInitializationStatus getDatabaseStatus() {
        return databaseInitializationService.getInitializationStatus();
    }

    private boolean verifyTablesExist(DataSource dataSource) {
        try (var connection = dataSource.getConnection();
             var statement = connection.createStatement()) {

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

    @Transactional
    public TenantResponse updateTenant(Long tenantId, UpdateTenantRequest request, UserPrincipal currentUser) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found: " + tenantId));

        // Build update parameters with current or new values
        String tenantName = request.getTenantName() != null ? request.getTenantName() : tenant.getTenantName();
        String contactEmail = request.getContactEmail() != null ? request.getContactEmail() : tenant.getContactEmail();
        String contactPhone = request.getContactPhone() != null ? request.getContactPhone() : tenant.getContactPhone();
        String status = request.getStatus() != null ? request.getStatus().name() : tenant.getStatus().name();
        LocalDateTime subscriptionEndDate = request.getSubscriptionEndDate() != null ?
                request.getSubscriptionEndDate() : tenant.getSubscriptionEndDate();

        int updated = tenantRepository.updateTenant(
                tenantId,
                tenantName,
                contactEmail,
                contactPhone,
                status,
                subscriptionEndDate,
                LocalDateTime.now(),
                currentUser.getId()
        );

        if (updated == 0) {
            throw new RuntimeException("Failed to update tenant");
        }

        Tenant updatedTenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Failed to fetch updated tenant"));

        return convertToResponse(updatedTenant);
    }

    @Transactional
    public void suspendTenant(Long tenantId, UserPrincipal currentUser) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found: " + tenantId));

        int updated = tenantRepository.updateTenantStatus(
                tenantId,
                Tenant.TenantStatus.SUSPENDED.name(),
                LocalDateTime.now(),
                currentUser.getId()
        );

        if (updated == 0) {
            throw new RuntimeException("Failed to suspend tenant");
        }

        // Disable all tenant users
        userRepository.updateAllUsersByTenantId(
                tenantId,
                false,
                LocalDateTime.now(),
                currentUser.getId()
        );
    }

    @Transactional
    public void deleteTenant(Long tenantId, UserPrincipal currentUser) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found: " + tenantId));

        try {
            // Soft delete tenant
            int updated = tenantRepository.softDeleteTenant(
                    tenantId,
                    false,
                    Tenant.TenantStatus.INACTIVE.name(),
                    LocalDateTime.now(),
                    currentUser.getId()
            );

            if (updated == 0) {
                throw new RuntimeException("Failed to delete tenant");
            }

            // Disable all tenant users
            userRepository.updateAllUsersByTenantId(
                    tenantId,
                    false,
                    LocalDateTime.now(),
                    currentUser.getId()
            );

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
        // Map camelCase to snake_case for database columns
        String sortBy = mapSortField(request.getSortBy());

        Sort sort = Sort.by(
                "ASC".equalsIgnoreCase(request.getSortDirection()) ?
                        Sort.Direction.ASC : Sort.Direction.DESC,
                sortBy
        );
        PageRequest pageRequest = PageRequest.of(request.getPage(), request.getSize(), sort);

        Page<Tenant> tenants = tenantRepository.findTenantsWithFilters(
                request.getTenantName(),
                request.getTenantCode(),
                request.getContactEmail(),
                request.getStatus() != null ? request.getStatus().name() : null,
                request.getIsActive(),
                request.getSubscriptionStartAfter(),
                request.getSubscriptionEndBefore(),
                pageRequest
        );

        return tenants.map(this::convertToResponse);
    }

    private String mapSortField(String fieldName) {
        return switch (fieldName) {
            case "createdAt" -> "created_at";
            case "updatedAt" -> "updated_at";
            case "tenantName" -> "tenant_name";
            case "tenantCode" -> "tenant_code";
            case "contactEmail" -> "contact_email";
            case "contactPhone" -> "contact_phone";
            case "subscriptionStartDate" -> "subscription_start_date";
            case "subscriptionEndDate" -> "subscription_end_date";
            case "isActive" -> "is_active";
            default -> fieldName;
        };
    }

    private String generateSchemaName(String tenantCode) {
        return "tenant_" + tenantCode.toLowerCase();
    }

    private Long createTenantAdmin(CreateTenantRequest request, Tenant tenant, UserPrincipal currentUser, String tempPassword) {
        LocalDateTime now = LocalDateTime.now();

        int inserted = userRepository.insertUser(
                request.getAdminUsername(),
                request.getAdminEmail(),
                passwordEncoder.encode(tempPassword),
                request.getAdminFirstName(),
                request.getAdminLastName(),
                request.getAdminPhone(),
                User.UserType.TENANT_ADMIN.name(),
                tenant.getId(),
                true, // passwordChangeRequired
                0, // failedLoginAttempts
                true, // isActive
                now, // createdAt
                currentUser.getId(), // createdBy
                now, // updatedAt
                currentUser.getId() // updatedBy
        );

        if (inserted == 0) {
            throw new RuntimeException("Failed to create tenant admin user");
        }

        return userRepository.getLastInsertId();
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

    public List<TenantResponse> getAllActiveTenants() {
        List<Tenant> tenants = tenantRepository.findByIsActiveTrueOrderByCreatedAtDesc();
        return tenants.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

}