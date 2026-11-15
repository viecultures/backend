package com.viecultures.backend.service;

import com.viecultures.backend.dto.LearningJourneyRequest;
import com.viecultures.backend.dto.LearningJourneyResponse;
import com.viecultures.backend.dto.PagedLearningJourneyResponse;
import com.viecultures.backend.entity.CefrLevel;
import com.viecultures.backend.entity.LearningJourney;
import com.viecultures.backend.entity.ReadingPost;
import com.viecultures.backend.repository.LearningJourneyRepository;
import com.viecultures.backend.repository.ReadingPostRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LearningJourneyService {

    private final LearningJourneyRepository journeyRepository;
    private final ReadingPostRepository readingPostRepository;

    /**
     * Tự động quét và xuất bản các hành trình học có trạng thái "scheduled" đã đến hạn.
     */
    @Transactional
    public int autoPublishScheduledJourneys() {
        LocalDateTime now = LocalDateTime.now();
        List<LearningJourney> dueJourneys = journeyRepository.findByStatusIgnoreCaseAndScheduledPublishAtLessThanEqual("scheduled", now);
        if (dueJourneys.isEmpty()) {
            return 0;
        }

        for (LearningJourney journey : dueJourneys) {
            journey.setStatus("published");
            journeyRepository.save(journey);
            log.info("[LearningJourneyService] Đã tự động xuất bản hành trình học: ID={}, Title={}, Lên lịch={}",
                    journey.getId(), journey.getTitleEn(), journey.getScheduledPublishAt());
        }
        return dueJourneys.size();
    }

    /**
     * Cron chạy ngầm định kỳ mỗi 60 giây kiểm tra và kích hoạt xuất bản các hành trình đến hạn.
     */
    @Scheduled(fixedRate = 60000)
    public void runScheduledPublishCron() {
        int count = autoPublishScheduledJourneys();
        if (count > 0) {
            log.info("[ScheduledPublish Cron] Đã tự động xuất bản thành công {} hành trình học.", count);
        }
    }

    private Specification<LearningJourney> buildSpecification(String region, String provinceId, String theme, String status, String query) {
        return (root, q, cb) -> {
            if (q != null) {
                q.distinct(true);
            }
            List<Predicate> predicates = new ArrayList<>();

            if (region != null && !region.isBlank() && !region.equalsIgnoreCase("all")) {
                predicates.add(cb.equal(cb.lower(root.get("region")), region.toLowerCase().trim()));
            }

            if (provinceId != null && !provinceId.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("provinceId")), provinceId.toLowerCase().trim()));
            }

            if (theme != null && !theme.isBlank() && !theme.equalsIgnoreCase("all")) {
                predicates.add(cb.equal(cb.lower(root.get("theme")), theme.toLowerCase().trim()));
            }

            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("status")), status.toLowerCase().trim()));
            }

            if (query != null && !query.isBlank()) {
                String pattern = "%" + query.toLowerCase().trim() + "%";
                Predicate matchEn = cb.like(cb.lower(root.get("titleEn")), pattern);
                Predicate matchVi = cb.like(cb.lower(root.get("titleVi")), pattern);
                Predicate matchDesc = cb.like(cb.lower(root.get("description")), pattern);
                Predicate matchDescVi = cb.like(cb.lower(root.get("descriptionVi")), pattern);
                predicates.add(cb.or(matchEn, matchVi, matchDesc, matchDescVi));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    @Transactional(readOnly = true)
    public List<LearningJourneyResponse> getJourneys(String region, String provinceId, String theme, String status, String query) {
        Specification<LearningJourney> spec = buildSpecification(region, provinceId, theme, status, query);
        List<LearningJourney> journeys = journeyRepository.findAll(spec);
        return journeys.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PagedLearningJourneyResponse getPagedJourneys(String region, String provinceId, String theme, String status, String query, int page, int size) {
        Specification<LearningJourney> spec = buildSpecification(region, provinceId, theme, status, query);
        int validPage = Math.max(0, page);
        int validSize = size > 0 ? size : 6;
        Pageable pageable = PageRequest.of(validPage, validSize);
        Page<LearningJourney> journeyPage = journeyRepository.findAll(spec, pageable);

        List<LearningJourneyResponse> items = journeyPage.getContent().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return PagedLearningJourneyResponse.builder()
                .items(items)
                .page(journeyPage.getNumber())
                .size(journeyPage.getSize())
                .totalElements(journeyPage.getTotalElements())
                .totalPages(journeyPage.getTotalPages())
                .build();
    }

    @Transactional(readOnly = true)
    public LearningJourneyResponse getJourneyById(String id) {
        LearningJourney journey = journeyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Hành trình học không tồn tại với ID: " + id));
        return toResponse(journey);
    }

    @Transactional
    public LearningJourneyResponse createJourney(LearningJourneyRequest request) {
        String slug = generateSlug(request.getId(), request.getTitleEn());

        if (journeyRepository.existsById(slug)) {
            slug = slug + "-" + System.currentTimeMillis() % 10000;
        }

        String targetStatus = request.getStatus() != null && !request.getStatus().isBlank()
                ? request.getStatus().toLowerCase().trim()
                : "published";

        LocalDateTime scheduledPublishAt = request.getScheduledPublishAt();
        if ("scheduled".equalsIgnoreCase(targetStatus)) {
            if (scheduledPublishAt != null && !scheduledPublishAt.isAfter(LocalDateTime.now())) {
                targetStatus = "published";
            }
        }

        LearningJourney journey = LearningJourney.builder()
                .id(slug)
                .titleEn(request.getTitleEn())
                .titleVi(request.getTitleVi())
                .theme(request.getTheme() != null ? request.getTheme().toLowerCase().trim() : "cuisine")
                .provinceId(request.getProvinceId() != null ? request.getProvinceId().trim() : "")
                .region(request.getRegion() != null ? request.getRegion().toLowerCase().trim() : "north")
                .description(request.getDescription())
                .descriptionVi(request.getDescriptionVi())
                .imageUrl(request.getImageUrl())
                .status(targetStatus)
                .scheduledPublishAt(scheduledPublishAt)
                .build();

        LearningJourney saved = journeyRepository.save(journey);

        if (request.getChapterPostIds() != null && !request.getChapterPostIds().isEmpty()) {
            assignChapters(saved, request.getChapterPostIds());
        }

        return toResponse(saved);
    }

    @Transactional
    public LearningJourneyResponse updateJourney(String id, LearningJourneyRequest request) {
        LearningJourney journey = journeyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Hành trình học không tồn tại với ID: " + id));

        journey.setTitleEn(request.getTitleEn());
        journey.setTitleVi(request.getTitleVi());
        if (request.getTheme() != null) journey.setTheme(request.getTheme().toLowerCase().trim());
        if (request.getProvinceId() != null) journey.setProvinceId(request.getProvinceId().trim());
        if (request.getRegion() != null) journey.setRegion(request.getRegion().toLowerCase().trim());
        journey.setDescription(request.getDescription());
        if (request.getDescriptionVi() != null) {
            journey.setDescriptionVi(request.getDescriptionVi());
        }
        journey.setImageUrl(request.getImageUrl());
        if (request.getStatus() != null) {
            String targetStatus = request.getStatus().toLowerCase().trim();
            LocalDateTime scheduledPublishAt = request.getScheduledPublishAt();
            if ("scheduled".equalsIgnoreCase(targetStatus)) {
                if (scheduledPublishAt != null && !scheduledPublishAt.isAfter(LocalDateTime.now())) {
                    targetStatus = "published";
                }
            }
            journey.setStatus(targetStatus);
            journey.setScheduledPublishAt(scheduledPublishAt);
        } else if (request.getScheduledPublishAt() != null) {
            journey.setScheduledPublishAt(request.getScheduledPublishAt());
        }

        LearningJourney updated = journeyRepository.save(journey);

        if (request.getChapterPostIds() != null) {
            assignChapters(updated, request.getChapterPostIds());
        }

        return toResponse(updated);
    }

    @Transactional
    public void deleteJourney(String id) {
        LearningJourney journey = journeyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Hành trình học không tồn tại với ID: " + id));

        // Unlink any associated posts so they become standalone posts
        if (journey.getChapters() != null) {
            for (ReadingPost post : journey.getChapters()) {
                post.setJourney(null);
                post.setChapterOrder(null);
                readingPostRepository.save(post);
            }
        }

        journeyRepository.delete(journey);
    }

    private void assignChapters(LearningJourney journey, List<String> chapterPostIds) {
        // Unlink previous posts not in the new list
        List<ReadingPost> currentPosts = journey.getChapters() != null ? journey.getChapters() : new ArrayList<>();
        Set<String> newPostIdSet = new HashSet<>(chapterPostIds);

        for (ReadingPost current : currentPosts) {
            if (!newPostIdSet.contains(current.getId())) {
                current.setJourney(null);
                current.setChapterOrder(null);
                readingPostRepository.save(current);
            }
        }

        // Link and order new posts
        for (int i = 0; i < chapterPostIds.size(); i++) {
            String postId = chapterPostIds.get(i);
            Optional<ReadingPost> optPost = readingPostRepository.findById(postId);
            if (optPost.isPresent()) {
                ReadingPost post = optPost.get();
                post.setJourney(journey);
                post.setChapterOrder(i + 1);
                if (journey.getProvinceId() != null && !journey.getProvinceId().isBlank()) {
                    post.setProvinceId(journey.getProvinceId());
                }
                readingPostRepository.save(post);
            }
        }
    }

    public LearningJourneyResponse toResponse(LearningJourney journey) {
        List<LearningJourneyResponse.ChapterSummaryDto> chapterDtos = Collections.emptyList();
        if (journey.getChapters() != null && !journey.getChapters().isEmpty()) {
            Map<String, List<ReadingPost>> grouped = journey.getChapters().stream()
                    .filter(p -> p.getStatus() == null || "published".equalsIgnoreCase(p.getStatus()))
                    .collect(Collectors.groupingBy(
                            p -> p.getChapterOrder() != null ? "order-" + p.getChapterOrder()
                                    : (p.getTopicGroup() != null && !p.getTopicGroup().isBlank()
                                            ? "group-" + p.getTopicGroup() : "id-" + p.getId()),
                            LinkedHashMap::new,
                            Collectors.toList()
                    ));

            List<LearningJourneyResponse.ChapterSummaryDto> list = new ArrayList<>();
            for (Map.Entry<String, List<ReadingPost>> entry : grouped.entrySet()) {
                List<ReadingPost> groupPosts = entry.getValue();
                if (groupPosts.isEmpty()) continue;

                ReadingPost rep = groupPosts.stream()
                        .filter(p -> p.getLevel() == CefrLevel.B1 || p.getLevel() == CefrLevel.B2)
                        .findFirst()
                        .orElse(groupPosts.get(0));

                int minOrder = groupPosts.stream()
                        .map(ReadingPost::getChapterOrder)
                        .filter(Objects::nonNull)
                        .min(Integer::compareTo)
                        .orElse(999);

                list.add(LearningJourneyResponse.ChapterSummaryDto.builder()
                        .id(rep.getId())
                        .chapterOrder(minOrder != 999 ? minOrder : (list.size() + 1))
                        .titleEn(rep.getTitleEn())
                        .titleVi(rep.getTitleVi())
                        .summary(rep.getSummary())
                        .readTime(rep.getReadTime())
                        .level(rep.getLevel() != null ? rep.getLevel().name() : "B1")
                        .imageUrl(rep.getImageUrl())
                        .build());
            }

            list.sort(Comparator.comparing(c -> c.getChapterOrder() != null ? c.getChapterOrder() : 999));
            chapterDtos = list;
        }

        return LearningJourneyResponse.builder()
                .id(journey.getId())
                .titleEn(journey.getTitleEn())
                .titleVi(journey.getTitleVi())
                .theme(journey.getTheme())
                .provinceId(journey.getProvinceId())
                .region(journey.getRegion())
                .description(journey.getDescription())
                .descriptionVi(journey.getDescriptionVi())
                .imageUrl(journey.getImageUrl())
                .status(journey.getStatus())
                .scheduledPublishAt(journey.getScheduledPublishAt())
                .createdAt(journey.getCreatedAt())
                .chapters(chapterDtos)
                .build();
    }

    private String generateSlug(String explicitId, String titleEn) {
        if (explicitId != null && !explicitId.trim().isEmpty()) {
            return slugify(explicitId.trim());
        }
        if (titleEn != null && !titleEn.trim().isEmpty()) {
            return slugify(titleEn.trim());
        }
        return "journey-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String slugify(String input) {
        String nfdNormalizedString = Normalizer.normalize(input, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        String noDiacritics = pattern.matcher(nfdNormalizedString).replaceAll("");
        return noDiacritics.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
        .replaceAll("^-+|-+$", "");
    }
}
