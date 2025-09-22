package com.erp.tenant.repository;

import com.erp.tenant.entity.StudentAcademicInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentAcademicInfoRepository extends JpaRepository<StudentAcademicInfo, Long> {

    Optional<StudentAcademicInfo> findByStudentId(Long studentId);
}
