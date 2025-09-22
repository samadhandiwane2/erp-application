package com.erp.tenant.repository;

import com.erp.tenant.entity.StudentPromotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface StudentPromotionRepository extends JpaRepository<StudentPromotion, Long> {

    List<StudentPromotion> findByStudentIdOrderByPromotionDateDesc(Long studentId);

    List<StudentPromotion> findByAcademicYearId(Long academicYearId);

    @Query("SELECT sp FROM StudentPromotion sp WHERE sp.fromClassId = :classId " +
            "AND sp.academicYearId = :academicYearId")
    List<StudentPromotion> findByClassAndAcademicYear(@Param("classId") Long classId, @Param("academicYearId") Long academicYearId);

    @Query("SELECT sp FROM StudentPromotion sp WHERE sp.promotionDate BETWEEN :startDate AND :endDate")
    List<StudentPromotion> findByPromotionDateBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(sp) FROM StudentPromotion sp WHERE sp.fromClassId = :classId " +
            "AND sp.academicYearId = :academicYearId AND sp.promotionType = :type")
    long countByClassAndType(@Param("classId") Long classId, @Param("academicYearId") Long academicYearId,
                             @Param("type") StudentPromotion.PromotionType type);

}
