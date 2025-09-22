package com.erp.tenant.service;

import com.erp.common.annotation.ForceTenantSchema;
import com.erp.common.jwt.UserPrincipal;
import com.erp.tenant.dto.classes.ClassResponse;
import com.erp.tenant.dto.classes.CreateClassRequest;
import com.erp.tenant.dto.classes.UpdateClassRequest;
import com.erp.tenant.dto.section.SectionResponse;
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
public class ClassService {

    private final ClassRepository classRepository;
    private final SectionRepository sectionRepository;

    @Transactional
    public ClassResponse createClass(CreateClassRequest request, UserPrincipal currentUser) {
        log.info("Creating class: {}", request.getClassName());

        // Check if class code already exists
        if (classRepository.existsByClassCodeAndIsActiveTrue(request.getClassCode())) {
            throw new RuntimeException("Class with code " + request.getClassCode() + " already exists");
        }

        Class classEntity = new Class();
        classEntity.setClassName(request.getClassName());
        classEntity.setClassCode(request.getClassCode());
        classEntity.setGradeLevel(request.getGradeLevel());
        classEntity.setDescription(request.getDescription());
        classEntity.setMaxStudents(request.getMaxStudents());
        classEntity.setCreatedBy(currentUser.getId());

        Class saved = classRepository.save(classEntity);
        return mapToResponse(saved);
    }

    @Transactional
    public ClassResponse updateClass(Long id, UpdateClassRequest request, UserPrincipal currentUser) {
        Class classEntity = classRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new RuntimeException("Class not found"));

        if (request.getClassName() != null) {
            classEntity.setClassName(request.getClassName());
        }
        if (request.getDescription() != null) {
            classEntity.setDescription(request.getDescription());
        }
        if (request.getMaxStudents() != null) {
            classEntity.setMaxStudents(request.getMaxStudents());
        }
        if (request.getIsActive() != null) {
            classEntity.setIsActive(request.getIsActive());
        }

        classEntity.setUpdatedBy(currentUser.getId());
        Class saved = classRepository.save(classEntity);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public ClassResponse getClassById(Long id) {
        Class classEntity = classRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new RuntimeException("Class not found"));
        return mapToResponse(classEntity);
    }

    @Transactional(readOnly = true)
    public List<ClassResponse> getAllClasses() {
        return classRepository.findByIsActiveTrueOrderByGradeLevel()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private ClassResponse mapToResponse(Class classEntity) {
        List<Section> sections = sectionRepository.findByClassEntityIdAndIsActiveTrue(classEntity.getId());
        Long studentCount = classRepository.countActiveStudentsInClass(classEntity.getId());

        return ClassResponse.builder()
                .id(classEntity.getId())
                .className(classEntity.getClassName())
                .classCode(classEntity.getClassCode())
                .gradeLevel(classEntity.getGradeLevel())
                .description(classEntity.getDescription())
                .maxStudents(classEntity.getMaxStudents())
                .currentStudents(studentCount.intValue())
                .sections(sections.stream()
                        .map(this::mapSectionToResponse)
                        .collect(Collectors.toList()))
                .createdAt(classEntity.getCreatedAt())
                .updatedAt(classEntity.getUpdatedAt())
                .isActive(classEntity.getIsActive())
                .build();
    }

    private SectionResponse mapSectionToResponse(Section section) {
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
