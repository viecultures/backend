package com.viecultures.backend.repository;

import com.viecultures.backend.entity.CefrLevel;
import com.viecultures.backend.entity.ReadingPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReadingPostRepository extends JpaRepository<ReadingPost, String> {

    @Query("SELECT p FROM ReadingPost p WHERE " +
           "(:topic IS NULL OR LOWER(p.topic) = LOWER(:topic)) AND " +
           "(:level IS NULL OR p.level = :level) AND " +
           "(:query IS NULL OR LOWER(p.titleEn) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(p.titleVi) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<ReadingPost> searchPosts(
            @Param("topic") String topic,
            @Param("level") CefrLevel level,
            @Param("query") String query
    );
}
