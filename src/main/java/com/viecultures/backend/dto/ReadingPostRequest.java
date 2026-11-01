package com.viecultures.backend.dto;

import com.viecultures.backend.entity.CefrLevel;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReadingPostRequest {
    private String id; // Explicit slug ID (e.g. "pho-hanoi-history", "tet-holiday") or null to auto-generate
    private String journeyId; // Link to LearningJourney (nullable for standalone posts)
    private Integer chapterOrder; // Order in journey (1, 2, 3...)
    private String provinceId; // Province identifier (e.g. "ha-noi")
    private String topicGroup;
    private String titleEn;
    private String titleVi;
    private String topic;
    private String region;
    private String imageUrl;
    private Integer wordCount;
    private String summary;
    private String contentEn;
    private String contentVi;
    private String shadowingAudioUrl;
    private String audioTracks;
    private CefrLevel level;
    private String readTime;
    private Boolean featured;
    private List<ParagraphPairDto> paragraphs;
    private List<VocabItemDto> vocabularies;
    private List<QuizQuestionDto> questions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParagraphPairDto {
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
        private String word;
        private String ipa;
        private String pos;
        private String viMeaning;
        private String contextSentence;
        private String audioUrl;
        private CefrLevel level;
        private String usageNote;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuizQuestionDto {
        private Integer orderIndex;
        private String question;
        private String options;
        private String correctKey;
        private String explanation;
    }
}
