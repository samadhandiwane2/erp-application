package com.erp.tenant.service;

import com.erp.common.annotation.ForceTenantSchema;
import com.erp.common.jwt.UserPrincipal;
import com.erp.tenant.dto.section.CreateSectionRequest;
import com.erp.tenant.dto.section.SectionResponse;
import com.erp.tenant.dto.section.UpdateSectionRequest;
import com.erp.tenant.entity.Class;
import com.erp.tenant.entity.Section;
import com.erp.tenant.repository.ClassRepository;
import com.erp.tenant.repository.SectionRepository;
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
public class SectionService {

    private final SectionRepository sectionRepository;
    private final ClassRepository classRepository;

    @Transactional
    public SectionResponse createSection(CreateSectionRequest request, UserPrincipal currentUser) {
        log.info("Creating section: {} for class: {}", request.getSectionName(), request.getClassId());

        Class classEntity = classRepository.findByIdAndIsActiveTrue(request.getClassId())
                .orElseThrow(() -> new RuntimeException("Class not found"));

        // Check if section code already exists for this class
        if (sectionRepository.existsByClassEntityIdAndSectionCodeAndIsActiveTrue(
                request.getClassId(), request.getSectionCode())) {
            throw new RuntimeException("Section with code " + request.getSectionCode() +
                    " already exists for this class");
        }

        Section section = new Section();
        section.setClassEntity(classEntity);
        section.setSectionName(request.getSectionName());
        section.setSectionCode(request.getSectionCode());
        section.setMaxStudents(request.getMaxStudents());
        section.setRoomNumber(request.getRoomNumber());
        section.setCreatedBy(currentUser.getId());

        Section saved = sectionRepository.save(section);
        return mapToResponse(saved);
    }

    @Transactional
    public SectionResponse updateSection(Long id, UpdateSectionRequest request, UserPrincipal currentUser) {
        Section section = sectionRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new RuntimeException("Section not found"));

        if (request.getSectionName() != null) {
            section.setSectionName(request.getSectionName());
        }
        if (request.getMaxStudents() != null) {
            section.setMaxStudents(request.getMaxStudents());
        }
        if (request.getRoomNumber() != null) {
            section.setRoomNumber(request.getRoomNumber());
        }
        if (request.getIsActive() != null) {
            section.setIsActive(request.getIsActive());
        }

        section.setUpdatedBy(currentUser.getId());
        Section saved = sectionRepository.save(section);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public SectionResponse getSectionById(Long id) {
        Section section = sectionRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new RuntimeException("Section not found"));
        return mapToResponse(section);
    }

    @Transactional(readOnly = true)
    public List<SectionResponse> getSectionsByClassId(Long classId) {
        return sectionRepository.findByClassIdOrderBySectionCode(classId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SectionResponse> getAllSections() {
        return sectionRepository.findAll()
                .stream()
                .filter(Section::getIsActive)
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private SectionResponse mapToResponse(Section section) {
        Long studentCount = sectionRepository.countActiveStudentsInSection(
                section.getClassEntity().getId(), section.getId());

        return SectionResponse.builder()
                .id(section.getId())
                .classId(section.getClassEntity().getId())
                .className(section.getClassEntity().getClassName())
                .sectionName(section.getSectionName())
                .sectionCode(section.getSectionCode())
                .maxStudents(section.getMaxStudents())
                .currentStudents(studentCount.intValue())
                .roomNumber(section.getRoomNumber())
                .createdAt(section.getCreatedAt())
                .updatedAt(section.getUpdatedAt())
                .isActive(section.getIsActive())
                .build();
    }

}
