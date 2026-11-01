package com.viecultures.backend.service;

import com.viecultures.backend.dto.reflection.CreateReflectionRequest;
import com.viecultures.backend.dto.reflection.ReactionResponse;
import com.viecultures.backend.dto.reflection.UserReflectionDto;

import java.util.List;

public interface UserReflectionService {
    List<UserReflectionDto> getReflectionsByPostId(String postId, String currentUserEmail);
    UserReflectionDto createReflection(CreateReflectionRequest request, String currentUserEmail);
    ReactionResponse toggleReaction(String reflectionId, String currentUserEmail);
    void reportReflection(String reflectionId, String currentUserEmail);
}
