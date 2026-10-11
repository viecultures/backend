package com.viecultures.backend.repository;

import com.viecultures.backend.entity.VocabItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VocabItemRepository extends JpaRepository<VocabItem, String> {
    List<VocabItem> findByReadingPostId(String postId);
}
