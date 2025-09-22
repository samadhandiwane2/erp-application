package com.erp.tenant.repository;

import com.erp.tenant.entity.Class;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClassRepository extends JpaRepository<Class, Long> {

    Optional<Class> findByClassCodeAndIsActiveTrue(String classCode);

    Optional<Class> findByIdAndIsActiveTrue(Long id);

    List<Class> findByIsActiveTrueOrderByGradeLevel();

    boolean existsByClassCodeAndIsActiveTrue(String classCode);

    @Query("SELECT c FROM Class c WHERE c.gradeLevel = :gradeLevel AND c.isActive = true")
    List<Class> findByGradeLevel(Integer gradeLevel);

    @Query("SELECT COUNT(s) FROM Student s WHERE s.currentClassId = :classId AND s.isActive = true")
    Long countActiveStudentsInClass(Long classId);

}
