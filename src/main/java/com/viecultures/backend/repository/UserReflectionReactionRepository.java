package com.viecultures.backend.repository;

import com.viecultures.backend.entity.UserReflectionReaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserReflectionReactionRepository extends JpaRepository<UserReflectionReaction, String> {
    Optional<UserReflectionReaction> findByUserIdAndReflectionId(String userId, String reflectionId);
    boolean existsByUserIdAndReflectionId(String userId, String reflectionId);
    void deleteByUserIdAndReflectionId(String userId, String reflectionId);
    List<UserReflectionReaction> findByUserIdAndReflectionIdIn(String userId, Collection<String> reflectionIds);
}
