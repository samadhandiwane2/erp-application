package com.erp.tenant.repository;

import com.erp.tenant.entity.AcademicYear;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AcademicYearRepository extends JpaRepository<AcademicYear, Long> {

    Optional<AcademicYear> findByYearNameAndIsActiveTrue(String yearName);

    Optional<AcademicYear> findByIsCurrentTrue();

    List<AcademicYear> findByIsActiveTrueOrderByStartDateDesc();

    @Query("SELECT ay FROM AcademicYear ay WHERE :date BETWEEN ay.startDate AND ay.endDate AND ay.isActive = true")
    Optional<AcademicYear> findByDate(LocalDate date);

    boolean existsByYearNameAndIsActiveTrue(String yearName);

}
