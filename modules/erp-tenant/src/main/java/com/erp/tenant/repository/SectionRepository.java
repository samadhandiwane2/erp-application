package com.erp.tenant.repository;

import com.erp.tenant.entity.Section;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SectionRepository extends JpaRepository<Section, Long> {

    List<Section> findByClassEntityIdAndIsActiveTrue(Long classId);

    Optional<Section> findByIdAndIsActiveTrue(Long id);

    @Query("SELECT s FROM Section s WHERE s.classEntity.id = :classId AND s.sectionCode = :sectionCode AND s.isActive = true")
    Optional<Section> findByClassIdAndSectionCode(@Param("classId") Long classId, @Param("sectionCode") String sectionCode);

    @Query("SELECT s FROM Section s WHERE s.classEntity.id = :classId AND s.isActive = true ORDER BY s.sectionCode")
    List<Section> findByClassIdOrderBySectionCode(@Param("classId") Long classId);

    @Query("SELECT COUNT(st) FROM Student st WHERE st.currentClassId = :classId AND st.currentSectionId = :sectionId AND st.isActive = true")
    Long countActiveStudentsInSection(@Param("classId") Long classId, @Param("sectionId") Long sectionId);

    boolean existsByClassEntityIdAndSectionCodeAndIsActiveTrue(Long classId, String sectionCode);

}
