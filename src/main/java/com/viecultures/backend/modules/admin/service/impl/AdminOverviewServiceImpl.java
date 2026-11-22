package com.viecultures.backend.modules.admin.service.impl;

import com.viecultures.backend.modules.admin.dto.AdminOverviewStatsDto;
import com.viecultures.backend.repository.LearningJourneyRepository;
import com.viecultures.backend.repository.ReadingPostRepository;
import com.viecultures.backend.repository.UserActivityLogRepository;
import com.viecultures.backend.repository.UserRepository;
import com.viecultures.backend.modules.admin.service.AdminOverviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminOverviewServiceImpl implements AdminOverviewService {

    private final UserRepository userRepository;
    private final ReadingPostRepository readingPostRepository;
    private final LearningJourneyRepository learningJourneyRepository;
    private final UserActivityLogRepository userActivityLogRepository;

    @Override
    @Transactional(readOnly = true)
    public AdminOverviewStatsDto getOverviewStats() {
        log.info("Calculating system overview statistics for Admin CMS");
        try {
            long totalUsers = userRepository.count();

            LocalDateTime oneWeekAgo = LocalDateTime.now().minusDays(7);
            long activeLearnersWeek;
            try {
                activeLearnersWeek = userActivityLogRepository.countDistinctActiveUsersSince(oneWeekAgo);
            } catch (Exception ex) {
                log.error("Failed to query active learners in the past 7 days: {}", ex.getMessage(), ex);
                activeLearnersWeek = 0;
            }

            long totalPosts = readingPostRepository.count();
            long publishedPosts = totalPosts;

            Long sumReadsNullable = readingPostRepository.sumTotalReads();
            long totalReads = sumReadsNullable != null ? sumReadsNullable : 0L;

            double avgReadsPerPost = 0.0;
            if (totalPosts > 0) {
                double rawAvg = (double) totalReads / (double) totalPosts;
                avgReadsPerPost = Math.round(rawAvg * 100.0) / 100.0;
            }

            Map<String, Long> cefrCoverage = new LinkedHashMap<>();
            try {
                List<Object[]> cefrCounts = readingPostRepository.countGroupByLevel();
                for (Object[] row : cefrCounts) {
                    if (row != null && row.length >= 2 && row[0] != null) {
                        String levelKey = row[0].toString();
                        long count = ((Number) row[1]).longValue();
                        cefrCoverage.put(levelKey, count);
                    }
                }
            } catch (Exception ex) {
                log.error("Error computing CEFR coverage breakdown: {}", ex.getMessage(), ex);
            }

            Map<String, Long> regionCoverage = new LinkedHashMap<>();
            try {
                List<Object[]> regionCounts = readingPostRepository.countGroupByRegion();
                for (Object[] row : regionCounts) {
                    if (row != null && row.length >= 2 && row[0] != null) {
                        String regionKey = row[0].toString();
                        long count = ((Number) row[1]).longValue();
                        regionCoverage.put(regionKey, count);
                    }
                }
            } catch (Exception ex) {
                log.error("Error computing region coverage breakdown: {}", ex.getMessage(), ex);
            }

            String systemStatus = "HEALTHY";

            return AdminOverviewStatsDto.builder()
                    .totalUsers(totalUsers)
                    .activeLearnersWeek(activeLearnersWeek)
                    .totalPosts(totalPosts)
                    .publishedPosts(publishedPosts)
                    .totalReads(totalReads)
                    .avgReadsPerPost(avgReadsPerPost)
                    .cefrCoverage(cefrCoverage)
                    .regionCoverage(regionCoverage)
                    .systemStatus(systemStatus)
                    .build();

        } catch (Exception e) {
            log.error("Critical failure during getOverviewStats aggregation: {}", e.getMessage(), e);
            return AdminOverviewStatsDto.builder()
                    .totalUsers(0)
                    .activeLearnersWeek(0)
                    .totalPosts(0)
                    .publishedPosts(0)
                    .totalReads(0)
                    .avgReadsPerPost(0.0)
                    .cefrCoverage(new LinkedHashMap<>())
                    .regionCoverage(new LinkedHashMap<>())
                    .systemStatus("DEGRADED")
                    .build();
        }
    }
}
