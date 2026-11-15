package com.viecultures.backend.service;

import com.viecultures.backend.dto.CatalogLessonDto;
import com.viecultures.backend.dto.CatalogResponse;
import com.viecultures.backend.dto.ReadingPostRequest;
import com.viecultures.backend.dto.ReadingPostResponse;
import com.viecultures.backend.entity.CefrLevel;
import com.viecultures.backend.entity.ParagraphPair;
import com.viecultures.backend.entity.QuizQuestion;
import com.viecultures.backend.entity.ReadingPost;
import com.viecultures.backend.entity.VocabItem;
import com.viecultures.backend.repository.LearningJourneyRepository;
import com.viecultures.backend.repository.ParagraphPairRepository;
import com.viecultures.backend.repository.ReadingPostRepository;
import com.viecultures.backend.repository.VocabItemRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReadingPostService {

    private final ReadingPostRepository readingPostRepository;
    private final LearningJourneyRepository journeyRepository;
    private final ParagraphPairRepository paragraphPairRepository;
    private final VocabItemRepository vocabItemRepository;

    @Transactional(readOnly = true)
    public List<ReadingPostResponse> getPosts(String topic, CefrLevel level, String query, String region, String topicGroup) {
        return getPosts(topic, level, query, region, topicGroup, null, null, null);
    }

    @Transactional(readOnly = true)
    public List<ReadingPostResponse> getPosts(String topic, CefrLevel level, String query, String region, String topicGroup, Boolean standalone, String journeyId, String provinceId) {
        Specification<ReadingPost> spec = (root, q, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (topic != null && !topic.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("topic")), topic.toLowerCase().trim()));
            }

            if (region != null && !region.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("region")), region.toLowerCase().trim()));
            }

            if (provinceId != null && !provinceId.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("provinceId")), provinceId.toLowerCase().trim()));
            }

            if (topicGroup != null && !topicGroup.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("topicGroup")), topicGroup.toLowerCase().trim()));
            }

            if (level != null) {
                predicates.add(cb.equal(root.get("level"), level));
            }

            if (Boolean.TRUE.equals(standalone)) {
                predicates.add(cb.isNull(root.get("journey")));
            }

            if (journeyId != null && !journeyId.isBlank()) {
                predicates.add(cb.equal(root.get("journey").get("id"), journeyId.trim()));
            }

            if (query != null && !query.isBlank()) {
                String pattern = "%" + query.toLowerCase().trim() + "%";
                Predicate matchEn = cb.like(cb.lower(root.get("titleEn")), pattern);
                Predicate matchVi = cb.like(cb.lower(root.get("titleVi")), pattern);
                Predicate matchTopicGroup = cb.like(cb.lower(root.get("topicGroup")), pattern);
                predicates.add(cb.or(matchEn, matchVi, matchTopicGroup));
            }

            predicates.add(cb.or(
                cb.isNull(root.get("status")),
                cb.equal(cb.lower(root.get("status")), "published")
            ));

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        List<ReadingPost> posts = readingPostRepository.findAll(spec);
        return posts.stream()
                .map(p -> toResponse(p, false))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ReadingPostResponse getPostById(String id) {
        ReadingPost post = readingPostRepository.findById(id)
                .or(() -> {
                    if ("tet-holiday".equals(id)) return readingPostRepository.findById("tet-festivities");
                    if ("tet-festivities".equals(id)) return readingPostRepository.findById("tet-holiday");
                    return Optional.empty();
                })
                .orElseThrow(() -> new RuntimeException("Bài đọc không tồn tại với ID: " + id));
        return toResponse(post, true);
    }

    @Transactional
    public ReadingPostResponse createPost(ReadingPostRequest request) {
        ReadingPost post = ReadingPost.builder()
                .id(request.getId() != null && !request.getId().isBlank() ? request.getId().trim() : UUID.randomUUID().toString())
                .topicGroup(request.getTopicGroup())
                .titleEn(request.getTitleEn())
                .titleVi(request.getTitleVi())
                .topic(request.getTopic())
                .region(request.getRegion())
                .provinceId(request.getProvinceId())
                .chapterOrder(request.getChapterOrder())
                .imageUrl(request.getImageUrl())
                .wordCount(request.getWordCount())
                .summary(request.getSummary())
                .contentEn(request.getContentEn())
                .contentVi(request.getContentVi())
                .shadowingAudioUrl(request.getShadowingAudioUrl())
                .audioTracks(request.getAudioTracks())
                .level(request.getLevel())
                .readTime(request.getReadTime())
                .featured(Boolean.TRUE.equals(request.getFeatured()))
                .status(request.getStatus() != null && !request.getStatus().isBlank() ? request.getStatus().toLowerCase().trim() : "published")
                .build();

        if (request.getJourneyId() != null && !request.getJourneyId().isBlank()) {
            journeyRepository.findById(request.getJourneyId()).ifPresent(post::setJourney);
        }

        if (request.getParagraphs() != null) {
            for (ReadingPostRequest.ParagraphPairDto pDto : request.getParagraphs()) {
                ParagraphPair pair = ParagraphPair.builder()
                        .readingPost(post)
                        .orderIndex(pDto.getOrderIndex())
                        .enText(pDto.getEnText())
                        .viText(pDto.getViText())
                        .audioTimestampStart(pDto.getAudioTimestampStart())
                        .audioTimestampEnd(pDto.getAudioTimestampEnd())
                        .build();
                post.getParagraphs().add(pair);
            }
        }

        if (request.getVocabularies() != null) {
            for (ReadingPostRequest.VocabItemDto vDto : request.getVocabularies()) {
                VocabItem vocab = VocabItem.builder()
                        .readingPost(post)
                        .word(vDto.getWord())
                        .ipa(vDto.getIpa())
                        .pos(vDto.getPos())
                        .viMeaning(vDto.getViMeaning())
                        .contextSentence(vDto.getContextSentence())
                        .audioUrl(vDto.getAudioUrl())
                        .level(vDto.getLevel())
                        .usageNote(vDto.getUsageNote())
                        .build();
                post.getVocabularies().add(vocab);
            }
        }

        if (request.getQuestions() != null) {
            for (ReadingPostRequest.QuizQuestionDto qDto : request.getQuestions()) {
                QuizQuestion question = QuizQuestion.builder()
                        .readingPost(post)
                        .orderIndex(qDto.getOrderIndex() != null ? qDto.getOrderIndex() : 0)
                        .question(qDto.getQuestion())
                        .options(qDto.getOptions())
                        .correctKey(qDto.getCorrectKey())
                        .explanation(qDto.getExplanation())
                        .build();
                post.getQuestions().add(question);
            }
        }

        ReadingPost saved = readingPostRepository.save(post);
        return toResponse(saved, true);
    }

    @Transactional
    public ReadingPostResponse updatePost(String id, ReadingPostRequest request) {
        ReadingPost post = readingPostRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bài đọc không tồn tại với ID: " + id));

        post.setTopicGroup(request.getTopicGroup());
        post.setTitleEn(request.getTitleEn());
        post.setTitleVi(request.getTitleVi());
        post.setTopic(request.getTopic());
        post.setRegion(request.getRegion());
        if (request.getProvinceId() != null) {
            post.setProvinceId(request.getProvinceId());
        }
        post.setChapterOrder(request.getChapterOrder());
        post.setImageUrl(request.getImageUrl());
        post.setWordCount(request.getWordCount());
        post.setSummary(request.getSummary());
        post.setContentEn(request.getContentEn());
        post.setContentVi(request.getContentVi());
        post.setShadowingAudioUrl(request.getShadowingAudioUrl());
        post.setAudioTracks(request.getAudioTracks());
        post.setLevel(request.getLevel());
        post.setReadTime(request.getReadTime());
        if (request.getFeatured() != null) {
            post.setFeatured(request.getFeatured());
        }
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            post.setStatus(request.getStatus().toLowerCase().trim());
        }

        if (request.getJourneyId() != null && !request.getJourneyId().isBlank()) {
            journeyRepository.findById(request.getJourneyId()).ifPresent(post::setJourney);
        } else if (request.getJourneyId() != null && request.getJourneyId().isBlank()) {
            post.setJourney(null);
            post.setChapterOrder(null);
        }

        if (request.getParagraphs() != null) {
            post.getParagraphs().clear();
            for (ReadingPostRequest.ParagraphPairDto pDto : request.getParagraphs()) {
                ParagraphPair pair = ParagraphPair.builder()
                        .readingPost(post)
                        .orderIndex(pDto.getOrderIndex())
                        .enText(pDto.getEnText())
                        .viText(pDto.getViText())
                        .audioTimestampStart(pDto.getAudioTimestampStart())
                        .audioTimestampEnd(pDto.getAudioTimestampEnd())
                        .build();
                post.getParagraphs().add(pair);
            }
        }

        if (request.getVocabularies() != null) {
            post.getVocabularies().clear();
            for (ReadingPostRequest.VocabItemDto vDto : request.getVocabularies()) {
                VocabItem vocab = VocabItem.builder()
                        .readingPost(post)
                        .word(vDto.getWord())
                        .ipa(vDto.getIpa())
                        .pos(vDto.getPos())
                        .viMeaning(vDto.getViMeaning())
                        .contextSentence(vDto.getContextSentence())
                        .audioUrl(vDto.getAudioUrl())
                        .level(vDto.getLevel())
                        .usageNote(vDto.getUsageNote())
                        .build();
                post.getVocabularies().add(vocab);
            }
        }

        if (request.getQuestions() != null) {
            post.getQuestions().clear();
            for (ReadingPostRequest.QuizQuestionDto qDto : request.getQuestions()) {
                QuizQuestion question = QuizQuestion.builder()
                        .readingPost(post)
                        .orderIndex(qDto.getOrderIndex() != null ? qDto.getOrderIndex() : 0)
                        .question(qDto.getQuestion())
                        .options(qDto.getOptions())
                        .correctKey(qDto.getCorrectKey())
                        .explanation(qDto.getExplanation())
                        .build();
                post.getQuestions().add(question);
            }
        }

        ReadingPost updated = readingPostRepository.save(post);
        return toResponse(updated, true);
    }

    @Transactional
    public void deletePost(String id) {
        if (!readingPostRepository.existsById(id)) {
            throw new RuntimeException("Bài đọc không tồn tại với ID: " + id);
        }
        readingPostRepository.deleteById(id);
    }

    @Transactional
    public void incrementReads(String id) {
        readingPostRepository.findById(id).ifPresent(post -> {
            post.setTotalReads(post.getTotalReads() == null ? 1 : post.getTotalReads() + 1);
            readingPostRepository.save(post);
        });
    }

    public ReadingPostResponse toResponse(ReadingPost post, boolean includeDetails) {
        ReadingPostResponse.ReadingPostResponseBuilder builder = ReadingPostResponse.builder()
                .id(post.getId())
                .topicGroup(post.getTopicGroup())
                .titleEn(post.getTitleEn())
                .titleVi(post.getTitleVi())
                .topic(post.getTopic())
                .region(post.getRegion())
                .provinceId(post.getProvinceId())
                .chapterOrder(post.getChapterOrder())
                .imageUrl(post.getImageUrl())
                .wordCount(post.getWordCount())
                .summary(post.getSummary())
                .contentEn(post.getContentEn())
                .contentVi(post.getContentVi())
                .shadowingAudioUrl(post.getShadowingAudioUrl())
                .audioTracks(post.getAudioTracks())
                .level(post.getLevel())
                .readTime(post.getReadTime())
                .totalReads(post.getTotalReads())
                .likesCount(post.getLikesCount())
                .featured(Boolean.TRUE.equals(post.getFeatured()))
                .status(post.getStatus() != null ? post.getStatus() : "published")
                .createdAt(post.getCreatedAt());

        if (post.getJourney() != null) {
            builder.journeyId(post.getJourney().getId());
            builder.journeyTitleEn(post.getJourney().getTitleEn());
        }

        if (includeDetails) {
            if (post.getParagraphs() != null) {
                builder.paragraphs(post.getParagraphs().stream()
                        .map(p -> ReadingPostResponse.ParagraphPairDto.builder()
                                .id(p.getId())
                                .orderIndex(p.getOrderIndex())
                                .enText(p.getEnText())
                                .viText(p.getViText())
                                .audioTimestampStart(p.getAudioTimestampStart())
                                .audioTimestampEnd(p.getAudioTimestampEnd())
                                .build())
                        .collect(Collectors.toList()));
            }

            if (post.getVocabularies() != null) {
                builder.vocabularies(post.getVocabularies().stream()
                        .map(v -> ReadingPostResponse.VocabItemDto.builder()
                                .id(v.getId())
                                .word(v.getWord())
                                .ipa(v.getIpa())
                                .pos(v.getPos())
                                .viMeaning(v.getViMeaning())
                                .contextSentence(v.getContextSentence())
                                .audioUrl(v.getAudioUrl())
                                .level(v.getLevel())
                                .usageNote(v.getUsageNote())
                                .build())
                        .collect(Collectors.toList()));
            }

            if (post.getQuestions() != null) {
                builder.questions(post.getQuestions().stream()
                        .map(q -> ReadingPostResponse.QuizQuestionDto.builder()
                                .id(q.getId())
                                .orderIndex(q.getOrderIndex())
                                .question(q.getQuestion())
                                .options(q.getOptions())
                                .correctKey(q.getCorrectKey())
                                .explanation(q.getExplanation())
                                .build())
                        .collect(Collectors.toList()));
            }
        }

        return builder.build();
    }

    @Transactional(readOnly = true)
    public CatalogResponse getCatalog(String topic, String level, String query, String region, String sortBy, Integer page, Integer size) {
        List<ReadingPost> allPosts = readingPostRepository.findAll();

        // 1. Group posts by topicGroup or individual id
        Map<String, List<ReadingPost>> groups = new LinkedHashMap<>();
        for (ReadingPost post : allPosts) {
            if (post.getStatus() != null && !"published".equalsIgnoreCase(post.getStatus())) {
                continue;
            }
            String key = (post.getTopicGroup() != null && !post.getTopicGroup().isBlank())
                    ? "group:" + post.getTopicGroup().trim().toLowerCase()
                    : "id:" + post.getId();
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(post);
        }

        // 2. Build CatalogLessonDto for each group
        List<CatalogLessonDto> lessonList = new ArrayList<>();
        Map<String, Long> topicCounts = new HashMap<>();

        for (Map.Entry<String, List<ReadingPost>> entry : groups.entrySet()) {
            List<ReadingPost> postGroup = entry.getValue();
            if (postGroup.isEmpty()) continue;

            // Pick representative post (prefer Level 2/B1/B2, else first)
            ReadingPost primary = postGroup.stream()
                    .filter(p -> p.getLevel() == CefrLevel.B1 || p.getLevel() == CefrLevel.B2)
                    .findFirst()
                    .orElse(postGroup.get(0));

            String category = normalizeCategory(primary.getTopic());
            topicCounts.put(category, topicCounts.getOrDefault(category, 0L) + 1L);

            List<String> availableLevels = new ArrayList<>();
            Map<String, CatalogLessonDto.LevelDetailDto> levelDetails = new HashMap<>();
            Map<String, String> postIds = new HashMap<>();

            int maxVocabCount = 0;
            int totalReads = 0;
            int totalLikes = 0;
            boolean isFeatured = false;

            for (ReadingPost p : postGroup) {
                String levelType = mapCefrToLevelType(p.getLevel());
                if (!availableLevels.contains(levelType)) {
                    availableLevels.add(levelType);
                }
                postIds.put(levelType, p.getId());

                int vCount = (p.getVocabularies() != null) ? p.getVocabularies().size() : 0;
                if (vCount > maxVocabCount) maxVocabCount = vCount;

                if (p.getTotalReads() != null) totalReads += p.getTotalReads();
                if (p.getLikesCount() != null) totalLikes += p.getLikesCount();
                if (Boolean.TRUE.equals(p.getFeatured())) isFeatured = true;

                levelDetails.put(levelType, CatalogLessonDto.LevelDetailDto.builder()
                        .readTime(p.getReadTime() != null ? p.getReadTime() : "4 min read")
                        .vocabCount(vCount)
                        .wordCount(p.getWordCount() != null ? p.getWordCount() : 350)
                        .summary(p.getSummary())
                        .build());
            }

            availableLevels.sort((l1, l2) -> {
                int n1 = Integer.parseInt(l1.replaceAll("\\D+", ""));
                int n2 = Integer.parseInt(l2.replaceAll("\\D+", ""));
                return Integer.compare(n1, n2);
            });

            CatalogLessonDto dto = CatalogLessonDto.builder()
                    .id(primary.getId())
                    .postIds(postIds)
                    .topicGroup(primary.getTopicGroup())
                    .titleEn(primary.getTitleEn())
                    .titleVi(primary.getTitleVi())
                    .category(category)
                    .categoryVi(getCategoryVi(category))
                    .region(primary.getRegion() != null ? primary.getRegion() : "Việt Nam")
                    .imageUrl(primary.getImageUrl() != null && !primary.getImageUrl().isBlank()
                            ? primary.getImageUrl()
                            : "https://images.unsplash.com/photo-1543857778-c4a1a3e0b2eb?auto=format&fit=crop&w=1200&q=80")
                    .summary(primary.getSummary())
                    .featured(isFeatured)
                    .totalReads(totalReads)
                    .likesCount(totalLikes)
                    .primaryLevel(availableLevels.contains("Level 2") ? "Level 2" : availableLevels.get(0))
                    .readTime(primary.getReadTime() != null ? primary.getReadTime() : "4 min read")
                    .vocabCount(maxVocabCount)
                    .availableLevels(availableLevels)
                    .levelDetails(levelDetails)
                    .createdAt(primary.getCreatedAt())
                    .build();

            lessonList.add(dto);
        }

        // 3. Filter by category/topic if requested and not "All"
        if (topic != null && !topic.isBlank() && !topic.equalsIgnoreCase("All")) {
            lessonList = lessonList.stream()
                    .filter(l -> l.getCategory().equalsIgnoreCase(topic.trim()))
                    .collect(Collectors.toList());
        }

        // 4. Filter by level if requested and not "All"
        if (level != null && !level.isBlank() && !level.equalsIgnoreCase("All")) {
            lessonList = lessonList.stream()
                    .filter(l -> l.getAvailableLevels().contains(level.trim()))
                    .collect(Collectors.toList());
        }

        // 5. Filter by query search
        if (query != null && !query.isBlank()) {
            String qLower = query.toLowerCase().trim();
            lessonList = lessonList.stream()
                    .filter(l -> (l.getTitleEn() != null && l.getTitleEn().toLowerCase().contains(qLower))
                            || (l.getTitleVi() != null && l.getTitleVi().toLowerCase().contains(qLower))
                            || (l.getSummary() != null && l.getSummary().toLowerCase().contains(qLower))
                            || (l.getRegion() != null && l.getRegion().toLowerCase().contains(qLower))
                            || (l.getTopicGroup() != null && l.getTopicGroup().toLowerCase().contains(qLower)))
                    .collect(Collectors.toList());
        }

        // 6. Filter by region
        if (region != null && !region.isBlank()) {
            String rLower = region.toLowerCase().trim();
            lessonList = lessonList.stream()
                    .filter(l -> l.getRegion() != null && l.getRegion().toLowerCase().contains(rLower))
                    .collect(Collectors.toList());
        }

        // 7. Sort
        if ("vocab".equalsIgnoreCase(sortBy)) {
            lessonList.sort((a, b) -> Integer.compare(b.getVocabCount(), a.getVocabCount()));
        } else if ("time_asc".equalsIgnoreCase(sortBy)) {
            lessonList.sort(Comparator.comparingInt(this::extractMinutes));
        } else if ("time_desc".equalsIgnoreCase(sortBy)) {
            lessonList.sort((a, b) -> Integer.compare(extractMinutes(b), extractMinutes(a)));
        } else {
            // "recent" default
            lessonList.sort((a, b) -> {
                if (a.getCreatedAt() == null || b.getCreatedAt() == null) return 0;
                return b.getCreatedAt().compareTo(a.getCreatedAt());
            });
        }

        int totalElements = lessonList.size();
        int pageSize = (size != null && size > 0) ? size : (totalElements > 0 ? totalElements : 12);
        int pageIndex = (page != null && page >= 0) ? page : 0;
        int totalPages = (int) Math.ceil((double) totalElements / pageSize);

        List<CatalogLessonDto> pagedItems;
        int start = pageIndex * pageSize;
        if (start >= totalElements) {
            pagedItems = Collections.emptyList();
        } else {
            int end = Math.min(start + pageSize, totalElements);
            pagedItems = lessonList.subList(start, end);
        }

        return CatalogResponse.builder()
                .items(pagedItems)
                .page(pageIndex)
                .size(pageSize)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .topicCounts(topicCounts)
                .build();
    }

    private String normalizeCategory(String topic) {
        if (topic == null || topic.isBlank()) return "Heritage";
        String lower = topic.toLowerCase();
        if (lower.contains("ẩm thực") || lower.contains("cuisine") || lower.contains("food") || lower.contains("món")) {
            return "Cuisine";
        }
        if (lower.contains("làng nghề") || lower.contains("craft") || lower.contains("nghề") || lower.contains("gốm") || lower.contains("lụa")) {
            return "Crafts";
        }
        if (lower.contains("thiên nhiên") || lower.contains("nature") || lower.contains("hang") || lower.contains("vịnh") || lower.contains("núi")) {
            return "Nature";
        }
        if (lower.contains("phong tục") || lower.contains("dân gian") || lower.contains("folklore") || lower.contains("truyền thuyết") || lower.contains("lễ hội")) {
            return "Folklore";
        }
        return "Heritage";
    }

    private String getCategoryVi(String category) {
        switch (category) {
            case "Cuisine": return "Ẩm thực & Phong vị";
            case "Crafts": return "Làng nghề truyền thống";
            case "Nature": return "Thiên nhiên & Danh thắng";
            case "Folklore": return "Phong tục & Dân gian";
            default: return "Di sản văn hóa";
        }
    }

    private String mapCefrToLevelType(CefrLevel level) {
        if (level == null) return "Level 2";
        switch (level) {
            case A1:
            case A2:
                return "Level 1";
            case B1:
            case B2:
                return "Level 2";
            case C1:
            case C2:
                return "Level 3";
            default:
                return "Level 2";
        }
    }

    private int extractMinutes(CatalogLessonDto dto) {
        if (dto.getReadTime() == null) return 4;
        try {
            String digits = dto.getReadTime().replaceAll("[^0-9]", "");
            return digits.isEmpty() ? 4 : Integer.parseInt(digits);
        } catch (Exception e) {
            return 4;
        }
    }
}
