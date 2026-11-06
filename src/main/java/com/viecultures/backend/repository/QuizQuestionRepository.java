package com.viecultures.backend.repository;

import com.viecultures.backend.entity.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, String> {
    List<QuizQuestion> findByReadingPostIdOrderByOrderIndexAsc(String postId);

    @Query("SELECT q.readingPost.id, COUNT(q.id) FROM QuizQuestion q GROUP BY q.readingPost.id")
    List<Object[]> countQuestionsGroupedByPostId();

    @Query("SELECT q.readingPost.id, COUNT(q.id) FROM QuizQuestion q WHERE q.readingPost.id IN :postIds GROUP BY q.readingPost.id")
    List<Object[]> countQuestionsByPostIds(@Param("postIds") Collection<String> postIds);
}
