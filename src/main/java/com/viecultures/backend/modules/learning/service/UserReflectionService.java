package com.viecultures.backend.modules.learning.service;

import com.viecultures.backend.modules.learning.dto.CreateReflectionRequest;
import com.viecultures.backend.modules.learning.dto.ReactionResponse;
import com.viecultures.backend.modules.learning.dto.UserReflectionDto;

import java.util.List;

public interface UserReflectionService {
    List<UserReflectionDto> getReflectionsByPostId(String postId, String currentUserEmail);
    UserReflectionDto createReflection(CreateReflectionRequest request, String currentUserEmail);
    ReactionResponse toggleReaction(String reflectionId, String currentUserEmail);
    void reportReflection(String reflectionId, String currentUserEmail);
}
