package com.viecultures.backend.modules.learning.service.impl;

import com.viecultures.backend.modules.learning.dto.CreateReflectionRequest;
import com.viecultures.backend.modules.learning.dto.ReactionResponse;
import com.viecultures.backend.modules.learning.dto.UserReflectionDto;
import com.viecultures.backend.entity.*;
import com.viecultures.backend.repository.ReadingPostRepository;
import com.viecultures.backend.repository.UserReflectionReactionRepository;
import com.viecultures.backend.repository.UserReflectionRepository;
import com.viecultures.backend.repository.UserRepository;
import com.viecultures.backend.modules.learning.service.UserReflectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserReflectionServiceImpl implements UserReflectionService {

    private final UserReflectionRepository userReflectionRepository;
    private final UserReflectionReactionRepository userReflectionReactionRepository;
    private final UserRepository userRepository;
    private final ReadingPostRepository readingPostRepository;

    // Passive moderation pattern: matches overt profanities, hate speech and spam links
    private static final Pattern MODERATION_PATTERN = Pattern.compile(
            "(?i)\\b(fuck|shit|bitch|bastard|asshole|cunt|dick|pussy|địt|đụ|lồn|cặc|buồi|chó đẻ|dkm|đkm|vcl|vcc)\\b|(https?://\\S+|www\\.\\S+)"
    );

    @Override
    @Transactional(readOnly = true)
    public List<UserReflectionDto> getReflectionsByPostId(String postId, String currentUserEmail) {
        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        List<UserReflection> allReflections = userReflectionRepository.findByReadingPostIdOrderByCreatedAtDesc(postId);

        // Community Policy: show PUBLISHED reflections to everyone; show PENDING_REVIEW only to author or ADMIN
        boolean isAdmin = "ROLE_ADMIN".equalsIgnoreCase(currentUser.getRole());
        List<UserReflection> visibleReflections = allReflections.stream()
                .filter(r -> r.getStatus() == ReflectionStatus.PUBLISHED
                        || isAdmin
                        || (r.getUser() != null && r.getUser().getId().equals(currentUser.getId())))
                .toList();

        if (visibleReflections.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> visibleIds = visibleReflections.stream().map(UserReflection::getId).toList();
        List<UserReflectionReaction> userReactions = userReflectionReactionRepository
                .findByUserIdAndReflectionIdIn(currentUser.getId(), visibleIds);
        Set<String> reactedReflectionIds = userReactions.stream()
                .map(r -> r.getReflection().getId())
                .collect(Collectors.toSet());

        return visibleReflections.stream()
                .map(r -> mapToDto(r, currentUser.getId(), reactedReflectionIds.contains(r.getId())))
                .toList();
    }

    @Override
    @Transactional
    public UserReflectionDto createReflection(CreateReflectionRequest request, String currentUserEmail) {
        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        ReadingPost post = readingPostRepository.findById(request.getPostId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reading post not found with id: " + request.getPostId()));

        String content = request.getContent().trim();
        if (content.length() < 10) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reflection must be at least 10 characters long");
        }

        // Passive Moderation Check (No-Judgment policy: Never judge grammar, only filter toxicity & spam)
        boolean hasSensitiveContent = MODERATION_PATTERN.matcher(content).find();
        ReflectionStatus initialStatus = hasSensitiveContent ? ReflectionStatus.PENDING_REVIEW : ReflectionStatus.PUBLISHED;

        if (hasSensitiveContent) {
            log.warn("[PassiveModeration] Reflection flagged for review by user={}", currentUser.getEmail());
        }

        UserReflection reflection = UserReflection.builder()
                .user(currentUser)
                .readingPost(post)
                .content(content)
                .usedVocabIds(request.getUsedVocabIds() != null ? new ArrayList<>(request.getUsedVocabIds()) : new ArrayList<>())
                .reactionsCount(0)
                .reportsCount(0)
                .status(initialStatus)
                .build();

        UserReflection saved = userReflectionRepository.save(reflection);
        log.info("[CommunityReflection] Created reflection id={} status={} for post={}", saved.getId(), saved.getStatus(), post.getId());

        return mapToDto(saved, currentUser.getId(), false);
    }

    @Override
    @Transactional
    public ReactionResponse toggleReaction(String reflectionId, String currentUserEmail) {
        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        UserReflection reflection = userReflectionRepository.findById(reflectionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reflection not found with id: " + reflectionId));

        Optional<UserReflectionReaction> existingReaction = userReflectionReactionRepository
                .findByUserIdAndReflectionId(currentUser.getId(), reflectionId);

        boolean userLiked;
        int currentCount = reflection.getReactionsCount() != null ? reflection.getReactionsCount() : 0;

        if (existingReaction.isPresent()) {
            // Unlike / remove reaction
            userReflectionReactionRepository.delete(existingReaction.get());
            int newCount = Math.max(0, currentCount - 1);
            reflection.setReactionsCount(newCount);
            userReflectionRepository.save(reflection);
            userLiked = false;
            log.info("[CommunityReflection] User {} unliked reflection {}", currentUser.getEmail(), reflectionId);
        } else {
            // Like / add reaction
            UserReflectionReaction reaction = UserReflectionReaction.builder()
                    .user(currentUser)
                    .reflection(reflection)
                    .build();
            userReflectionReactionRepository.save(reaction);
            int newCount = currentCount + 1;
            reflection.setReactionsCount(newCount);
            userReflectionRepository.save(reflection);
            userLiked = true;
            log.info("[CommunityReflection] User {} liked reflection {}", currentUser.getEmail(), reflectionId);
        }

        return ReactionResponse.builder()
                .reflectionId(reflectionId)
                .reactionsCount(reflection.getReactionsCount())
                .userLiked(userLiked)
                .build();
    }

    @Override
    @Transactional
    public void reportReflection(String reflectionId, String currentUserEmail) {
        User reflectionUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        UserReflection reflection = userReflectionRepository.findById(reflectionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reflection not found with id: " + reflectionId));

        int reports = (reflection.getReportsCount() != null ? reflection.getReportsCount() : 0) + 1;
        reflection.setReportsCount(reports);

        // Community report trigger: send to review queue
        if (reflection.getStatus() == ReflectionStatus.PUBLISHED) {
            reflection.setStatus(ReflectionStatus.PENDING_REVIEW);
            log.warn("[CommunityModeration] Reflection {} flagged via report by user {}. Status updated to PENDING_REVIEW.",
                    reflectionId, reflectionUser.getEmail());
        }

        userReflectionRepository.save(reflection);
    }

    private UserReflectionDto mapToDto(UserReflection reflection, String currentUserId, boolean userLiked) {
        User author = reflection.getUser();
        String authorName = (author != null && author.getName() != null) ? author.getName() : "VieCultures learner";
        String authorRole = (author != null && "ROLE_ADMIN".equalsIgnoreCase(author.getRole()))
                ? "VieCultures Admin"
                : "VieCultures learner";
        String avatarText = generateAvatarInitials(authorName);

        boolean isReported = reflection.getReportsCount() != null && reflection.getReportsCount() > 0;

        return UserReflectionDto.builder()
                .id(reflection.getId())
                .postId(reflection.getReadingPost() != null ? reflection.getReadingPost().getId() : null)
                .userId(author != null ? author.getId() : null)
                .authorName(authorName)
                .authorRole(authorRole)
                .avatarText(avatarText)
                .content(reflection.getContent())
                .usedVocabIds(reflection.getUsedVocabIds() != null ? new ArrayList<>(reflection.getUsedVocabIds()) : new ArrayList<>())
                .reactionsCount(reflection.getReactionsCount() != null ? reflection.getReactionsCount() : 0)
                .userLiked(userLiked)
                .status(reflection.getStatus() != null ? reflection.getStatus().name() : "PUBLISHED")
                .createdAt(reflection.getCreatedAt())
                .isReported(isReported)
                .build();
    }

    private String generateAvatarInitials(String name) {
        if (name == null || name.isBlank()) {
            return "VC";
        }
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        }
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }
}
