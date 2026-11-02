package com.viecultures.backend.repository;

import com.viecultures.backend.entity.AiUsageLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AiUsageLogRepository extends JpaRepository<AiUsageLog, Long> {

    Page<AiUsageLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<AiUsageLog> findByProviderOrderByCreatedAtDesc(String provider, Pageable pageable);

    Page<AiUsageLog> findByFeatureCodeOrderByCreatedAtDesc(String featureCode, Pageable pageable);

    Page<AiUsageLog> findByProviderAndFeatureCodeOrderByCreatedAtDesc(String provider, String featureCode, Pageable pageable);

    @Query("SELECT COALESCE(SUM(l.estimatedCostUsd), 0) FROM AiUsageLog l")
    BigDecimal calculateTotalCost();

    @Query("SELECT COALESCE(SUM(l.estimatedCostUsd), 0) FROM AiUsageLog l WHERE l.createdAt >= :since")
    BigDecimal calculateTotalCostSince(@Param("since") LocalDateTime since);

    @Query("SELECT COUNT(l) FROM AiUsageLog l WHERE l.createdAt >= :since")
    long countRequestsSince(@Param("since") LocalDateTime since);

    @Query("SELECT COALESCE(SUM(l.promptTokens + l.completionTokens), 0) FROM AiUsageLog l")
    long calculateTotalTokens();

    @Query("SELECT COALESCE(SUM(l.characterCount), 0) FROM AiUsageLog l")
    long calculateTotalCharacters();

    @Query("SELECT l.provider, COALESCE(SUM(l.estimatedCostUsd), 0), COUNT(l), COALESCE(SUM(l.promptTokens + l.completionTokens), 0), COALESCE(SUM(l.characterCount), 0) FROM AiUsageLog l GROUP BY l.provider")
    List<Object[]> getCostSummaryByProvider();

    @Query("SELECT l.featureCode, COALESCE(SUM(l.estimatedCostUsd), 0), COUNT(l) FROM AiUsageLog l GROUP BY l.featureCode")
    List<Object[]> getCostSummaryByFeature();
}
