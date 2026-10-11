package com.viecultures.backend.repository;

import com.viecultures.backend.entity.ParagraphPair;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ParagraphPairRepository extends JpaRepository<ParagraphPair, String> {
    List<ParagraphPair> findByReadingPostIdOrderByOrderIndexAsc(String postId);
}
