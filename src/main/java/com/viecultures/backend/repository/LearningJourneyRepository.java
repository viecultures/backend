package com.viecultures.backend.repository;

import com.viecultures.backend.modules.learning.dto.EntityOptionDto;
import com.viecultures.backend.entity.LearningJourney;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface LearningJourneyRepository extends JpaRepository<LearningJourney, String>, JpaSpecificationExecutor<LearningJourney> {
    @Override
    @EntityGraph(attributePaths = {"chapters"})
    Optional<LearningJourney> findById(String id);

    @Query("SELECT new com.viecultures.backend.modules.learning.dto.EntityOptionDto(j.id, j.titleVi, j.titleEn) " +
           "FROM LearningJourney j ORDER BY j.titleVi ASC")
    List<EntityOptionDto> findAllJourneyOptions();

    List<LearningJourney> findByRegionIgnoreCase(String region);
    List<LearningJourney> findByProvinceIdIgnoreCase(String provinceId);
    List<LearningJourney> findByThemeIgnoreCase(String theme);
    List<LearningJourney> findByStatusIgnoreCase(String status);
    List<LearningJourney> findByStatusIgnoreCaseAndScheduledPublishAtLessThanEqual(String status, LocalDateTime dateTime);
}
