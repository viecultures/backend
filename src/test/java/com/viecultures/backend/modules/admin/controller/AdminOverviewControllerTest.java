package com.viecultures.backend.modules.admin.controller;

import com.viecultures.backend.modules.admin.dto.AdminOverviewStatsDto;
import com.viecultures.backend.modules.admin.service.AdminOverviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminOverviewControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AdminOverviewService adminOverviewService;

    @InjectMocks
    private AdminOverviewController adminOverviewController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminOverviewController).build();
    }

    @Test
    @DisplayName("getOverview_returns200AndValidApiResponse: Tra ve HTTP 200 va data khong bi null")
    void getOverview_returns200AndValidApiResponse() throws Exception {
        Map<String, Long> cefrCoverage = new LinkedHashMap<>();
        cefrCoverage.put("A1", 10L);
        cefrCoverage.put("B1", 20L);

        Map<String, Long> regionCoverage = new LinkedHashMap<>();
        regionCoverage.put("bac", 15L);
        regionCoverage.put("nam", 15L);

        AdminOverviewStatsDto mockStats = AdminOverviewStatsDto.builder()
                .totalUsers(100L)
                .activeLearnersWeek(30L)
                .totalPosts(50L)
                .publishedPosts(50L)
                .totalReads(200L)
                .avgReadsPerPost(4.0)
                .cefrCoverage(cefrCoverage)
                .regionCoverage(regionCoverage)
                .systemStatus("HEALTHY")
                .build();

        when(adminOverviewService.getOverviewStats()).thenReturn(mockStats);

        mockMvc.perform(get("/api/v1/admin/overview")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isMap())
                .andExpect(jsonPath("$.data.totalUsers").value(100))
                .andExpect(jsonPath("$.data.activeLearnersWeek").value(30))
                .andExpect(jsonPath("$.data.totalPosts").value(50))
                .andExpect(jsonPath("$.data.publishedPosts").value(50))
                .andExpect(jsonPath("$.data.totalReads").value(200))
                .andExpect(jsonPath("$.data.avgReadsPerPost").value(4.0))
                .andExpect(jsonPath("$.data.systemStatus").value("HEALTHY"))
                .andExpect(jsonPath("$.data.cefrCoverage.A1").value(10))
                .andExpect(jsonPath("$.data.regionCoverage.bac").value(15));

        verify(adminOverviewService, times(1)).getOverviewStats();
    }

    @Test
    @DisplayName("getOverview_serviceThrowsException_returnsErrorResponse: Tra ve HTTP 500 khi service gap loi")
    void getOverview_serviceThrowsException_returnsErrorResponse() throws Exception {
        when(adminOverviewService.getOverviewStats()).thenThrow(new RuntimeException("Critical database failure"));

        mockMvc.perform(get("/api/v1/admin/overview")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.message").exists());

        verify(adminOverviewService, times(1)).getOverviewStats();
    }
}