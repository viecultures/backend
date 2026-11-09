package com.viecultures.backend.dto;

import com.viecultures.backend.entity.CefrLevel;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReadingPostResponse {
    private String id;
    private String journeyId;
    private String journeyTitleEn;
    private Integer chapterOrder;
    private String provinceId;
    private String topicGroup;
    private String titleEn;
    private String titleVi;
    private String topic;
    private String region;
    private String imageUrl;
    private Integer wordCount;
    private String summary;
    private String summaryVi;
    private String contentEn;
    private String contentVi;
    private String shadowingAudioUrl;
    private String audioTracks;
    private CefrLevel level;
    private String readTime;
    private Integer vocabCount;
    private Integer questionCount;
    private Integer totalReads;
    private Integer likesCount;
    private Boolean featured;
    private LocalDateTime createdAt;
    private List<ParagraphPairDto> paragraphs;
    private List<VocabItemDto> vocabularies;
    private List<QuizQuestionDto> questions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParagraphPairDto {
        private String id;
        private Integer orderIndex;
        private String enText;
        private String viText;
        private Float audioTimestampStart;
        private Float audioTimestampEnd;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VocabItemDto {
        private String id;
        private String word;
        private String pos;
        private String ipa;
        private String viMeaning;
        private String contextSentence;
        private String enDefinition;
        private String audioUrl;
        private CefrLevel level;
        private String usageNote;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuizQuestionDto {
        private String id;
        private Integer orderIndex;
        private String question;
        private String options;
        private String correctKey;
        private String explanation;

        // Bilingual question fields for compatibility
        private Integer questionOrder;
        private String questionEn;
        private String questionVi;
        private String optionA;
        private String optionB;
        private String optionC;
        private String optionD;
        private String correctOption;
        private String explanationEn;
        private String explanationVi;
    }
}
