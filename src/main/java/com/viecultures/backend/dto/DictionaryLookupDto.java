package com.viecultures.backend.dto;

import com.viecultures.backend.entity.DictionaryEntry;
import com.viecultures.backend.entity.ReadingPost;
import com.viecultures.backend.entity.VocabItem;
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

    private String postId;
    private String postTitleEn;
    private String postTitleVi;
    private String journeyId;
    private String journeyTitleEn;
    private String usageNote;

    public static DictionaryLookupDto fromEntity(DictionaryEntry entry) {
        return fromEntity(entry, true, "");
    }

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

    public static DictionaryLookupDto fromVocabItem(VocabItem item) {
        ReadingPost post = item.getReadingPost();
        return DictionaryLookupDto.builder()
                .id(item.getId())
                .word(item.getWord())
                .pos(item.getPos())
                .ipa(item.getIpa())
                .viMeaning(item.getViMeaning())
                .enDefinition("")
                .contextSentence(item.getContextSentence())
                .audioUrl(item.getAudioUrl())
                .level(item.getLevel() != null ? item.getLevel().name() : null)
                .usageNote(item.getUsageNote())
                .postId(post != null ? post.getId() : null)
                .postTitleEn(post != null ? post.getTitleEn() : null)
                .postTitleVi(post != null ? post.getTitleVi() : null)
                .journeyId(post != null && post.getJourney() != null ? post.getJourney().getId() : null)
                .journeyTitleEn(post != null && post.getJourney() != null ? post.getJourney().getTitleEn() : null)
                .searchCount(1)
                .fromCache(true)
                .build();
    }
}
