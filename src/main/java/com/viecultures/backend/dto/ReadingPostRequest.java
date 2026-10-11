package com.viecultures.backend.dto;

import com.viecultures.backend.entity.CefrLevel;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReadingPostRequest {
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
    private CefrLevel level;
    private String readTime;
    private Boolean featured;
    private List<ParagraphPairDto> paragraphs;
    private List<VocabItemDto> vocabularies;

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
}
