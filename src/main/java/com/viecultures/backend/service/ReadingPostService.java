package com.viecultures.backend.service;

import com.viecultures.backend.dto.CatalogLessonDto;
import com.viecultures.backend.dto.CatalogResponse;
import com.viecultures.backend.dto.ReadingPostRequest;
import com.viecultures.backend.dto.ReadingPostResponse;
import com.viecultures.backend.entity.CefrLevel;
import com.viecultures.backend.entity.ParagraphPair;
import com.viecultures.backend.entity.ReadingPost;
import com.viecultures.backend.entity.VocabItem;
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
    private final ParagraphPairRepository paragraphPairRepository;
    private final VocabItemRepository vocabItemRepository;

    @Transactional(readOnly = true)
    public List<ReadingPostResponse> getPosts(String topic, CefrLevel level, String query, String region, String topicGroup) {
        Specification<ReadingPost> spec = (root, q, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (topic != null && !topic.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("topic")), topic.toLowerCase().trim()));
            }

            if (region != null && !region.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("region")), region.toLowerCase().trim()));
            }

            if (topicGroup != null && !topicGroup.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("topicGroup")), topicGroup.toLowerCase().trim()));
            }

            if (level != null) {
                predicates.add(cb.equal(root.get("level"), level));
            }

            if (query != null && !query.isBlank()) {
                String pattern = "%" + query.toLowerCase().trim() + "%";
                Predicate matchEn = cb.like(cb.lower(root.get("titleEn")), pattern);
                Predicate matchVi = cb.like(cb.lower(root.get("titleVi")), pattern);
                Predicate matchTopicGroup = cb.like(cb.lower(root.get("topicGroup")), pattern);
                predicates.add(cb.or(matchEn, matchVi, matchTopicGroup));
            }

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
                .orElseThrow(() -> new RuntimeException("Bài đọc không tồn tại với ID: " + id));
        return toResponse(post, true);
    }

    @Transactional
    public ReadingPostResponse createPost(ReadingPostRequest request) {
        ReadingPost post = ReadingPost.builder()
                .topicGroup(request.getTopicGroup())
                .titleEn(request.getTitleEn())
                .titleVi(request.getTitleVi())
                .topic(request.getTopic())
                .region(request.getRegion())
                .imageUrl(request.getImageUrl())
                .wordCount(request.getWordCount())
                .summary(request.getSummary())
                .contentEn(request.getContentEn())
                .contentVi(request.getContentVi())
                .shadowingAudioUrl(request.getShadowingAudioUrl())
                .level(request.getLevel())
                .readTime(request.getReadTime())
                .featured(Boolean.TRUE.equals(request.getFeatured()))
                .build();

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
        post.setImageUrl(request.getImageUrl());
        post.setWordCount(request.getWordCount());
        post.setSummary(request.getSummary());
        post.setContentEn(request.getContentEn());
        post.setContentVi(request.getContentVi());
        post.setShadowingAudioUrl(request.getShadowingAudioUrl());
        post.setLevel(request.getLevel());
        post.setReadTime(request.getReadTime());
        if (request.getFeatured() != null) {
            post.setFeatured(request.getFeatured());
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
                .imageUrl(post.getImageUrl())
                .wordCount(post.getWordCount())
                .summary(post.getSummary())
                .contentEn(post.getContentEn())
                .contentVi(post.getContentVi())
                .shadowingAudioUrl(post.getShadowingAudioUrl())
                .level(post.getLevel())
                .readTime(post.getReadTime())
                .totalReads(post.getTotalReads())
                .likesCount(post.getLikesCount())
                .featured(Boolean.TRUE.equals(post.getFeatured()))
                .createdAt(post.getCreatedAt());

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
        }

        return builder.build();
    }

    @Transactional(readOnly = true)
    public CatalogResponse getCatalog(String topic, String level, String query, String region, String sortBy, Integer page, Integer size) {
        List<ReadingPost> allPosts = readingPostRepository.findAll();

        // 1. Group posts by topicGroup or individual id
        Map<String, List<ReadingPost>> groups = new LinkedHashMap<>();
        for (ReadingPost post : allPosts) {
            String key = (post.getTopicGroup() != null && !post.getTopicGroup().isBlank())
                    ? "group:" + post.getTopicGroup().trim().toLowerCase()
                    : "id:" + post.getId();
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(post);
        }

        // 2. Compute topicCounts map across all unique grouped lessons
        Map<String, Long> topicCounts = new LinkedHashMap<>();
        topicCounts.put("All", (long) groups.size());
        topicCounts.put("Heritage", 0L);
        topicCounts.put("Cuisine", 0L);
        topicCounts.put("Crafts", 0L);
        topicCounts.put("Nature", 0L);
        topicCounts.put("Folklore", 0L);

        List<CatalogLessonDto> lessonList = new ArrayList<>();

        for (List<ReadingPost> groupPosts : groups.values()) {
            ReadingPost primary = groupPosts.stream()
                    .filter(p -> p.getLevel() == CefrLevel.B1 || p.getLevel() == CefrLevel.B2)
                    .findFirst()
                    .orElse(groupPosts.get(0));

            String category = normalizeCategory(primary.getTopic());
            topicCounts.put(category, topicCounts.getOrDefault(category, 0L) + 1L);

            List<String> availableLevels = new ArrayList<>();
            Map<String, CatalogLessonDto.LevelDetailDto> levelDetails = new LinkedHashMap<>();

            for (ReadingPost p : groupPosts) {
                String levelType = mapCefrToLevelType(p.getLevel());
                if (!availableLevels.contains(levelType)) {
                    availableLevels.add(levelType);
                }

                int vocabCount = (p.getVocabularies() != null) ? p.getVocabularies().size() : 0;
                String readTimeStr = (p.getReadTime() != null && !p.getReadTime().isBlank())
                        ? p.getReadTime()
                        : Math.max(2, (int) Math.ceil((p.getWordCount() != null ? p.getWordCount() : 250) / 100.0)) + " min read";

                CatalogLessonDto.LevelDetailDto detail = CatalogLessonDto.LevelDetailDto.builder()
                        .postId(p.getId())
                        .cefrLevel(p.getLevel() != null ? p.getLevel().name() : "B1")
                        .readTime(readTimeStr)
                        .vocabCount(vocabCount)
                        .wordCount(p.getWordCount())
                        .summary(p.getSummary())
                        .build();

                levelDetails.put(levelType, detail);
            }

            availableLevels.sort(String::compareTo);

            int maxVocabCount = levelDetails.values().stream()
                    .mapToInt(CatalogLessonDto.LevelDetailDto::getVocabCount)
                    .max()
                    .orElse(0);

            boolean isFeatured = groupPosts.stream().anyMatch(p -> Boolean.TRUE.equals(p.getFeatured()));
            int totalReads = groupPosts.stream().mapToInt(p -> p.getTotalReads() != null ? p.getTotalReads() : 0).sum();
            int totalLikes = groupPosts.stream().mapToInt(p -> p.getLikesCount() != null ? p.getLikesCount() : 0).sum();

            CatalogLessonDto dto = CatalogLessonDto.builder()
                    .id(primary.getId())
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
