package com.viecultures.backend.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.viecultures.backend.dto.google.Ga4ReportResponse;
import com.viecultures.backend.dto.google.GscReportResponse;
import com.viecultures.backend.service.GoogleIntegrationService;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleIntegrationServiceImpl implements GoogleIntegrationService {

    private final ObjectMapper objectMapper;

    @Value("${google.integration.credentials-path:service-account.json}")
    private String credentialsPath;

    @Value("${google.integration.ga4-property-id:561849201}")
    private String ga4PropertyId;

    @Value("${google.integration.gsc-site-url:sc-domain:viecultures.app}")
    private String gscSiteUrl;

    private static final String GOOGLE_TOKEN_URI = "https://oauth2.googleapis.com/token";
    private static final String GA4_DATA_URL_TEMPLATE = "https://analyticsdata.googleapis.com/v1beta/properties/%s:runReport";
    private static final String GSC_QUERY_URL_TEMPLATE = "https://www.googleapis.com/webmasters/v3/sites/%s/searchAnalytics/query";
    private static final String OAUTH_SCOPES = "https://www.googleapis.com/auth/analytics.readonly https://www.googleapis.com/auth/webmasters.readonly";

    private final RestTemplate restTemplate = createCustomRestTemplate();
    private final ReentrantLock tokenLock = new ReentrantLock();

    private volatile String cachedAccessToken = null;
    private volatile Instant tokenExpiresAt = Instant.MIN;

    private static RestTemplate createCustomRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(15000);
        return new RestTemplate(factory);
    }

    /**
     * Lấy OAuth2 Access Token từ Google sử dụng JWT ký RSA-SHA256 với cơ chế cache trong bộ nhớ.
     */
    public String getOAuth2AccessToken() {
        if (cachedAccessToken != null && Instant.now().isBefore(tokenExpiresAt.minusSeconds(90))) {
            return cachedAccessToken;
        }

        tokenLock.lock();
        try {
            if (cachedAccessToken != null && Instant.now().isBefore(tokenExpiresAt.minusSeconds(90))) {
                return cachedAccessToken;
            }

            JsonNode serviceAccountNode = loadServiceAccountCredentials();
            if (serviceAccountNode == null) {
                log.info("Không tìm thấy cấu hình service-account.json hợp lệ. Google Service sẽ chuyển sang chế độ fallback an toàn.");
                return null;
            }

            String clientEmail = serviceAccountNode.path("client_email").asText();
            String privateKeyPem = serviceAccountNode.path("private_key").asText();

            if (clientEmail.isBlank() || privateKeyPem.isBlank()) {
                log.warn("service-account.json thiếu client_email hoặc private_key. Kích hoạt fallback.");
                return null;
            }

            PrivateKey privateKey = parsePrivateKey(privateKeyPem);
            Instant now = Instant.now();
            Instant expiration = now.plusSeconds(3600);

            String signedJwt = Jwts.builder()
                    .header()
                    .add("alg", "RS256")
                    .add("typ", "JWT")
                    .and()
                    .issuer(clientEmail)
                    .claim("scope", OAUTH_SCOPES)
                    .audience()
                    .single(GOOGLE_TOKEN_URI)
                    .issuedAt(Date.from(now))
                    .expiration(Date.from(expiration))
                    .signWith(privateKey, Jwts.SIG.RS256)
                    .compact();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer");
            body.add("assertion", signedJwt);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(GOOGLE_TOKEN_URI, request, JsonNode.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode respBody = response.getBody();
                String accessToken = respBody.path("access_token").asText();
                long expiresIn = respBody.path("expires_in").asLong(3600);

                if (!accessToken.isBlank()) {
                    cachedAccessToken = accessToken;
                    tokenExpiresAt = Instant.now().plusSeconds(expiresIn);
                    log.info("Lấy Google OAuth2 token thành công, hết hạn sau {} giây", expiresIn);
                    return cachedAccessToken;
                }
            }

            log.warn("Phản hồi từ Google OAuth2 token endpoint không chứa access_token hợp lệ");
            return null;
        } catch (Exception e) {
            log.warn("Không thể cấp phát Google OAuth2 token ({}), chuyển sang chế độ fallback: {}", e.getClass().getSimpleName(), e.getMessage());
            return null;
        } finally {
            tokenLock.unlock();
        }
    }

    @Override
    public Ga4ReportResponse getGa4Report(String range) {
        String safeRange = normalizeRange(range, "30d");
        String startDateParam = safeRange.replaceAll("[^0-9]", "") + "daysAgo";

        try {
            String token = getOAuth2AccessToken();
            if (token == null) {
                return buildGa4FallbackResponse(safeRange, "Chưa cấu hình Google Service Account credentials (service-account.json)");
            }
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            String url = String.format(GA4_DATA_URL_TEMPLATE, ga4PropertyId);

            // 1. Lấy Summary
            Ga4ReportResponse.Ga4Summary summary = fetchGa4Summary(url, headers, startDateParam);

            // 2. Lấy Timeline theo ngày
            List<Ga4ReportResponse.Ga4TimelinePoint> timeline = fetchGa4Timeline(url, headers, startDateParam);

            // 3. Lấy Top Pages
            List<Ga4ReportResponse.Ga4TopPage> topPages = fetchGa4TopPages(url, headers, startDateParam);

            // 4. Lấy Traffic Sources
            List<Ga4ReportResponse.Ga4TrafficSource> trafficSources = fetchGa4TrafficSources(url, headers, startDateParam);

            return Ga4ReportResponse.builder()
                    .propertyId(ga4PropertyId)
                    .dateRange(safeRange)
                    .liveData(true)
                    .statusMessage("Dữ liệu trực tiếp đồng bộ từ Google Analytics 4")
                    .summary(summary)
                    .timeline(timeline)
                    .topPages(topPages)
                    .trafficSources(trafficSources)
                    .build();

        } catch (Exception e) {
            log.warn("Lỗi truy vấn Google Analytics 4 API, kích hoạt fallback an toàn: {}", e.getMessage());
            return buildGa4FallbackResponse(safeRange, e.getMessage());
        }
    }

    @Override
    public GscReportResponse getGscReport(String range) {
        String safeRange = normalizeRange(range, "28d");
        int days = parseDaysFromRange(safeRange, 28);

        LocalDate endDate = LocalDate.now().minusDays(2);
        LocalDate startDate = endDate.minusDays(Math.max(1, days - 1));
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String startDateStr = startDate.format(dtf);
        String endDateStr = endDate.format(dtf);

        try {
            String token = getOAuth2AccessToken();
            if (token == null) {
                return buildGscFallbackResponse(startDateStr, endDateStr, safeRange, "Chưa cấu hình Google Service Account credentials (service-account.json)");
            }
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            String encodedSiteUrl = URLEncoder.encode(gscSiteUrl, StandardCharsets.UTF_8);
            URI uri = URI.create(String.format(GSC_QUERY_URL_TEMPLATE, encodedSiteUrl));

            // 1. Lấy Timeline theo ngày
            List<GscReportResponse.GscTimelinePoint> timeline = fetchGscTimeline(uri, headers, startDateStr, endDateStr);

            // 2. Tính toán Summary
            GscReportResponse.GscSummary summary = calculateGscSummary(timeline);

            // 3. Lấy Top Queries
            List<GscReportResponse.GscQueryItem> topQueries = fetchGscTopQueries(uri, headers, startDateStr, endDateStr);

            // 4. Lấy Top Pages
            List<GscReportResponse.GscPageItem> topPages = fetchGscTopPages(uri, headers, startDateStr, endDateStr);

            // 5. Lấy Devices
            List<GscReportResponse.GscDeviceItem> devices = fetchGscDevices(uri, headers, startDateStr, endDateStr);

            return GscReportResponse.builder()
                    .siteUrl(gscSiteUrl)
                    .startDate(startDateStr)
                    .endDate(endDateStr)
                    .liveData(true)
                    .statusMessage("Dữ liệu trực tiếp đồng bộ từ Google Search Console")
                    .summary(summary)
                    .timeline(timeline)
                    .topQueries(topQueries)
                    .topPages(topPages)
                    .devices(devices)
                    .build();

        } catch (Exception e) {
            log.warn("Lỗi truy vấn Google Search Console API, kích hoạt fallback an toàn: {}", e.getMessage());
            return buildGscFallbackResponse(startDateStr, endDateStr, safeRange, e.getMessage());
        }
    }

    private Ga4ReportResponse.Ga4Summary fetchGa4Summary(String url, HttpHeaders headers, String startDateParam) {
        Map<String, Object> reqBody = new HashMap<>();
        reqBody.put("dateRanges", List.of(Map.of("startDate", startDateParam, "endDate", "today")));
        reqBody.put("metrics", List.of(
                Map.of("name", "activeUsers"),
                Map.of("name", "sessions"),
                Map.of("name", "screenPageViews"),
                Map.of("name", "averageSessionDuration")
        ));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(reqBody, headers);
        ResponseEntity<JsonNode> response = restTemplate.postForEntity(url, entity, JsonNode.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            JsonNode rows = response.getBody().path("rows");
            if (rows.isArray() && !rows.isEmpty()) {
                JsonNode metricValues = rows.get(0).path("metricValues");
                long activeUsers = parseLongSafe(metricValues, 0);
                long sessions = parseLongSafe(metricValues, 1);
                long screenPageViews = parseLongSafe(metricValues, 2);
                double avgDuration = parseDoubleSafe(metricValues, 3);

                return Ga4ReportResponse.Ga4Summary.builder()
                        .activeUsers(activeUsers)
                        .sessions(sessions)
                        .screenPageViews(screenPageViews)
                        .averageSessionDuration(Math.round(avgDuration * 100.0) / 100.0)
                        .build();
            }
        }

        return Ga4ReportResponse.Ga4Summary.builder()
                .activeUsers(0)
                .sessions(0)
                .screenPageViews(0)
                .averageSessionDuration(0.0)
                .build();
    }

    private List<Ga4ReportResponse.Ga4TimelinePoint> fetchGa4Timeline(String url, HttpHeaders headers, String startDateParam) {
        Map<String, Object> reqBody = new HashMap<>();
        reqBody.put("dateRanges", List.of(Map.of("startDate", startDateParam, "endDate", "today")));
        reqBody.put("dimensions", List.of(Map.of("name", "date")));
        reqBody.put("metrics", List.of(
                Map.of("name", "activeUsers"),
                Map.of("name", "sessions"),
                Map.of("name", "screenPageViews"),
                Map.of("name", "averageSessionDuration")
        ));
        reqBody.put("orderBys", List.of(Map.of("dimension", Map.of("dimensionName", "date", "orderType", "ALPHANUMERIC"))));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(reqBody, headers);
        ResponseEntity<JsonNode> response = restTemplate.postForEntity(url, entity, JsonNode.class);

        List<Ga4ReportResponse.Ga4TimelinePoint> timeline = new ArrayList<>();
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            JsonNode rows = response.getBody().path("rows");
            if (rows.isArray()) {
                for (JsonNode row : rows) {
                    JsonNode dims = row.path("dimensionValues");
                    JsonNode metrics = row.path("metricValues");
                    String rawDate = dims.path(0).path("value").asText("");
                    String formattedDate = formatDateString(rawDate);

                    timeline.add(Ga4ReportResponse.Ga4TimelinePoint.builder()
                            .date(formattedDate)
                            .activeUsers(parseLongSafe(metrics, 0))
                            .sessions(parseLongSafe(metrics, 1))
                            .screenPageViews(parseLongSafe(metrics, 2))
                            .averageSessionDuration(Math.round(parseDoubleSafe(metrics, 3) * 100.0) / 100.0)
                            .build());
                }
            }
        }

        return timeline;
    }

    private List<Ga4ReportResponse.Ga4TopPage> fetchGa4TopPages(String url, HttpHeaders headers, String startDateParam) {
        Map<String, Object> reqBody = new HashMap<>();
        reqBody.put("dateRanges", List.of(Map.of("startDate", startDateParam, "endDate", "today")));
        reqBody.put("dimensions", List.of(
                Map.of("name", "pagePath"),
                Map.of("name", "pageTitle")
        ));
        reqBody.put("metrics", List.of(
                Map.of("name", "screenPageViews"),
                Map.of("name", "activeUsers")
        ));
        reqBody.put("orderBys", List.of(Map.of("metric", Map.of("metricName", "screenPageViews"), "desc", true)));
        reqBody.put("limit", 10);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(reqBody, headers);
        ResponseEntity<JsonNode> response = restTemplate.postForEntity(url, entity, JsonNode.class);

        List<Ga4ReportResponse.Ga4TopPage> list = new ArrayList<>();
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            JsonNode rows = response.getBody().path("rows");
            if (rows.isArray()) {
                for (JsonNode row : rows) {
                    JsonNode dims = row.path("dimensionValues");
                    JsonNode metrics = row.path("metricValues");
                    list.add(Ga4ReportResponse.Ga4TopPage.builder()
                            .pagePath(dims.path(0).path("value").asText("/"))
                            .pageTitle(dims.path(1).path("value").asText(""))
                            .screenPageViews(parseLongSafe(metrics, 0))
                            .activeUsers(parseLongSafe(metrics, 1))
                            .build());
                }
            }
        }
        return list;
    }

    private List<Ga4ReportResponse.Ga4TrafficSource> fetchGa4TrafficSources(String url, HttpHeaders headers, String startDateParam) {
        Map<String, Object> reqBody = new HashMap<>();
        reqBody.put("dateRanges", List.of(Map.of("startDate", startDateParam, "endDate", "today")));
        reqBody.put("dimensions", List.of(Map.of("name", "sessionSource")));
        reqBody.put("metrics", List.of(
                Map.of("name", "sessions"),
                Map.of("name", "activeUsers")
        ));
        reqBody.put("orderBys", List.of(Map.of("metric", Map.of("metricName", "sessions"), "desc", true)));
        reqBody.put("limit", 10);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(reqBody, headers);
        ResponseEntity<JsonNode> response = restTemplate.postForEntity(url, entity, JsonNode.class);

        List<Ga4ReportResponse.Ga4TrafficSource> list = new ArrayList<>();
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            JsonNode rows = response.getBody().path("rows");
            if (rows.isArray()) {
                for (JsonNode row : rows) {
                    JsonNode dims = row.path("dimensionValues");
                    JsonNode metrics = row.path("metricValues");
                    list.add(Ga4ReportResponse.Ga4TrafficSource.builder()
                            .source(dims.path(0).path("value").asText("(direct)"))
                            .sessions(parseLongSafe(metrics, 0))
                            .activeUsers(parseLongSafe(metrics, 1))
                            .build());
                }
            }
        }
        return list;
    }

    private List<GscReportResponse.GscTimelinePoint> fetchGscTimeline(URI uri, HttpHeaders headers, String startDate, String endDate) {
        Map<String, Object> reqBody = new HashMap<>();
        reqBody.put("startDate", startDate);
        reqBody.put("endDate", endDate);
        reqBody.put("dimensions", List.of("date"));
        reqBody.put("rowLimit", 1000);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(reqBody, headers);
        ResponseEntity<JsonNode> response = restTemplate.postForEntity(uri, entity, JsonNode.class);

        Map<String, GscReportResponse.GscTimelinePoint> rowMap = new HashMap<>();
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            JsonNode rows = response.getBody().path("rows");
            if (rows.isArray()) {
                for (JsonNode row : rows) {
                    String date = row.path("keys").path(0).asText("");
                    long clicks = row.path("clicks").asLong(0);
                    long impressions = row.path("impressions").asLong(0);
                    double ctr = Math.round(row.path("ctr").asDouble(0.0) * 10000.0) / 100.0;
                    double position = Math.round(row.path("position").asDouble(0.0) * 10.0) / 10.0;

                    rowMap.put(date, GscReportResponse.GscTimelinePoint.builder()
                            .date(date)
                            .clicks(clicks)
                            .impressions(impressions)
                            .ctr(ctr)
                            .position(position)
                            .build());
                }
            }
        }

        List<GscReportResponse.GscTimelinePoint> points = new ArrayList<>();
        LocalDate cur = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        while (!cur.isAfter(end)) {
            String dateKey = cur.format(dtf);
            if (rowMap.containsKey(dateKey)) {
                points.add(rowMap.get(dateKey));
            } else {
                points.add(GscReportResponse.GscTimelinePoint.builder()
                        .date(dateKey)
                        .clicks(0)
                        .impressions(0)
                        .ctr(0.0)
                        .position(0.0)
                        .build());
            }
            cur = cur.plusDays(1);
        }
        return points;
    }

    private GscReportResponse.GscSummary calculateGscSummary(List<GscReportResponse.GscTimelinePoint> timeline) {
        if (timeline == null || timeline.isEmpty()) {
            return GscReportResponse.GscSummary.builder()
                    .clicks(0)
                    .impressions(0)
                    .ctr(0.0)
                    .position(0.0)
                    .build();
        }

        long totalClicks = 0;
        long totalImpressions = 0;
        double weightedPositionSum = 0;

        for (GscReportResponse.GscTimelinePoint point : timeline) {
            totalClicks += point.getClicks();
            totalImpressions += point.getImpressions();
            weightedPositionSum += (point.getPosition() * point.getImpressions());
        }

        double overallCtr = totalImpressions > 0 ? ((double) totalClicks / totalImpressions) * 100.0 : 0.0;
        double overallPos = totalImpressions > 0 ? weightedPositionSum / totalImpressions : 0.0;

        return GscReportResponse.GscSummary.builder()
                .clicks(totalClicks)
                .impressions(totalImpressions)
                .ctr(Math.round(overallCtr * 100.0) / 100.0)
                .position(Math.round(overallPos * 10.0) / 10.0)
                .build();
    }

    private List<GscReportResponse.GscQueryItem> fetchGscTopQueries(URI uri, HttpHeaders headers, String startDate, String endDate) {
        Map<String, Object> reqBody = new HashMap<>();
        reqBody.put("startDate", startDate);
        reqBody.put("endDate", endDate);
        reqBody.put("dimensions", List.of("query"));
        reqBody.put("rowLimit", 10);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(reqBody, headers);
        ResponseEntity<JsonNode> response = restTemplate.postForEntity(uri, entity, JsonNode.class);

        List<GscReportResponse.GscQueryItem> items = new ArrayList<>();
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            JsonNode rows = response.getBody().path("rows");
            if (rows.isArray()) {
                for (JsonNode row : rows) {
                    items.add(GscReportResponse.GscQueryItem.builder()
                            .query(row.path("keys").path(0).asText(""))
                            .clicks(row.path("clicks").asLong(0))
                            .impressions(row.path("impressions").asLong(0))
                            .ctr(Math.round(row.path("ctr").asDouble(0.0) * 10000.0) / 100.0)
                            .position(Math.round(row.path("position").asDouble(0.0) * 10.0) / 10.0)
                            .build());
                }
            }
        }
        return items;
    }

    private List<GscReportResponse.GscPageItem> fetchGscTopPages(URI uri, HttpHeaders headers, String startDate, String endDate) {
        Map<String, Object> reqBody = new HashMap<>();
        reqBody.put("startDate", startDate);
        reqBody.put("endDate", endDate);
        reqBody.put("dimensions", List.of("page"));
        reqBody.put("rowLimit", 10);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(reqBody, headers);
        ResponseEntity<JsonNode> response = restTemplate.postForEntity(uri, entity, JsonNode.class);

        List<GscReportResponse.GscPageItem> items = new ArrayList<>();
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            JsonNode rows = response.getBody().path("rows");
            if (rows.isArray()) {
                for (JsonNode row : rows) {
                    items.add(GscReportResponse.GscPageItem.builder()
                            .page(row.path("keys").path(0).asText(""))
                            .clicks(row.path("clicks").asLong(0))
                            .impressions(row.path("impressions").asLong(0))
                            .ctr(Math.round(row.path("ctr").asDouble(0.0) * 10000.0) / 100.0)
                            .position(Math.round(row.path("position").asDouble(0.0) * 10.0) / 10.0)
                            .build());
                }
            }
        }
        return items;
    }

    private List<GscReportResponse.GscDeviceItem> fetchGscDevices(URI uri, HttpHeaders headers, String startDate, String endDate) {
        Map<String, Object> reqBody = new HashMap<>();
        reqBody.put("startDate", startDate);
        reqBody.put("endDate", endDate);
        reqBody.put("dimensions", List.of("device"));
        reqBody.put("rowLimit", 10);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(reqBody, headers);
        ResponseEntity<JsonNode> response = restTemplate.postForEntity(uri, entity, JsonNode.class);

        List<GscReportResponse.GscDeviceItem> items = new ArrayList<>();
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            JsonNode rows = response.getBody().path("rows");
            if (rows.isArray()) {
                for (JsonNode row : rows) {
                    items.add(GscReportResponse.GscDeviceItem.builder()
                            .device(row.path("keys").path(0).asText(""))
                            .clicks(row.path("clicks").asLong(0))
                            .impressions(row.path("impressions").asLong(0))
                            .ctr(Math.round(row.path("ctr").asDouble(0.0) * 10000.0) / 100.0)
                            .position(Math.round(row.path("position").asDouble(0.0) * 10.0) / 10.0)
                            .build());
                }
            }
        }
        return items;
    }

    private JsonNode loadServiceAccountCredentials() {
        try {
            // 0. Biến môi trường GOOGLE_CREDENTIALS_JSON chứa trực tiếp nội dung JSON (hữu ích khi deploy Cloud)
            String jsonContent = System.getenv("GOOGLE_CREDENTIALS_JSON");
            if (jsonContent == null || jsonContent.isBlank()) {
                jsonContent = System.getenv("GOOGLE_SERVICE_ACCOUNT_JSON");
            }
            if (jsonContent != null && !jsonContent.isBlank()) {
                return objectMapper.readTree(jsonContent.trim());
            }

            // 1. Kiểm tra đường dẫn trực tiếp
            if (credentialsPath != null && !credentialsPath.isBlank()) {
                File file = new File(credentialsPath);
                if (file.exists() && file.isFile()) {
                    try (InputStream is = new FileInputStream(file)) {
                        return objectMapper.readTree(is);
                    }
                }

                // 2. Kiểm tra thư mục gốc cha ../service-account.json
                File parentFile = new File(".." + File.separator + credentialsPath);
                if (parentFile.exists() && parentFile.isFile()) {
                    try (InputStream is = new FileInputStream(parentFile)) {
                        return objectMapper.readTree(is);
                    }
                }

                // 3. Kiểm tra classpath
                ClassPathResource cpr = new ClassPathResource(credentialsPath);
                if (cpr.exists()) {
                    try (InputStream is = cpr.getInputStream()) {
                        return objectMapper.readTree(is);
                    }
                }
            }

            // 4. Biến môi trường GOOGLE_APPLICATION_CREDENTIALS hoặc GOOGLE_CREDENTIALS_PATH
            String envPath = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
            if (envPath == null || envPath.isBlank()) {
                envPath = System.getenv("GOOGLE_CREDENTIALS_PATH");
            }
            if (envPath != null && !envPath.isBlank()) {
                File envFile = new File(envPath);
                if (envFile.exists() && envFile.isFile()) {
                    try (InputStream is = new FileInputStream(envFile)) {
                        return objectMapper.readTree(is);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Không thể tải tập tin chứng chỉ Google Service Account: {}", e.getMessage());
        }
        return null;
    }

    private PrivateKey parsePrivateKey(String pemKey) throws Exception {
        String clean = pemKey
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");
        byte[] encoded = Base64.getDecoder().decode(clean);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePrivate(keySpec);
    }

    private String formatDateString(String raw) {
        if (raw != null && raw.length() == 8) {
            return raw.substring(0, 4) + "-" + raw.substring(4, 6) + "-" + raw.substring(6, 8);
        }
        return raw != null ? raw : "";
    }

    private long parseLongSafe(JsonNode node, int index) {
        if (node != null && node.has(index)) {
            String val = node.get(index).path("value").asText("0");
            try {
                return Long.parseLong(val);
            } catch (NumberFormatException e) {
                return 0L;
            }
        }
        return 0L;
    }

    private double parseDoubleSafe(JsonNode node, int index) {
        if (node != null && node.has(index)) {
            String val = node.get(index).path("value").asText("0.0");
            try {
                return Double.parseDouble(val);
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }
        return 0.0;
    }

    private String normalizeRange(String range, String defaultRange) {
        if (range == null || range.isBlank()) {
            return defaultRange;
        }
        String clean = range.trim().toLowerCase();
        if (clean.matches("^\\d+d$")) {
            return clean;
        }
        return defaultRange;
    }

    private int parseDaysFromRange(String range, int defaultDays) {
        try {
            String num = range.replaceAll("[^0-9]", "");
            return Integer.parseInt(num);
        } catch (Exception e) {
            return defaultDays;
        }
    }

    private Ga4ReportResponse buildGa4FallbackResponse(String range, String errorMsg) {
        int days = parseDaysFromRange(range, 30);
        List<Ga4ReportResponse.Ga4TimelinePoint> points = new ArrayList<>();
        LocalDate today = LocalDate.now();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (int i = days - 1; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            points.add(Ga4ReportResponse.Ga4TimelinePoint.builder()
                    .date(d.format(dtf))
                    .activeUsers(0)
                    .sessions(0)
                    .screenPageViews(0)
                    .averageSessionDuration(0.0)
                    .build());
        }

        return Ga4ReportResponse.builder()
                .propertyId(ga4PropertyId)
                .dateRange(range)
                .liveData(false)
                .statusMessage("Dữ liệu mặc định do kết nối Google API gián đoạn: " + errorMsg)
                .summary(Ga4ReportResponse.Ga4Summary.builder()
                        .activeUsers(0)
                        .sessions(0)
                        .screenPageViews(0)
                        .averageSessionDuration(0.0)
                        .build())
                .timeline(points)
                .topPages(Collections.emptyList())
                .trafficSources(Collections.emptyList())
                .build();
    }

    private GscReportResponse buildGscFallbackResponse(String startDate, String endDate, String range, String errorMsg) {
        int days = parseDaysFromRange(range, 28);
        List<GscReportResponse.GscTimelinePoint> points = new ArrayList<>();
        LocalDate start = LocalDate.parse(startDate);
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (int i = 0; i < days; i++) {
            LocalDate d = start.plusDays(i);
            points.add(GscReportResponse.GscTimelinePoint.builder()
                    .date(d.format(dtf))
                    .clicks(0)
                    .impressions(0)
                    .ctr(0.0)
                    .position(0.0)
                    .build());
        }

        return GscReportResponse.builder()
                .siteUrl(gscSiteUrl)
                .startDate(startDate)
                .endDate(endDate)
                .liveData(false)
                .statusMessage("Dữ liệu mặc định do kết nối Google API gián đoạn: " + errorMsg)
                .summary(GscReportResponse.GscSummary.builder()
                        .clicks(0)
                        .impressions(0)
                        .ctr(0.0)
                        .position(0.0)
                        .build())
                .timeline(points)
                .topQueries(Collections.emptyList())
                .topPages(Collections.emptyList())
                .devices(Collections.emptyList())
                .build();
    }
}
