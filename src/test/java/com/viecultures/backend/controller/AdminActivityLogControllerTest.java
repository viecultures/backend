package com.viecultures.backend.controller;

import com.viecultures.backend.modules.gamification.dto.ActivityLogResponse;
import com.viecultures.backend.modules.gamification.dto.ActivityStatsSummaryDto;
import com.viecultures.backend.modules.gamification.service.UserActivityLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminActivityLogControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserActivityLogService userActivityLogService;

    @InjectMocks
    private AdminActivityLogController adminActivityLogController;

    private ActivityLogResponse sampleLogResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminActivityLogController).build();

        sampleLogResponse = ActivityLogResponse.builder()
                .id("log-001")
                .userId("usr-123")
                .userEmail("learner@example.com")
                .anonymousSessionId(null)
                .eventType("ARTICLE_READ")
                .eventCategory("READER")
                .path("/reading/posts/post-abc")
                .payload("{\"readingTimeMs\": 120000}")
                .ipAddress("127.0.0.1")
                .userAgent("Mozilla/5.0")
                .createdAt(LocalDateTime.of(2026, 11, 2, 9, 30, 0))
                .build();
    }

    @Test
    @DisplayName("getLogs_withFullFilterParams_returns200AndPagedList: Kiem tra GET /api/v1/admin/activity-logs voi cac query params")
    void getLogs_withFullFilterParams_returns200AndPagedList() throws Exception {
        Page<ActivityLogResponse> mockPage = new PageImpl<>(List.of(sampleLogResponse), PageRequest.of(0, 15), 1);

        when(userActivityLogService.getLogs(
                eq("ARTICLE_READ"),
                eq("READER"),
                eq("usr-123"),
                eq("post-abc"),
                eq("2026-11-01"),
                eq("2026-11-02"),
                any(Pageable.class)
        )).thenReturn(mockPage);

        mockMvc.perform(get("/api/v1/admin/activity-logs")
                        .param("eventType", "ARTICLE_READ")
                        .param("eventCategory", "READER")
                        .param("userId", "usr-123")
                        .param("search", "post-abc")
                        .param("fromDate", "2026-11-01")
                        .param("toDate", "2026-11-02")
                        .param("page", "0")
                        .param("size", "15")
                        .param("sortBy", "createdAt")
                        .param("direction", "DESC")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Lay danh sach nhat ky hanh vi thanh cong"))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].id").value("log-001"))
                .andExpect(jsonPath("$.data.content[0].userId").value("usr-123"))
                .andExpect(jsonPath("$.data.content[0].userEmail").value("learner@example.com"))
                .andExpect(jsonPath("$.data.content[0].eventType").value("ARTICLE_READ"))
                .andExpect(jsonPath("$.data.content[0].eventCategory").value("READER"))
                .andExpect(jsonPath("$.data.content[0].path").value("/reading/posts/post-abc"));

        verify(userActivityLogService, times(1)).getLogs(
                eq("ARTICLE_READ"),
                eq("READER"),
                eq("usr-123"),
                eq("post-abc"),
                eq("2026-11-01"),
                eq("2026-11-02"),
                any(Pageable.class)
        );
    }

    @Test
    @DisplayName("getLogs_defaultParams_callsServiceWithDefaultPageable: Kiem tra goi voi tham so mac dinh")
    void getLogs_defaultParams_callsServiceWithDefaultPageable() throws Exception {
        Page<ActivityLogResponse> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);

        when(userActivityLogService.getLogs(
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)
        )).thenReturn(emptyPage);

        mockMvc.perform(get("/api/v1/admin/activity-logs")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").value(0));

        verify(userActivityLogService, times(1)).getLogs(
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)
        );
    }

    @Test
    @DisplayName("getLogs_serviceThrowsException_returns500: Khi service gap loi, tra ve HTTP 500 va QUERY_ERROR")
    void getLogs_serviceThrowsException_returns500() throws Exception {
        when(userActivityLogService.getLogs(any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenThrow(new RuntimeException("Simulated query timeout"));

        mockMvc.perform(get("/api/v1/admin/activity-logs")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("QUERY_ERROR"))
                .andExpect(jsonPath("$.message").value("Khong the tai nhat ky hanh vi"));

        verify(userActivityLogService, times(1))
                .getLogs(any(), any(), any(), any(), any(), any(), any(Pageable.class));
    }

    @Test
    @DisplayName("getStatsSummary_success: Tra ve HTTP 200 kem du lieu thong ke tong quan nhat ky")
    void getStatsSummary_success() throws Exception {
        Map<String, Long> categoryCounts = new HashMap<>();
        categoryCounts.put("READER", 350L);
        categoryCounts.put("VOCABULARY", 120L);

        Map<String, Long> eventTypeCounts = new HashMap<>();
        eventTypeCounts.put("ARTICLE_READ", 250L);
        eventTypeCounts.put("VOCAB_LOOKUP", 120L);

        ActivityStatsSummaryDto.TopMetricItemDto topPost = ActivityStatsSummaryDto.TopMetricItemDto.builder()
                .key("post-vn-01")
                .label("Van mieu Quoc Tu Giam")
                .count(85L)
                .build();

        ActivityStatsSummaryDto.TopMetricItemDto topWord = ActivityStatsSummaryDto.TopMetricItemDto.builder()
                .key("heritage")
                .label("heritage")
                .count(42L)
                .build();

        ActivityStatsSummaryDto statsDto = ActivityStatsSummaryDto.builder()
                .totalLogs(470L)
                .categoryCounts(categoryCounts)
                .eventTypeCounts(eventTypeCounts)
                .topReadPosts(List.of(topPost))
                .topLookedUpWords(List.of(topWord))
                .build();

        when(userActivityLogService.getStatsSummary()).thenReturn(statsDto);

        mockMvc.perform(get("/api/v1/admin/activity-logs/stats")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Lay thong ke nhat ky hanh vi thanh cong"))
                .andExpect(jsonPath("$.data.totalLogs").value(470))
                .andExpect(jsonPath("$.data.categoryCounts.READER").value(350))
                .andExpect(jsonPath("$.data.eventTypeCounts.ARTICLE_READ").value(250))
                .andExpect(jsonPath("$.data.topReadPosts[0].key").value("post-vn-01"))
                .andExpect(jsonPath("$.data.topReadPosts[0].label").value("Van mieu Quoc Tu Giam"))
                .andExpect(jsonPath("$.data.topLookedUpWords[0].key").value("heritage"))
                .andExpect(jsonPath("$.data.topLookedUpWords[0].count").value(42));

        verify(userActivityLogService, times(1)).getStatsSummary();
    }

    @Test
    @DisplayName("getStatsSummary_serviceThrowsException_returns500: Khi service thong ke gap loi, tra ve HTTP 500 va STATS_ERROR")
    void getStatsSummary_serviceThrowsException_returns500() throws Exception {
        when(userActivityLogService.getStatsSummary())
                .thenThrow(new RuntimeException("Database aggregation failure"));

        mockMvc.perform(get("/api/v1/admin/activity-logs/stats")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("STATS_ERROR"))
                .andExpect(jsonPath("$.message").value("Khong the tai thong ke nhat ky hanh vi"));

        verify(userActivityLogService, times(1)).getStatsSummary();
    }

    @Test
    @DisplayName("getLogs_legacyPath_returns200: Kiem tra legacy route /api/admin/activity-logs van hoat dong dong nhat")
    void getLogs_legacyPath_returns200() throws Exception {
        Page<ActivityLogResponse> mockPage = new PageImpl<>(List.of(sampleLogResponse), PageRequest.of(0, 15), 1);

        when(userActivityLogService.getLogs(any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(mockPage);

        mockMvc.perform(get("/api/admin/activity-logs")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value("log-001"));

        verify(userActivityLogService, times(1))
                .getLogs(any(), any(), any(), any(), any(), any(), any(Pageable.class));
    }
}
