package com.viecultures.backend.service;

import com.viecultures.backend.dto.admin.AdminOverviewStatsDto;
import com.viecultures.backend.repository.LearningJourneyRepository;
import com.viecultures.backend.repository.ReadingPostRepository;
import com.viecultures.backend.repository.UserActivityLogRepository;
import com.viecultures.backend.repository.UserRepository;
import com.viecultures.backend.service.impl.AdminOverviewServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataRetrievalFailureException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminOverviewServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ReadingPostRepository readingPostRepository;

    @Mock
    private LearningJourneyRepository learningJourneyRepository;

    @Mock
    private UserActivityLogRepository userActivityLogRepository;

    @InjectMocks
    private AdminOverviewServiceImpl adminOverviewService;

    @Test
    @DisplayName("getOverviewStats_success: Tinh toan chinh xac cac chi so tong quan he thong")
    void getOverviewStats_success() {
        when(userRepository.count()).thenReturn(150L);
        when(userActivityLogRepository.countDistinctActiveUsersSince(any(LocalDateTime.class))).thenReturn(42L);
        when(readingPostRepository.count()).thenReturn(50L);
        when(readingPostRepository.sumTotalReads()).thenReturn(250L);

        List<Object[]> cefrCounts = new ArrayList<>();
        cefrCounts.add(new Object[]{"A1", 10L});
        cefrCounts.add(new Object[]{"B1", 40L});
        when(readingPostRepository.countGroupByLevel()).thenReturn(cefrCounts);

        List<Object[]> regionCounts = new ArrayList<>();
        regionCounts.add(new Object[]{"bac", 30L});
        regionCounts.add(new Object[]{"nam", 20L});
        when(readingPostRepository.countGroupByRegion()).thenReturn(regionCounts);

        AdminOverviewStatsDto result = adminOverviewService.getOverviewStats();

        assertNotNull(result);
        assertEquals(150L, result.getTotalUsers());
        assertEquals(42L, result.getActiveLearnersWeek());
        assertEquals(50L, result.getTotalPosts());
        assertEquals(50L, result.getPublishedPosts());
        assertEquals(250L, result.getTotalReads());
        assertEquals(5.0, result.getAvgReadsPerPost());
        assertEquals("HEALTHY", result.getSystemStatus());

        assertEquals(2, result.getCefrCoverage().size());
        assertEquals(10L, result.getCefrCoverage().get("A1"));
        assertEquals(40L, result.getCefrCoverage().get("B1"));

        assertEquals(2, result.getRegionCoverage().size());
        assertEquals(30L, result.getRegionCoverage().get("bac"));
        assertEquals(20L, result.getRegionCoverage().get("nam"));

        verify(userRepository, times(1)).count();
        verify(userActivityLogRepository, times(1)).countDistinctActiveUsersSince(any(LocalDateTime.class));
        verify(readingPostRepository, times(1)).count();
        verify(readingPostRepository, times(1)).sumTotalReads();
        verify(readingPostRepository, times(1)).countGroupByLevel();
        verify(readingPostRepository, times(1)).countGroupByRegion();
    }

    @Test
    @DisplayName("getOverviewStats_emptyDatabase: Khi chua co bai viet va user, avgReadsPerPost tra ve 0.0 tranh loi chia cho 0")
    void getOverviewStats_emptyDatabase() {
        when(userRepository.count()).thenReturn(0L);
        when(userActivityLogRepository.countDistinctActiveUsersSince(any(LocalDateTime.class))).thenReturn(0L);
        when(readingPostRepository.count()).thenReturn(0L);
        when(readingPostRepository.sumTotalReads()).thenReturn(null);
        when(readingPostRepository.countGroupByLevel()).thenReturn(Collections.emptyList());
        when(readingPostRepository.countGroupByRegion()).thenReturn(Collections.emptyList());

        AdminOverviewStatsDto result = adminOverviewService.getOverviewStats();

        assertNotNull(result);
        assertEquals(0L, result.getTotalUsers());
        assertEquals(0L, result.getActiveLearnersWeek());
        assertEquals(0L, result.getTotalPosts());
        assertEquals(0L, result.getPublishedPosts());
        assertEquals(0L, result.getTotalReads());
        assertEquals(0.0, result.getAvgReadsPerPost());
        assertEquals("HEALTHY", result.getSystemStatus());
        assertTrue(result.getCefrCoverage().isEmpty());
        assertTrue(result.getRegionCoverage().isEmpty());
    }

    @Test
    @DisplayName("getOverviewStats_preciseFloatingPoint: Ti le trung binh khong bi lam tron thanh 0 khi so luot doc nho")
    void getOverviewStats_preciseFloatingPoint() {
        when(userRepository.count()).thenReturn(10L);
        when(userActivityLogRepository.countDistinctActiveUsersSince(any(LocalDateTime.class))).thenReturn(2L);
        when(readingPostRepository.count()).thenReturn(38L);
        when(readingPostRepository.sumTotalReads()).thenReturn(1L);
        when(readingPostRepository.countGroupByLevel()).thenReturn(Collections.emptyList());
        when(readingPostRepository.countGroupByRegion()).thenReturn(Collections.emptyList());

        AdminOverviewStatsDto result = adminOverviewService.getOverviewStats();

        assertNotNull(result);
        assertEquals(38L, result.getTotalPosts());
        assertEquals(1L, result.getTotalReads());
        assertEquals(0.03, result.getAvgReadsPerPost());
    }

    @Test
    @DisplayName("getOverviewStats_cefrAndRegionCoverage: Map phan bo dung level CEFR A1-C2 va vung mien bac trung nam")
    void getOverviewStats_cefrAndRegionCoverage() {
        when(userRepository.count()).thenReturn(200L);
        when(userActivityLogRepository.countDistinctActiveUsersSince(any(LocalDateTime.class))).thenReturn(80L);
        when(readingPostRepository.count()).thenReturn(60L);
        when(readingPostRepository.sumTotalReads()).thenReturn(1200L);

        List<Object[]> cefrCounts = List.of(
                new Object[]{"A1", 10L},
                new Object[]{"A2", 15L},
                new Object[]{"B1", 12L},
                new Object[]{"B2", 10L},
                new Object[]{"C1", 8L},
                new Object[]{"C2", 5L}
        );
        when(readingPostRepository.countGroupByLevel()).thenReturn(cefrCounts);

        List<Object[]> regionCounts = List.of(
                new Object[]{"bac", 25L},
                new Object[]{"trung", 20L},
                new Object[]{"nam", 15L}
        );
        when(readingPostRepository.countGroupByRegion()).thenReturn(regionCounts);

        AdminOverviewStatsDto result = adminOverviewService.getOverviewStats();

        assertNotNull(result);
        assertEquals(6, result.getCefrCoverage().size());
        assertEquals(10L, result.getCefrCoverage().get("A1"));
        assertEquals(15L, result.getCefrCoverage().get("A2"));
        assertEquals(12L, result.getCefrCoverage().get("B1"));
        assertEquals(10L, result.getCefrCoverage().get("B2"));
        assertEquals(8L, result.getCefrCoverage().get("C1"));
        assertEquals(5L, result.getCefrCoverage().get("C2"));

        assertEquals(3, result.getRegionCoverage().size());
        assertEquals(25L, result.getRegionCoverage().get("bac"));
        assertEquals(20L, result.getRegionCoverage().get("trung"));
        assertEquals(15L, result.getRegionCoverage().get("nam"));
    }

    @Test
    @DisplayName("getOverviewStats_repositoryException_handledGracefully: Tra ve DTO fallback an toan voi systemStatus DEGRADED khi gap loi")
    void getOverviewStats_repositoryException_handledGracefully() {
        when(userRepository.count()).thenThrow(new DataRetrievalFailureException("Database cluster connection timeout"));

        AdminOverviewStatsDto result = adminOverviewService.getOverviewStats();

        assertNotNull(result);
        assertEquals(0L, result.getTotalUsers());
        assertEquals(0L, result.getActiveLearnersWeek());
        assertEquals(0L, result.getTotalPosts());
        assertEquals(0L, result.getPublishedPosts());
        assertEquals(0L, result.getTotalReads());
        assertEquals(0.0, result.getAvgReadsPerPost());
        assertEquals("DEGRADED", result.getSystemStatus());
        assertNotNull(result.getCefrCoverage());
        assertTrue(result.getCefrCoverage().isEmpty());
        assertNotNull(result.getRegionCoverage());
        assertTrue(result.getRegionCoverage().isEmpty());
    }

    @Test
    @DisplayName("getOverviewStats_activityLogFailure_handledGracefully: Khi userActivityLog bi loi van tra ve stats voi activeLearners = 0")
    void getOverviewStats_activityLogFailure_handledGracefully() {
        when(userRepository.count()).thenReturn(50L);
        when(userActivityLogRepository.countDistinctActiveUsersSince(any(LocalDateTime.class)))
                .thenThrow(new RuntimeException("Activity log table locked"));
        when(readingPostRepository.count()).thenReturn(20L);
        when(readingPostRepository.sumTotalReads()).thenReturn(100L);
        when(readingPostRepository.countGroupByLevel()).thenReturn(Collections.emptyList());
        when(readingPostRepository.countGroupByRegion()).thenReturn(Collections.emptyList());

        AdminOverviewStatsDto result = adminOverviewService.getOverviewStats();

        assertNotNull(result);
        assertEquals(50L, result.getTotalUsers());
        assertEquals(0L, result.getActiveLearnersWeek());
        assertEquals(20L, result.getTotalPosts());
        assertEquals(100L, result.getTotalReads());
        assertEquals(5.0, result.getAvgReadsPerPost());
        assertEquals("HEALTHY", result.getSystemStatus());
    }

    @Test
    @DisplayName("getOverviewStats_breakdownFailure_handledGracefully: Khi query CEFR hoac region bi loi, service van giu HEALTHY va tra ve map rong")
    void getOverviewStats_breakdownFailure_handledGracefully() {
        when(userRepository.count()).thenReturn(80L);
        when(userActivityLogRepository.countDistinctActiveUsersSince(any(LocalDateTime.class))).thenReturn(15L);
        when(readingPostRepository.count()).thenReturn(30L);
        when(readingPostRepository.sumTotalReads()).thenReturn(150L);
        when(readingPostRepository.countGroupByLevel()).thenThrow(new RuntimeException("CEFR aggregation syntax error"));
        when(readingPostRepository.countGroupByRegion()).thenThrow(new RuntimeException("Region aggregation syntax error"));

        AdminOverviewStatsDto result = adminOverviewService.getOverviewStats();

        assertNotNull(result);
        assertEquals(80L, result.getTotalUsers());
        assertEquals(15L, result.getActiveLearnersWeek());
        assertEquals(30L, result.getTotalPosts());
        assertEquals(150L, result.getTotalReads());
        assertEquals(5.0, result.getAvgReadsPerPost());
        assertEquals("HEALTHY", result.getSystemStatus());
        assertNotNull(result.getCefrCoverage());
        assertTrue(result.getCefrCoverage().isEmpty());
        assertNotNull(result.getRegionCoverage());
        assertTrue(result.getRegionCoverage().isEmpty());
    }
}