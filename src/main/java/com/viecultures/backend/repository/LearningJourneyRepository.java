package com.viecultures.backend.repository;

import com.viecultures.backend.entity.LearningJourney;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface LearningJourneyRepository extends JpaRepository<LearningJourney, String>, JpaSpecificationExecutor<LearningJourney> {
    @Override
    @EntityGraph(attributePaths = {"chapters"})
    List<LearningJourney> findAll(@Nullable Specification<LearningJourney> spec);

    @Override
    @EntityGraph(attributePaths = {"chapters"})
    Page<LearningJourney> findAll(@Nullable Specification<LearningJourney> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"chapters"})
    Optional<LearningJourney> findById(String id);

    List<LearningJourney> findByRegionIgnoreCase(String region);
    List<LearningJourney> findByProvinceIdIgnoreCase(String provinceId);
    List<LearningJourney> findByThemeIgnoreCase(String theme);
    List<LearningJourney> findByStatusIgnoreCase(String status);
    List<LearningJourney> findByStatusIgnoreCaseAndScheduledPublishAtLessThanEqual(String status, LocalDateTime dateTime);
}
