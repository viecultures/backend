package com.viecultures.backend.service;

import com.viecultures.backend.dto.google.Ga4ReportResponse;
import com.viecultures.backend.dto.google.GscReportResponse;

public interface GoogleIntegrationService {

    /**
     * Lấy dữ liệu báo cáo Google Analytics 4 (GA4).
     *
     * @param range Khoảng thời gian báo cáo, ví dụ "7d", "14d", "30d", "90d"
     * @return Dữ liệu báo cáo GA4 tổng hợp
     */
    Ga4ReportResponse getGa4Report(String range);

    /**
     * Lấy dữ liệu báo cáo Google Search Console (GSC).
     *
     * @param range Khoảng thời gian báo cáo, ví dụ "7d", "28d", "90d"
     * @return Dữ liệu báo cáo GSC tổng hợp
     */
    GscReportResponse getGscReport(String range);
}
