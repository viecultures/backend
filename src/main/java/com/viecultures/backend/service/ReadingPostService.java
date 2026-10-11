package com.viecultures.backend.service;

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

import java.util.ArrayList;
import java.util.List;
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
}
