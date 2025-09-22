package com.erp.tenant.repository;

import com.erp.tenant.entity.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByAdmissionNumberAndIsActiveTrue(String admissionNumber);

    Optional<Student> findByIdAndIsActiveTrue(Long id);

    boolean existsByAdmissionNumber(String admissionNumber);

    boolean existsByAadharNumber(String aadharNumber);

    boolean existsByEmail(String email);

    List<Student> findByCurrentClassIdAndIsActiveTrue(Long classId);

    List<Student> findByCurrentClassIdAndCurrentSectionIdAndIsActiveTrue(Long classId, Long sectionId);

    @Query("""
            SELECT s FROM Student s 
            WHERE (:firstName IS NULL OR LOWER(s.firstName) LIKE LOWER(CONCAT('%', :firstName, '%')))
            AND (:lastName IS NULL OR LOWER(s.lastName) LIKE LOWER(CONCAT('%', :lastName, '%')))
            AND (:admissionNumber IS NULL OR s.admissionNumber = :admissionNumber)
            AND (:classId IS NULL OR s.currentClassId = :classId)
            AND (:sectionId IS NULL OR s.currentSectionId = :sectionId)
            AND (:status IS NULL OR s.studentStatus = :status)
            AND (:gender IS NULL OR s.gender = :gender)
            AND (:isActive IS NULL OR s.isActive = :isActive)
            AND (:fromDate IS NULL OR s.admissionDate >= :fromDate)
            AND (:toDate IS NULL OR s.admissionDate <= :toDate)
            """)
    Page<Student> searchStudents(@Param("firstName") String firstName,
                                 @Param("lastName") String lastName,
                                 @Param("admissionNumber") String admissionNumber,
                                 @Param("classId") Long classId,
                                 @Param("sectionId") Long sectionId,
                                 @Param("status") Student.StudentStatus status,
                                 @Param("gender") Student.Gender gender,
                                 @Param("isActive") Boolean isActive,
                                 @Param("fromDate") LocalDate fromDate,
                                 @Param("toDate") LocalDate toDate,
                                 Pageable pageable
    );

    @Query("SELECT COUNT(s) FROM Student s WHERE s.currentClassId = :classId AND s.isActive = true")
    long countActiveStudentsByClassId(@Param("classId") Long classId);

    @Query("SELECT COUNT(s) FROM Student s WHERE s.studentStatus = :status AND s.isActive = true")
    long countByStatus(@Param("status") Student.StudentStatus status);

    @Query("SELECT MAX(CAST(SUBSTRING(s.admissionNumber, 5) AS integer)) FROM Student s WHERE s.admissionNumber LIKE :prefix%")
    Integer findMaxAdmissionNumberByPrefix(@Param("prefix") String prefix);

}
