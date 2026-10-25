package com.viecultures.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "companion_chat_messages", indexes = {
        @Index(name = "idx_companion_client_lesson", columnList = "client_identifier, lesson_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanionChatMessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "client_identifier", nullable = false)
    private String clientIdentifier;

    @Column(name = "lesson_id")
    private String lessonId;

    @Column(nullable = false)
    private String role;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "translated_content", columnDefinition = "TEXT")
    private String translatedContent;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
