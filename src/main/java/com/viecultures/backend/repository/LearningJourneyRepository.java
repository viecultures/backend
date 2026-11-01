package com.viecultures.backend.repository;

import com.viecultures.backend.entity.LearningJourney;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LearningJourneyRepository extends JpaRepository<LearningJourney, String>, JpaSpecificationExecutor<LearningJourney> {
    List<LearningJourney> findByRegionIgnoreCase(String region);
    List<LearningJourney> findByProvinceIdIgnoreCase(String provinceId);
    List<LearningJourney> findByThemeIgnoreCase(String theme);
    List<LearningJourney> findByStatusIgnoreCase(String status);
}
