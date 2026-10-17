package com.viecultures.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "dictionary_entries", indexes = {
        @Index(name = "idx_dict_word", columnList = "word")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DictionaryEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true, length = 120)
    private String word;

    @Column(length = 60)
    private String pos;

    @Column(length = 100)
    private String ipa;

    @Column(name = "vi_meaning", columnDefinition = "TEXT")
    private String viMeaning;

    @Column(name = "en_definition", columnDefinition = "TEXT")
    private String enDefinition;

    @Column(name = "context_sentence", columnDefinition = "TEXT")
    private String contextSentence;

    @Column(name = "audio_url")
    private String audioUrl;

    @Column(length = 20)
    private String level;

    @Builder.Default
    @Column(name = "search_count")
    private Integer searchCount = 1;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
