package com.viecultures.backend.dto.reflection;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserReflectionDto {
    private String id;
    private String postId;
    private String userId;
    private String authorName;
    private String authorRole;
    private String avatarText;
    private String content;
    @Builder.Default
    private List<String> usedVocabIds = new ArrayList<>();
    private Integer reactionsCount;
    private Boolean userLiked;
    private String status;
    private LocalDateTime createdAt;
    private Boolean isReported;
}
