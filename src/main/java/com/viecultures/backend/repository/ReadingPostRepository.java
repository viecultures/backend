package com.viecultures.backend.repository;

import com.viecultures.backend.entity.ReadingPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReadingPostRepository extends JpaRepository<ReadingPost, String> {
    List<ReadingPost> findByTopicIgnoreCase(String topic);
}
