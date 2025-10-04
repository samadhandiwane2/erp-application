package com.erp.security.repository;

import com.erp.common.entity.UserPreferences;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserPreferencesRepository extends JpaRepository<UserPreferences, Long> {

    @Query(value = "SELECT * FROM erp_master.user_preferences WHERE user_id = :userId LIMIT 1", nativeQuery = true)
    Optional<UserPreferences> findByUserId(@Param("userId") Long userId);

    @Query(value = "SELECT EXISTS(SELECT 1 FROM erp_master.user_preferences WHERE user_id = :userId)", nativeQuery = true)
    boolean existsByUserId(@Param("userId") Long userId);

}