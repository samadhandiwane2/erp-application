package com.erp.tenant.service;

import com.erp.common.annotation.ForceTenantSchema;
import com.erp.common.jwt.UserPrincipal;
import com.erp.tenant.dto.academicYear.AcademicYearResponse;
import com.erp.tenant.dto.academicYear.CreateAcademicYearRequest;
import com.erp.tenant.dto.academicYear.UpdateAcademicYearRequest;
import com.erp.tenant.entity.AcademicYear;
import com.erp.tenant.repository.AcademicYearRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@ForceTenantSchema
public class AcademicYearService {

    private final AcademicYearRepository academicYearRepository;

    @Transactional
    public AcademicYearResponse createAcademicYear(CreateAcademicYearRequest request, UserPrincipal currentUser) {
        log.info("Creating academic year: {}", request.getYearName());

        // Check if year already exists
        if (academicYearRepository.existsByYearNameAndIsActiveTrue(request.getYearName())) {
            throw new RuntimeException("Academic year with name " + request.getYearName() + " already exists");
        }

        // Validate date range
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new RuntimeException("End date must be after start date");
        }

        // If marking as current, unset other current years
        if (Boolean.TRUE.equals(request.getIsCurrent())) {
            academicYearRepository.findByIsCurrentTrue()
                    .ifPresent(current -> {
                        current.setIsCurrent(false);
                        academicYearRepository.save(current);
                    });
        }

        AcademicYear academicYear = new AcademicYear();
        academicYear.setYearName(request.getYearName());
        academicYear.setStartDate(request.getStartDate());
        academicYear.setEndDate(request.getEndDate());
        academicYear.setIsCurrent(request.getIsCurrent());
        academicYear.setDescription(request.getDescription());
        academicYear.setCreatedBy(currentUser.getId());

        AcademicYear saved = academicYearRepository.save(academicYear);
        return mapToResponse(saved);
    }

    @Transactional
    public AcademicYearResponse updateAcademicYear(Long id, UpdateAcademicYearRequest request, UserPrincipal currentUser) {
        AcademicYear academicYear = academicYearRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Academic year not found"));

        if (request.getYearName() != null) {
            academicYear.setYearName(request.getYearName());
        }
        if (request.getStartDate() != null) {
            academicYear.setStartDate(request.getStartDate());
        }
        if (request.getEndDate() != null) {
            academicYear.setEndDate(request.getEndDate());
        }
        if (request.getDescription() != null) {
            academicYear.setDescription(request.getDescription());
        }
        if (request.getIsActive() != null) {
            academicYear.setIsActive(request.getIsActive());
        }

        // Handle current year update
        if (Boolean.TRUE.equals(request.getIsCurrent()) && !academicYear.getIsCurrent()) {
            academicYearRepository.findByIsCurrentTrue()
                    .ifPresent(current -> {
                        if (!current.getId().equals(id)) {
                            current.setIsCurrent(false);
                            academicYearRepository.save(current);
                        }
                    });
            academicYear.setIsCurrent(true);
        } else if (Boolean.FALSE.equals(request.getIsCurrent())) {
            academicYear.setIsCurrent(false);
        }

        academicYear.setUpdatedBy(currentUser.getId());
        AcademicYear saved = academicYearRepository.save(academicYear);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public AcademicYearResponse getAcademicYearById(Long id) {
        AcademicYear academicYear = academicYearRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Academic year not found"));
        return mapToResponse(academicYear);
    }

    @Transactional(readOnly = true)
    public List<AcademicYearResponse> getAllAcademicYears() {
        return academicYearRepository.findByIsActiveTrueOrderByStartDateDesc()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AcademicYearResponse getCurrentAcademicYear() {
        AcademicYear current = academicYearRepository.findByIsCurrentTrue()
                .orElseThrow(() -> new RuntimeException("No current academic year set"));
        return mapToResponse(current);
    }

    private AcademicYearResponse mapToResponse(AcademicYear academicYear) {
        return AcademicYearResponse.builder()
                .id(academicYear.getId())
                .yearName(academicYear.getYearName())
                .startDate(academicYear.getStartDate())
                .endDate(academicYear.getEndDate())
                .isCurrent(academicYear.getIsCurrent())
                .description(academicYear.getDescription())
                .createdAt(academicYear.getCreatedAt())
                .updatedAt(academicYear.getUpdatedAt())
                .isActive(academicYear.getIsActive())
                .build();
    }

}