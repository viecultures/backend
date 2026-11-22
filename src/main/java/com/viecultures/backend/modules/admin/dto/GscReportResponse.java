package com.viecultures.backend.modules.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GscReportResponse {

    private String siteUrl;
    private String startDate;
    private String endDate;
    private boolean liveData;
    private String statusMessage;

    @Builder.Default
    private GscSummary summary = new GscSummary();

    @Builder.Default
    private List<GscTimelinePoint> timeline = new ArrayList<>();

    @Builder.Default
    private List<GscQueryItem> topQueries = new ArrayList<>();

    @Builder.Default
    private List<GscPageItem> topPages = new ArrayList<>();

    @Builder.Default
    private List<GscDeviceItem> devices = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GscSummary {
        private long clicks;
        private long impressions;
        private double ctr;
        private double position;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GscTimelinePoint {
        private String date;
        private long clicks;
        private long impressions;
        private double ctr;
        private double position;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GscQueryItem {
        private String query;
        private long clicks;
        private long impressions;
        private double ctr;
        private double position;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GscPageItem {
        private String page;
        private long clicks;
        private long impressions;
        private double ctr;
        private double position;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GscDeviceItem {
        private String device;
        private long clicks;
        private long impressions;
        private double ctr;
        private double position;
    }
}
