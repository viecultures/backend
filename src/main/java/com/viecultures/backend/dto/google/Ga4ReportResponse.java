package com.viecultures.backend.dto.google;

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
public class Ga4ReportResponse {

    private String propertyId;
    private String dateRange;
    private boolean liveData;
    private String statusMessage;

    @Builder.Default
    private Ga4Summary summary = new Ga4Summary();

    @Builder.Default
    private List<Ga4TimelinePoint> timeline = new ArrayList<>();

    @Builder.Default
    private List<Ga4TopPage> topPages = new ArrayList<>();

    @Builder.Default
    private List<Ga4TrafficSource> trafficSources = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Ga4Summary {
        private long activeUsers;
        private long sessions;
        private long screenPageViews;
        private double averageSessionDuration;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Ga4TimelinePoint {
        private String date;
        private long activeUsers;
        private long sessions;
        private long screenPageViews;
        private double averageSessionDuration;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Ga4TopPage {
        private String pagePath;
        private String pageTitle;
        private long screenPageViews;
        private long activeUsers;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Ga4TrafficSource {
        private String source;
        private long sessions;
        private long activeUsers;
    }
}
