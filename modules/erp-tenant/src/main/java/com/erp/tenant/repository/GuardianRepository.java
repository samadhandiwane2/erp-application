package com.erp.tenant.repository;

import com.erp.tenant.entity.Guardian;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GuardianRepository extends JpaRepository<Guardian, Long> {

    List<Guardian> findByStudentIdAndIsActiveTrue(Long studentId);

    Optional<Guardian> findByStudentIdAndIsPrimaryContactTrueAndIsActiveTrue(Long studentId);

    List<Guardian> findByPhoneAndIsActiveTrue(String phone);

    List<Guardian> findByEmailAndIsActiveTrue(String email);

    @Query("SELECT g FROM Guardian g WHERE g.student.id = :studentId AND g.guardianType = :type AND g.isActive = true")
    Optional<Guardian> findByStudentIdAndGuardianType(@Param("studentId") Long studentId,
                                                      @Param("type") Guardian.GuardianType type
    );

    @Query("SELECT COUNT(g) FROM Guardian g WHERE g.student.id = :studentId AND g.isActive = true")
    long countActiveGuardiansByStudentId(@Param("studentId") Long studentId);

    boolean existsByAadharNumberAndIsActiveTrue(String aadharNumber);

}
