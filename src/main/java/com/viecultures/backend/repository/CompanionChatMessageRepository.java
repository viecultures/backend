package com.viecultures.backend.repository;

import com.viecultures.backend.entity.CompanionChatMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompanionChatMessageRepository extends JpaRepository<CompanionChatMessageEntity, String> {

    List<CompanionChatMessageEntity> findByClientIdentifierAndLessonIdOrderByCreatedAtAsc(String clientIdentifier, String lessonId);

    List<CompanionChatMessageEntity> findByClientIdentifierOrderByCreatedAtAsc(String clientIdentifier);

    @Modifying
    @Query("DELETE FROM CompanionChatMessageEntity m WHERE m.clientIdentifier = :clientIdentifier AND m.lessonId = :lessonId")
    void deleteByClientIdentifierAndLessonId(@Param("clientIdentifier") String clientIdentifier, @Param("lessonId") String lessonId);

    @Modifying
    @Query("DELETE FROM CompanionChatMessageEntity m WHERE m.clientIdentifier = :clientIdentifier")
    void deleteByClientIdentifier(@Param("clientIdentifier") String clientIdentifier);
}
