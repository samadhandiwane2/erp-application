package com.erp.tenant.repository;

import com.erp.tenant.entity.StudentClassHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentClassHistoryRepository extends JpaRepository<StudentClassHistory, Long> {

    List<StudentClassHistory> findByStudentIdOrderByStartDateDesc(Long studentId);

    Optional<StudentClassHistory> findByStudentIdAndAcademicYearIdAndEndDateIsNull(Long studentId, Long academicYearId);

    boolean existsByStudentIdAndAcademicYearId(Long studentId, Long academicYearId);

    @Query("SELECT sch FROM StudentClassHistory sch WHERE sch.classId = :classId " +
            "AND sch.academicYearId = :academicYearId AND sch.promotionStatus = 'IN_PROGRESS'")
    List<StudentClassHistory> findCurrentStudentsByClass(@Param("classId") Long classId, @Param("academicYearId") Long academicYearId);

    @Query("SELECT sch FROM StudentClassHistory sch WHERE sch.classId = :classId " +
            "AND sch.sectionId = :sectionId AND sch.academicYearId = :academicYearId")
    List<StudentClassHistory> findByClassAndSection(@Param("classId") Long classId, @Param("sectionId") Long sectionId,
                                                    @Param("academicYearId") Long academicYearId);

}
