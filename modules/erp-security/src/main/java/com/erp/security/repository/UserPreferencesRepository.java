package com.erp.security.repository;

import com.erp.common.annotation.ForceMasterSchema;
import com.erp.common.entity.UserPreferences;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@ForceMasterSchema
public interface UserPreferencesRepository extends JpaRepository<UserPreferences, Long> {

    Optional<UserPreferences> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

}
