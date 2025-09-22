package com.erp.tenant.service;

import com.erp.common.annotation.ForceTenantSchema;
import com.erp.common.jwt.UserPrincipal;
import com.erp.tenant.dto.student.*;
import com.erp.tenant.entity.Guardian;
import com.erp.tenant.entity.Student;
import com.erp.tenant.repository.GuardianRepository;
import com.erp.tenant.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@ForceTenantSchema  // Force all methods to use tenant schema
public class StudentManagementService {

    private final StudentRepository studentRepository;
    private final GuardianRepository guardianRepository;

    @Transactional
    public StudentResponse createStudent(CreateStudentRequest request, UserPrincipal currentUser) {
        log.info("Creating new student: {} {}", request.getFirstName(), request.getLastName());

        // Validate unique constraints
        if (request.getAadharNumber() != null &&
                studentRepository.existsByAadharNumber(request.getAadharNumber())) {
            throw new RuntimeException("Aadhar number already exists");
        }

        if (request.getEmail() != null &&
                studentRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        // Generate admission number
        String admissionNumber = generateAdmissionNumber();

        // Create student entity
        Student student = new Student();
        student.setAdmissionNumber(admissionNumber);
        student.setFirstName(request.getFirstName());
        student.setLastName(request.getLastName());
        student.setDateOfBirth(request.getDateOfBirth());
        student.setGender(request.getGender());
        student.setEmail(request.getEmail());
        student.setPhone(request.getPhone());
        student.setAddress(request.getAddress());
        student.setCity(request.getCity());
        student.setState(request.getState());
        student.setPostalCode(request.getPostalCode());
        student.setCountry(request.getCountry());
        student.setAdmissionDate(request.getAdmissionDate());
        student.setCurrentClassId(request.getCurrentClassId());
        student.setCurrentSectionId(request.getCurrentSectionId());
        student.setAcademicYearId(request.getAcademicYearId());
        student.setBloodGroup(request.getBloodGroup());
        student.setReligion(request.getReligion());
        student.setCategory(request.getCategory());
        student.setNationality(request.getNationality());
        student.setMotherTongue(request.getMotherTongue());
        student.setAadharNumber(request.getAadharNumber());
        student.setPreviousSchool(request.getPreviousSchool());
        student.setMedicalConditions(request.getMedicalConditions());
        student.setEmergencyContactName(request.getEmergencyContactName());
        student.setEmergencyContactPhone(request.getEmergencyContactPhone());
        student.setStudentStatus(Student.StudentStatus.ACTIVE);
        student.setCreatedBy(currentUser.getId());
        student.setIsActive(true);

        // Generate roll number (class + section + sequence)
        String rollNumber = generateRollNumber(request.getCurrentClassId(), request.getCurrentSectionId());
        student.setRollNumber(rollNumber);

        Student savedStudent = studentRepository.save(student);

        // Create guardians
        List<Guardian> guardians = new ArrayList<>();
        if (request.getGuardians() != null && !request.getGuardians().isEmpty()) {
            boolean hasPrimary = false;
            for (CreateStudentRequest.GuardianInfo guardianInfo : request.getGuardians()) {
                Guardian guardian = createGuardian(savedStudent, guardianInfo);
                if (guardianInfo.getIsPrimaryContact() != null && guardianInfo.getIsPrimaryContact()) {
                    if (hasPrimary) {
                        guardian.setIsPrimaryContact(false);
                    } else {
                        hasPrimary = true;
                    }
                }
                guardians.add(guardianRepository.save(guardian));
            }

            // Ensure at least one primary contact
            if (!hasPrimary && !guardians.isEmpty()) {
                guardians.get(0).setIsPrimaryContact(true);
                guardianRepository.save(guardians.get(0));
            }
        }

        log.info("Student created successfully with admission number: {}", admissionNumber);

        return mapToResponse(savedStudent, guardians);
    }

    @Transactional
    public StudentResponse updateStudent(Long studentId, UpdateStudentRequest request, UserPrincipal currentUser) {
        log.info("Updating student: {}", studentId);

        Student student = studentRepository.findByIdAndIsActiveTrue(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        // Update fields if provided
        if (request.getFirstName() != null) {
            student.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            student.setLastName(request.getLastName());
        }
        if (request.getEmail() != null) {
            // Check uniqueness
            if (!request.getEmail().equals(student.getEmail()) &&
                    studentRepository.existsByEmail(request.getEmail())) {
                throw new RuntimeException("Email already exists");
            }
            student.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) {
            student.setPhone(request.getPhone());
        }
        if (request.getAddress() != null) {
            student.setAddress(request.getAddress());
        }
        if (request.getCity() != null) {
            student.setCity(request.getCity());
        }
        if (request.getState() != null) {
            student.setState(request.getState());
        }
        if (request.getPostalCode() != null) {
            student.setPostalCode(request.getPostalCode());
        }
        if (request.getCurrentClassId() != null) {
            student.setCurrentClassId(request.getCurrentClassId());
            // Regenerate roll number if class changes
            if (request.getCurrentSectionId() != null) {
                String newRollNumber = generateRollNumber(request.getCurrentClassId(), request.getCurrentSectionId());
                student.setRollNumber(newRollNumber);
            }
        }
        if (request.getCurrentSectionId() != null) {
            student.setCurrentSectionId(request.getCurrentSectionId());
        }
        if (request.getBloodGroup() != null) {
            student.setBloodGroup(request.getBloodGroup());
        }
        if (request.getReligion() != null) {
            student.setReligion(request.getReligion());
        }
        if (request.getCategory() != null) {
            student.setCategory(request.getCategory());
        }
        if (request.getMotherTongue() != null) {
            student.setMotherTongue(request.getMotherTongue());
        }
        if (request.getStudentStatus() != null) {
            student.setStudentStatus(request.getStudentStatus());
        }
        if (request.getMedicalConditions() != null) {
            student.setMedicalConditions(request.getMedicalConditions());
        }
        if (request.getEmergencyContactName() != null) {
            student.setEmergencyContactName(request.getEmergencyContactName());
        }
        if (request.getEmergencyContactPhone() != null) {
            student.setEmergencyContactPhone(request.getEmergencyContactPhone());
        }

        student.setUpdatedBy(currentUser.getId());
        student.setUpdatedAt(LocalDateTime.now());

        Student updatedStudent = studentRepository.save(student);

        // Get guardians
        List<Guardian> guardians = guardianRepository.findByStudentIdAndIsActiveTrue(studentId);

        return mapToResponse(updatedStudent, guardians);
    }

    @Transactional(readOnly = true)
    public StudentResponse getStudentById(Long studentId) {
        Student student = studentRepository.findByIdAndIsActiveTrue(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        List<Guardian> guardians = guardianRepository.findByStudentIdAndIsActiveTrue(studentId);

        return mapToResponse(student, guardians);
    }

    @Transactional(readOnly = true)
    public Page<StudentResponse> searchStudents(StudentSearchRequest request) {
        Sort sort = Sort.by(
                "ASC".equalsIgnoreCase(request.getSortDirection()) ?
                        Sort.Direction.ASC : Sort.Direction.DESC,
                request.getSortBy()
        );

        PageRequest pageRequest = PageRequest.of(request.getPage(), request.getSize(), sort);

        Page<Student> students = studentRepository.searchStudents(
                request.getFirstName(),
                request.getLastName(),
                request.getAdmissionNumber(),
                request.getClassId(),
                request.getSectionId(),
                request.getStatus(),
                request.getGender(),
                request.getIsActive(),
                request.getAdmissionFromDate(),
                request.getAdmissionToDate(),
                pageRequest
        );

        return students.map(student -> {
            List<Guardian> guardians = guardianRepository.findByStudentIdAndIsActiveTrue(student.getId());
            return mapToResponse(student, guardians);
        });
    }

    @Transactional
    public void deleteStudent(Long studentId, UserPrincipal currentUser) {
        Student student = studentRepository.findByIdAndIsActiveTrue(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        // Soft delete
        student.setIsActive(false);
        student.setStudentStatus(Student.StudentStatus.INACTIVE);
        student.setUpdatedBy(currentUser.getId());
        student.setUpdatedAt(LocalDateTime.now());

        studentRepository.save(student);

        // Deactivate guardians
        List<Guardian> guardians = guardianRepository.findByStudentIdAndIsActiveTrue(studentId);
        guardians.forEach(guardian -> {
            guardian.setIsActive(false);
            guardianRepository.save(guardian);
        });

        log.info("Student deleted (soft): {}", student.getAdmissionNumber());
    }

    @Transactional
    public GuardianResponse addGuardian(Long studentId, CreateStudentRequest.GuardianInfo guardianInfo, UserPrincipal currentUser) {
        Student student = studentRepository.findByIdAndIsActiveTrue(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        Guardian guardian = createGuardian(student, guardianInfo);

        // Check if this should be primary contact
        if (guardianInfo.getIsPrimaryContact() != null && guardianInfo.getIsPrimaryContact()) {
            // Remove primary from others
            guardianRepository.findByStudentIdAndIsActiveTrue(studentId)
                    .forEach(g -> {
                        g.setIsPrimaryContact(false);
                        guardianRepository.save(g);
                    });
        }

        Guardian savedGuardian = guardianRepository.save(guardian);

        return mapGuardianToResponse(savedGuardian);
    }

    private String generateAdmissionNumber() {
        // Format: YYYY + 4-digit sequence
        int year = LocalDate.now().getYear();
        String prefix = String.valueOf(year);

        Integer maxNumber = studentRepository.findMaxAdmissionNumberByPrefix(prefix);
        int nextNumber = (maxNumber != null) ? maxNumber + 1 : 1;

        return String.format("%s%04d", prefix, nextNumber);
    }

    private String generateRollNumber(Long classId, Long sectionId) {
        // Format: ClassCode-SectionCode-Sequence
        long count = studentRepository.countActiveStudentsByClassId(classId) + 1;
        return String.format("%d-%d-%03d", classId, sectionId, count);
    }

    private Guardian createGuardian(Student student, CreateStudentRequest.GuardianInfo guardianInfo) {
        Guardian guardian = new Guardian();
        guardian.setStudent(student);
        guardian.setGuardianType(guardianInfo.getGuardianType());
        guardian.setFirstName(guardianInfo.getFirstName());
        guardian.setLastName(guardianInfo.getLastName());
        guardian.setRelationship(guardianInfo.getRelationship());
        guardian.setEmail(guardianInfo.getEmail());
        guardian.setPhone(guardianInfo.getPhone());
        guardian.setOccupation(guardianInfo.getOccupation());
        guardian.setAddress(guardianInfo.getAddress());
        guardian.setIsPrimaryContact(guardianInfo.getIsPrimaryContact() != null ?
                guardianInfo.getIsPrimaryContact() : false);
        guardian.setIsActive(true);

        return guardian;
    }

    private StudentResponse mapToResponse(Student student, List<Guardian> guardians) {
        StudentResponse response = StudentResponse.builder()
                .id(student.getId())
                .admissionNumber(student.getAdmissionNumber())
                .rollNumber(student.getRollNumber())
                .firstName(student.getFirstName())
                .lastName(student.getLastName())
                .dateOfBirth(student.getDateOfBirth())
                .gender(student.getGender())
                .email(student.getEmail())
                .phone(student.getPhone())
                .address(student.getAddress())
                .city(student.getCity())
                .state(student.getState())
                .postalCode(student.getPostalCode())
                .country(student.getCountry())
                .admissionDate(student.getAdmissionDate())
                .currentClassId(student.getCurrentClassId())
                .currentSectionId(student.getCurrentSectionId())
                .academicYearId(student.getAcademicYearId())
                .bloodGroup(student.getBloodGroup())
                .religion(student.getReligion())
                .category(student.getCategory())
                .nationality(student.getNationality())
                .motherTongue(student.getMotherTongue())
                .studentStatus(student.getStudentStatus())
                .profilePhotoUrl(student.getProfilePhotoUrl())
                .aadharNumber(student.getAadharNumber())
                .previousSchool(student.getPreviousSchool())
                .medicalConditions(student.getMedicalConditions())
                .emergencyContactName(student.getEmergencyContactName())
                .emergencyContactPhone(student.getEmergencyContactPhone())
                .createdAt(student.getCreatedAt())
                .updatedAt(student.getUpdatedAt())
                .isActive(student.getIsActive())
                .build();

        // Calculate age
        if (student.getDateOfBirth() != null) {
            response.setAge(Period.between(student.getDateOfBirth(), LocalDate.now()).getYears());
        }

        // Map guardians
        if (guardians != null && !guardians.isEmpty()) {
            List<GuardianResponse> guardianResponses = guardians.stream()
                    .map(this::mapGuardianToResponse)
                    .collect(Collectors.toList());
            response.setGuardians(guardianResponses);
        }

        return response;
    }

    private GuardianResponse mapGuardianToResponse(Guardian guardian) {
        return GuardianResponse.builder()
                .id(guardian.getId())
                .guardianType(guardian.getGuardianType())
                .firstName(guardian.getFirstName())
                .lastName(guardian.getLastName())
                .relationship(guardian.getRelationship())
                .email(guardian.getEmail())
                .phone(guardian.getPhone())
                .occupation(guardian.getOccupation())
                .annualIncome(guardian.getAnnualIncome())
                .address(guardian.getAddress())
                .officeAddress(guardian.getOfficeAddress())
                .officePhone(guardian.getOfficePhone())
                .isPrimaryContact(guardian.getIsPrimaryContact())
                .canPickupChild(guardian.getCanPickupChild())
                .photoUrl(guardian.getPhotoUrl())
                .build();
    }

    @Transactional(readOnly = true)
    public StudentResponse getStudentByAdmissionNumber(String admissionNumber) {
        Student student = studentRepository.findByAdmissionNumberAndIsActiveTrue(admissionNumber)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        List<Guardian> guardians = guardianRepository.findByStudentIdAndIsActiveTrue(student.getId());

        return mapToResponse(student, guardians);
    }

}

