package com.viecultures.backend.dto;

import com.viecultures.backend.entity.DictionaryEntry;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DictionaryLookupDto {
    private String id;
    private String word;
    private String pos;
    private String ipa;
    private String viMeaning;
    private String enDefinition;
    private String contextSentence;
    private String audioUrl;
    private String level;
    private Integer searchCount;
    private boolean fromCache;

    public static DictionaryLookupDto fromEntity(DictionaryEntry entry, boolean fromCache, String fallbackContext) {
        return DictionaryLookupDto.builder()
                .id(entry.getId())
                .word(entry.getWord())
                .pos(entry.getPos())
                .ipa(entry.getIpa())
                .viMeaning(entry.getViMeaning())
                .enDefinition(entry.getEnDefinition())
                .contextSentence(entry.getContextSentence() != null && !entry.getContextSentence().isBlank()
                        ? entry.getContextSentence()
                        : fallbackContext)
                .audioUrl(entry.getAudioUrl())
                .level(entry.getLevel())
                .searchCount(entry.getSearchCount())
                .fromCache(fromCache)
                .build();
    }
}
