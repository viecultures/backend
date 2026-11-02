package com.viecultures.backend.repository;

import com.viecultures.backend.entity.SystemAiSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SystemAiSettingRepository extends JpaRepository<SystemAiSetting, Long> {

    Optional<SystemAiSetting> findBySettingKey(String settingKey);

    boolean existsBySettingKey(String settingKey);
}
