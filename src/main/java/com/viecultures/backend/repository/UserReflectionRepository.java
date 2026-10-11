package com.viecultures.backend.repository;

import com.viecultures.backend.entity.ReflectionStatus;
import com.viecultures.backend.entity.UserReflection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserReflectionRepository extends JpaRepository<UserReflection, String> {
    List<UserReflection> findByReadingPostIdAndStatus(String postId, ReflectionStatus status);
    List<UserReflection> findByUserId(String userId);
}
