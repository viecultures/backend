package com.viecultures.backend.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.viecultures.backend.dto.analytics.ActivityLogBatchRequest;
import com.viecultures.backend.dto.analytics.ActivityLogEventDto;
import com.viecultures.backend.dto.analytics.ActivityLogResponse;
import com.viecultures.backend.dto.analytics.ActivityStatsSummaryDto;
import com.viecultures.backend.entity.UserActivityLog;
import com.viecultures.backend.repository.UserActivityLogRepository;
import com.viecultures.backend.service.UserActivityLogService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserActivityLogServiceImpl implements UserActivityLogService {

    private final UserActivityLogRepository activityLogRepository;
    private final ObjectMapper objectMapper;

    private static final Pattern POST_ID_PATTERN = Pattern.compile("[?&]postId=([^&#]+)");

    @Override
    @Transactional
    public void saveBatch(ActivityLogBatchRequest request, String ipAddress, String userAgent, String authUserId, String authUserEmail) {
        if (request == null || request.getEvents() == null || request.getEvents().isEmpty()) {
            return;
        }

        try {
            String resolvedUserId = StringUtils.hasText(authUserId) ? authUserId : request.getUserId();
            String resolvedUserEmail = StringUtils.hasText(authUserEmail) ? authUserEmail : request.getUserEmail();
            String anonymousSessionId = request.getAnonymousSessionId();

            List<UserActivityLog> entities = new ArrayList<>(request.getEvents().size());

            for (ActivityLogEventDto event : request.getEvents()) {
                if (event == null || !StringUtils.hasText(event.getEventType())) {
                    continue;
                }

                String payloadStr = null;
                if (event.getPayload() != null) {
                    try {
                        if (event.getPayload().isTextual()) {
                            payloadStr = event.getPayload().asText();
                        } else {
                            payloadStr = objectMapper.writeValueAsString(event.getPayload());
                        }
                    } catch (Exception ex) {
                        log.error("Failed to serialize activity log payload: {}", ex.getMessage(), ex);
                        payloadStr = event.getPayload().toString();
                    }
                }

                UserActivityLog logEntry = UserActivityLog.builder()
                        .userId(resolvedUserId)
                        .userEmail(resolvedUserEmail)
                        .anonymousSessionId(anonymousSessionId)
                        .eventType(event.getEventType().trim().toUpperCase())
                        .eventCategory(StringUtils.hasText(event.getEventCategory()) ? event.getEventCategory().trim().toUpperCase() : null)
                        .path(truncate(event.getPath(), 1000))
                        .payload(payloadStr)
                        .ipAddress(truncate(ipAddress, 100))
                        .userAgent(truncate(userAgent, 1000))
                        .build();

                entities.add(logEntry);
            }

            if (!entities.isEmpty()) {
                activityLogRepository.saveAll(entities);
                log.debug("Successfully saved {} activity logs in batch", entities.size());
            }
        } catch (Exception e) {
            log.error("Error occurred while saving batch activity logs: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ActivityLogResponse> getLogs(String eventType, String eventCategory, String userId, String search, String fromDate, String toDate, Pageable pageable) {
        try {
            LocalDateTime startDateTime = parseDateTime(fromDate, false);
            LocalDateTime endDateTime = parseDateTime(toDate, true);

            Specification<UserActivityLog> spec = (root, query, cb) -> {
                List<Predicate> predicates = new ArrayList<>();

                if (StringUtils.hasText(eventType)) {
                    predicates.add(cb.equal(cb.upper(root.get("eventType")), eventType.trim().toUpperCase()));
                }

                if (StringUtils.hasText(eventCategory)) {
                    predicates.add(cb.equal(cb.upper(root.get("eventCategory")), eventCategory.trim().toUpperCase()));
                }

                if (StringUtils.hasText(userId)) {
                    predicates.add(cb.equal(root.get("userId"), userId.trim()));
                }

                if (startDateTime != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDateTime));
                }

                if (endDateTime != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endDateTime));
                }

                if (StringUtils.hasText(search)) {
                    String searchPattern = "%" + search.trim().toLowerCase() + "%";
                    Predicate searchInPath = cb.like(cb.lower(root.get("path")), searchPattern);
                    Predicate searchInPayload = cb.like(cb.lower(root.get("payload")), searchPattern);
                    Predicate searchInEmail = cb.like(cb.lower(root.get("userEmail")), searchPattern);
                    Predicate searchInIp = cb.like(cb.lower(root.get("ipAddress")), searchPattern);
                    Predicate searchInAnon = cb.like(cb.lower(root.get("anonymousSessionId")), searchPattern);

                    predicates.add(cb.or(searchInPath, searchInPayload, searchInEmail, searchInIp, searchInAnon));
                }

                return cb.and(predicates.toArray(new Predicate[0]));
            };

            Page<UserActivityLog> pageResult = activityLogRepository.findAll(spec, pageable);
            return pageResult.map(this::mapToResponse);
        } catch (Exception e) {
            log.error("Error occurred while querying activity logs: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ActivityStatsSummaryDto getStatsSummary() {
        try {
            long totalLogs = activityLogRepository.count();

            Map<String, Long> categoryCounts = new HashMap<>();
            List<Object[]> categoryResults = activityLogRepository.countGroupedByEventCategory();
            for (Object[] row : categoryResults) {
                if (row != null && row.length >= 2 && row[0] != null) {
                    categoryCounts.put(row[0].toString(), ((Number) row[1]).longValue());
                }
            }

            Map<String, Long> eventTypeCounts = new HashMap<>();
            List<Object[]> eventTypeResults = activityLogRepository.countGroupedByEventType();
            for (Object[] row : eventTypeResults) {
                if (row != null && row.length >= 2 && row[0] != null) {
                    eventTypeCounts.put(row[0].toString(), ((Number) row[1]).longValue());
                }
            }

            List<ActivityStatsSummaryDto.TopMetricItemDto> topReadPosts = extractTopReadPosts();
            List<ActivityStatsSummaryDto.TopMetricItemDto> topLookedUpWords = extractTopLookedUpWords();

            return ActivityStatsSummaryDto.builder()
                    .totalLogs(totalLogs)
                    .categoryCounts(categoryCounts)
                    .eventTypeCounts(eventTypeCounts)
                    .topReadPosts(topReadPosts)
                    .topLookedUpWords(topLookedUpWords)
                    .build();
        } catch (Exception e) {
            log.error("Error occurred while generating activity stats summary: {}", e.getMessage(), e);
            return ActivityStatsSummaryDto.builder().build();
        }
    }

    private List<ActivityStatsSummaryDto.TopMetricItemDto> extractTopReadPosts() {
        List<ActivityStatsSummaryDto.TopMetricItemDto> result = new ArrayList<>();
        try {
            List<Object[]> topPaths = activityLogRepository.findTopReadPaths(PageRequest.of(0, 10));
            Map<String, Long> aggregated = new LinkedHashMap<>();

            for (Object[] row : topPaths) {
                if (row == null || row.length < 2 || row[0] == null) {
                    continue;
                }
                String path = row[0].toString();
                long count = ((Number) row[1]).longValue();

                String postId = extractPostIdFromPath(path);
                String key = postId != null ? postId : path;
                aggregated.put(key, aggregated.getOrDefault(key, 0L) + count);
            }

            aggregated.entrySet().stream()
                    .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
                    .limit(5)
                    .forEach(entry -> result.add(ActivityStatsSummaryDto.TopMetricItemDto.builder()
                            .key(entry.getKey())
                            .label(formatPostLabel(entry.getKey()))
                            .count(entry.getValue())
                            .build()));
        } catch (Exception e) {
            log.error("Error extracting top read posts: {}", e.getMessage(), e);
        }
        return result;
    }

    private List<ActivityStatsSummaryDto.TopMetricItemDto> extractTopLookedUpWords() {
        List<ActivityStatsSummaryDto.TopMetricItemDto> result = new ArrayList<>();
        try {
            List<String> payloads = activityLogRepository.findRecentVocabPayloads(PageRequest.of(0, 500));
            Map<String, Long> wordCounts = new HashMap<>();

            for (String payload : payloads) {
                if (!StringUtils.hasText(payload)) {
                    continue;
                }
                try {
                    JsonNode node = objectMapper.readTree(payload);
                    String word = null;
                    if (node.hasNonNull("word")) {
                        word = node.get("word").asText();
                    } else if (node.hasNonNull("term")) {
                        word = node.get("term").asText();
                    } else if (node.hasNonNull("vocab")) {
                        word = node.get("vocab").asText();
                    } else if (node.hasNonNull("query")) {
                        word = node.get("query").asText();
                    }

                    if (StringUtils.hasText(word)) {
                        String cleanWord = word.trim().toLowerCase();
                        wordCounts.put(cleanWord, wordCounts.getOrDefault(cleanWord, 0L) + 1L);
                    }
                } catch (Exception ex) {
                    // Non-JSON payload or plain string word
                    String trimmed = payload.trim().toLowerCase();
                    if (trimmed.length() <= 50 && !trimmed.startsWith("{")) {
                        wordCounts.put(trimmed, wordCounts.getOrDefault(trimmed, 0L) + 1L);
                    }
                }
            }

            wordCounts.entrySet().stream()
                    .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
                    .limit(5)
                    .forEach(entry -> result.add(ActivityStatsSummaryDto.TopMetricItemDto.builder()
                            .key(entry.getKey())
                            .label(entry.getKey())
                            .count(entry.getValue())
                            .build()));
        } catch (Exception e) {
            log.error("Error extracting top looked up words: {}", e.getMessage(), e);
        }
        return result;
    }

    private ActivityLogResponse mapToResponse(UserActivityLog entity) {
        return ActivityLogResponse.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .userEmail(entity.getUserEmail())
                .anonymousSessionId(entity.getAnonymousSessionId())
                .eventType(entity.getEventType())
                .eventCategory(entity.getEventCategory())
                .path(entity.getPath())
                .payload(entity.getPayload())
                .ipAddress(entity.getIpAddress())
                .userAgent(entity.getUserAgent())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private String extractPostIdFromPath(String path) {
        if (!StringUtils.hasText(path)) {
            return null;
        }
        Matcher matcher = POST_ID_PATTERN.matcher(path);
        if (matcher.find()) {
            return matcher.group(1);
        }
        if (path.contains("/reader/") || path.contains("/posts/")) {
            String[] segments = path.split("/");
            if (segments.length > 0) {
                return segments[segments.length - 1];
            }
        }
        return null;
    }

    private String formatPostLabel(String key) {
        if (!StringUtils.hasText(key)) {
            return "Unknown Article";
        }
        return key.replace("-", " ");
    }

    private LocalDateTime parseDateTime(String text, boolean endOfDay) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        try {
            String trimmed = text.trim();
            if (trimmed.length() == 10) {
                LocalDate date = LocalDate.parse(trimmed, DateTimeFormatter.ISO_LOCAL_DATE);
                return endOfDay ? date.atTime(23, 59, 59, 999999999) : date.atStartOfDay();
            }
            if (trimmed.contains("T")) {
                return LocalDateTime.parse(trimmed, DateTimeFormatter.ISO_DATE_TIME);
            }
            return LocalDateTime.parse(trimmed, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (Exception e) {
            log.error("Could not parse date string: {}. Error: {}", text, e.getMessage());
            return null;
        }
    }

    private String truncate(String text, int maxLength) {
        if (text == null) {
            return null;
        }
        return text.length() > maxLength ? text.substring(0, maxLength) : text;
    }
}
