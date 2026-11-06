package com.viecultures.backend.service.impl;

import com.viecultures.backend.dto.admin.AdminUserSummaryDto;
import com.viecultures.backend.dto.admin.LearnerCompletedLessonDto;
import com.viecultures.backend.dto.admin.LearnerDetailDto;
import com.viecultures.backend.dto.admin.SubscriptionTransactionDto;
import com.viecultures.backend.entity.ReadingPost;
import com.viecultures.backend.entity.SubscriptionTransaction;
import com.viecultures.backend.entity.User;
import com.viecultures.backend.entity.UserActivityLog;
import com.viecultures.backend.entity.UserSeal;
import com.viecultures.backend.entity.UserStreak;
import com.viecultures.backend.entity.VocabProgressStatus;
import com.viecultures.backend.repository.ReadingPostRepository;
import com.viecultures.backend.repository.SubscriptionTransactionRepository;
import com.viecultures.backend.repository.UserActivityLogRepository;
import com.viecultures.backend.repository.UserRepository;
import com.viecultures.backend.repository.UserSealRepository;
import com.viecultures.backend.repository.UserStreakRepository;
import com.viecultures.backend.repository.UserVocabProgressRepository;
import com.viecultures.backend.service.AdminUserService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;
    private final UserVocabProgressRepository userVocabProgressRepository;
    private final SubscriptionTransactionRepository subscriptionTransactionRepository;
    private final UserSealRepository userSealRepository;
    private final ReadingPostRepository readingPostRepository;
    private final UserStreakRepository userStreakRepository;
    private final UserActivityLogRepository userActivityLogRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<AdminUserSummaryDto> getUsers(String query, String role, Boolean enabled, Pageable pageable) {
        return getUsers(query, role, enabled, null, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminUserSummaryDto> getUsers(String query, String role, Boolean enabled, String filterGroup, Pageable pageable) {
        String cleanQuery = (query != null && !query.trim().isEmpty()) ? query.trim() : null;
        String cleanRole = (role != null && !role.trim().isEmpty()) ? role.trim() : null;
        String cleanFilterGroup = (filterGroup != null && !filterGroup.trim().isEmpty()) ? filterGroup.trim() : null;

        if (cleanFilterGroup == null || "all".equalsIgnoreCase(cleanFilterGroup)) {
            Page<User> users = userRepository.searchUsers(cleanQuery, cleanRole, enabled, pageable);
            return users.map(this::toDto);
        }

        Sort sort = (pageable != null && pageable.getSort().isSorted())
                ? pageable.getSort()
                : Sort.by(Sort.Direction.DESC, "createdAt");
        List<User> userList = userRepository.searchUsersList(cleanQuery, cleanRole, enabled, sort);

        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        List<AdminUserSummaryDto> filtered = userList.stream()
                .map(this::toDto)
                .filter(dto -> matchesFilterGroup(dto, cleanFilterGroup, sevenDaysAgo))
                .toList();

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filtered.size());
        List<AdminUserSummaryDto> subList = (start <= end && start < filtered.size())
                ? filtered.subList(start, end)
                : Collections.emptyList();

        return new PageImpl<>(subList, pageable, filtered.size());
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportUsersCsv(String query, String role, Boolean enabled, String filterGroup) {
        log.info("Generating CSV export for users: query={}, role={}, enabled={}, filterGroup={}",
                query, role, enabled, filterGroup);
        try {
            String cleanQuery = (query != null && !query.trim().isEmpty()) ? query.trim() : null;
            String cleanRole = (role != null && !role.trim().isEmpty()) ? role.trim() : null;
            String cleanFilterGroup = (filterGroup != null && !filterGroup.trim().isEmpty()) ? filterGroup.trim() : null;

            List<User> userList = userRepository.searchUsersList(
                    cleanQuery, cleanRole, enabled, Sort.by(Sort.Direction.DESC, "createdAt"));

            LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
            List<AdminUserSummaryDto> dtos = userList.stream()
                    .map(this::toDto)
                    .filter(dto -> matchesFilterGroup(dto, cleanFilterGroup, sevenDaysAgo))
                    .toList();

            StringBuilder sb = new StringBuilder();
            // BOM for UTF-8 in Excel
            sb.append('\uFEFF');

            // Header: 'ID', 'Họ và tên', 'Email', 'Vai trò', 'Trình độ CEFR', 'Từ đã lưu', 'Phương thức đăng nhập', 'Ngày tạo', 'Trạng thái'
            sb.append("ID,Họ và tên,Email,Vai trò,Trình độ CEFR,Từ đã lưu,Phương thức đăng nhập,Ngày tạo,Trạng thái\r\n");

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

            for (AdminUserSummaryDto u : dtos) {
                String roleDisplay = "ROLE_ADMIN".equalsIgnoreCase(u.getRole()) || "ADMIN".equalsIgnoreCase(u.getRole())
                        ? "Quản trị viên"
                        : "Học viên";
                String cefrDisplay = u.getEnglishLevel() != null ? u.getEnglishLevel().name() : "Chưa xác định";
                String statusDisplay = u.isEnabled() ? "Đang hoạt động" : "Đã khóa";
                String createdAtDisplay = u.getCreatedAt() != null ? u.getCreatedAt().format(dtf) : "";

                sb.append(escapeCsv(u.getId())).append(',')
                  .append(escapeCsv(u.getName())).append(',')
                  .append(escapeCsv(u.getEmail())).append(',')
                  .append(escapeCsv(roleDisplay)).append(',')
                  .append(escapeCsv(cefrDisplay)).append(',')
                  .append(u.getSavedVocabCount()).append(',')
                  .append(escapeCsv(u.getAuthProvider())).append(',')
                  .append(escapeCsv(createdAtDisplay)).append(',')
                  .append(escapeCsv(statusDisplay)).append("\r\n");
            }

            return sb.toString().getBytes(StandardCharsets.UTF_8);
        } catch (Exception ex) {
            log.error("Error occurred while generating users CSV export: {}", ex.getMessage(), ex);
            return ("\uFEFFID,Họ và tên,Email,Vai trò,Trình độ CEFR,Từ đã lưu,Phương thức đăng nhập,Ngày tạo,Trạng thái\r\n")
                    .getBytes(StandardCharsets.UTF_8);
        }
    }

    private boolean matchesFilterGroup(AdminUserSummaryDto dto, String filterGroup, LocalDateTime sevenDaysAgo) {
        if (filterGroup == null || filterGroup.isBlank() || "all".equalsIgnoreCase(filterGroup)) {
            return true;
        }
        String fg = filterGroup.trim().toLowerCase();
        switch (fg) {
            case "recent_7d":
                return dto.getCreatedAt() != null && dto.getCreatedAt().isAfter(sevenDaysAgo);
            case "admin":
                return dto.getRole() != null && dto.getRole().toUpperCase().contains("ADMIN");
            case "active":
                return dto.getSavedVocabCount() > 0 || dto.isEnabled();
            case "inactive":
                return dto.getSavedVocabCount() == 0 || !dto.isEnabled();
            default:
                return true;
        }
    }

    private String escapeCsv(Object val) {
        if (val == null) return "";
        String str = String.valueOf(val);
        if (str.contains(",") || str.contains("\"") || str.contains("\n") || str.contains("\r")) {
            return "\"" + str.replace("\"", "\"\"") + "\"";
        }
        return str;
    }

    @Override
    @Transactional(readOnly = true)
    public AdminUserSummaryDto getUserById(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Người dùng không tồn tại với ID: " + id));
        return toDto(user);
    }

    @Override
    @Transactional(readOnly = true)
    public LearnerDetailDto getLearnerDetails(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người dùng với ID: " + userId));

        // 1. Danh sách giao dịch & xác định gói cước hiện tại
        List<SubscriptionTransaction> txList = subscriptionTransactionRepository
                .findByUserIdOrUserEmailOrderByCreatedAtDesc(userId, user.getEmail());

        List<SubscriptionTransactionDto> transactionDtos = txList.stream()
                .map(this::mapToTransactionDto)
                .toList();

        SubscriptionTransaction latestApprovedTx = txList.stream()
                .filter(t -> "APPROVED".equalsIgnoreCase(t.getStatus()))
                .findFirst()
                .orElse(null);

        String currentPlanCode = "FREE";
        String currentPlanName = "Gói Tiêu Chuẩn Miễn Phí";
        String subscriptionStatus = "ACTIVE";
        LocalDateTime subscriptionExpiry = null;

        if (latestApprovedTx != null) {
            currentPlanCode = latestApprovedTx.getPlanCode() != null ? latestApprovedTx.getPlanCode() : "FREE";
            currentPlanName = latestApprovedTx.getPlanName() != null ? latestApprovedTx.getPlanName() : "Gói Tiêu Chuẩn Miễn Phí";

            if ("LIFETIME".equalsIgnoreCase(currentPlanCode)) {
                subscriptionStatus = "ACTIVE";
                subscriptionExpiry = null;
            } else {
                LocalDateTime baseDate = latestApprovedTx.getApprovedAt() != null
                        ? latestApprovedTx.getApprovedAt()
                        : latestApprovedTx.getCreatedAt();
                if (baseDate != null) {
                    subscriptionExpiry = baseDate.plusDays(30);
                    subscriptionStatus = subscriptionExpiry.isBefore(LocalDateTime.now()) ? "INACTIVE" : "ACTIVE";
                } else {
                    subscriptionStatus = "ACTIVE";
                }
            }
        }

        // 2. Danh sách bài học đã niêm ấn (Sealed Lessons)
        List<UserSeal> seals = userSealRepository.findByUserIdOrderBySealedAtDesc(userId);
        long totalSealedLessons = seals.size();

        Set<String> postIds = seals.stream()
                .map(UserSeal::getReadingPostId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<String, ReadingPost> postMap = postIds.isEmpty()
                ? Collections.emptyMap()
                : readingPostRepository.findAllById(postIds).stream()
                        .collect(Collectors.toMap(ReadingPost::getId, Function.identity(), (p1, p2) -> p1));

        List<LearnerCompletedLessonDto> completedLessons = seals.stream()
                .map(seal -> {
                    ReadingPost post = postMap.get(seal.getReadingPostId());
                    String titleEn = (post != null && post.getTitleEn() != null) ? post.getTitleEn() : "Lesson " + seal.getReadingPostId();
                    String titleVi = (post != null && post.getTitleVi() != null) ? post.getTitleVi() : "Bài học " + seal.getReadingPostId();
                    return LearnerCompletedLessonDto.builder()
                            .readingPostId(seal.getReadingPostId())
                            .titleEn(titleEn)
                            .titleVi(titleVi)
                            .sealedAt(seal.getSealedAt())
                            .wordsLearnedCount(seal.getWordsLearnedCount() != null ? seal.getWordsLearnedCount() : 0)
                            .build();
                })
                .toList();

        // 3. Chuỗi học tập (User streak)
        Optional<UserStreak> streakOpt = userStreakRepository.findByUserId(userId);
        if (streakOpt.isEmpty() && user.getEmail() != null) {
            streakOpt = userStreakRepository.findByUserEmail(user.getEmail());
        }
        Integer streakDays = streakOpt.map(UserStreak::getStreakDays).orElse(0);
        LocalDate lastActiveDate = streakOpt.map(UserStreak::getLastActiveDate).orElse(null);

        // 4. Thống kê từ vựng từ UserVocabProgressRepository
        long totalWordsLearned = 0;
        long learningVocabCount = 0;
        long masteredVocabCount = 0;
        try {
            totalWordsLearned = userVocabProgressRepository.countByUserId(userId);
            learningVocabCount = userVocabProgressRepository.countByUserIdAndStatus(userId, VocabProgressStatus.LEARNING);
            masteredVocabCount = userVocabProgressRepository.countByUserIdAndStatus(userId, VocabProgressStatus.MASTERED);
        } catch (Exception e) {
            log.warn("Could not calculate vocab progress stats for userId {}: {}", userId, e.getMessage());
        }

        return LearnerDetailDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .authProvider(user.getAuthProvider())
                .enabled(user.isEnabled())
                .englishLevel(user.getEnglishLevel())
                .createdAt(user.getCreatedAt())
                .currentPlanCode(currentPlanCode)
                .currentPlanName(currentPlanName)
                .subscriptionStatus(subscriptionStatus)
                .subscriptionExpiry(subscriptionExpiry)
                .transactions(transactionDtos)
                .streakDays(streakDays)
                .lastActiveDate(lastActiveDate)
                .totalSealedLessons(totalSealedLessons)
                .totalWordsLearned(totalWordsLearned)
                .learningVocabCount(learningVocabCount)
                .masteredVocabCount(masteredVocabCount)
                .completedLessons(completedLessons)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserActivityLog> getLearnerActivityLogs(String userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người dùng với ID: " + userId));

        return userActivityLogRepository.findByUserIdOrUserEmailOrderByCreatedAtDesc(user.getId(), user.getEmail(), pageable);
    }

    private static final java.util.Set<String> ALLOWED_ROLES = java.util.Set.of("ROLE_USER", "ROLE_ADMIN");

    @Override
    @Transactional
    public AdminUserSummaryDto updateUserStatus(String id, boolean enabled) {
        return updateUserStatus(id, enabled, null);
    }

    @Override
    @Transactional
    public AdminUserSummaryDto updateUserStatus(String id, boolean enabled, String currentAdminEmail) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Người dùng không tồn tại với ID: " + id));

        String adminEmail = resolveCurrentAdminEmail(currentAdminEmail);

        // Ngan chan admin tu khoa tai khoan cua chinh minh
        if (!enabled && adminEmail != null && adminEmail.equalsIgnoreCase(user.getEmail())) {
            throw new IllegalStateException("Bạn không thể tự khóa tài khoản quản trị của chính mình để tránh mất quyền truy cập hệ thống.");
        }

        user.setEnabled(enabled);
        User saved = userRepository.save(user);
        log.info("Admin updated user status: id={}, email={}, enabled={}", id, user.getEmail(), enabled);
        return toDto(saved);
    }

    @Override
    @Transactional
    public AdminUserSummaryDto updateUserRole(String id, String role) {
        return updateUserRole(id, role, null);
    }

    @Override
    @Transactional
    public AdminUserSummaryDto updateUserRole(String id, String role, String currentAdminEmail) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Người dùng không tồn tại với ID: " + id));

        if (role == null || role.trim().isEmpty()) {
            throw new IllegalArgumentException("Vai trò không được để trống");
        }

        String normalizedRole = role.trim().toUpperCase();
        if (!normalizedRole.startsWith("ROLE_")) {
            normalizedRole = "ROLE_" + normalizedRole;
        }

        if (!ALLOWED_ROLES.contains(normalizedRole)) {
            throw new IllegalArgumentException("Vai trò không hợp lệ: '" + role + "'. Hệ thống chỉ chấp nhận: " + ALLOWED_ROLES);
        }

        String adminEmail = resolveCurrentAdminEmail(currentAdminEmail);

        // Ngan chan admin tu tuoc quyen ROLE_ADMIN cua chinh minh
        if (adminEmail != null && adminEmail.equalsIgnoreCase(user.getEmail())) {
            boolean isCurrentlyAdmin = "ROLE_ADMIN".equalsIgnoreCase(user.getRole()) || "ADMIN".equalsIgnoreCase(user.getRole());
            if (isCurrentlyAdmin && !"ROLE_ADMIN".equalsIgnoreCase(normalizedRole)) {
                throw new IllegalStateException("Bạn không thể tự tước quyền Quản trị viên (ROLE_ADMIN) của chính mình để tránh mất quyền quản trị.");
            }
        }

        user.setRole(normalizedRole);
        User saved = userRepository.save(user);
        log.info("Admin updated user role: id={}, email={}, role={}", id, user.getEmail(), normalizedRole);
        return toDto(saved);
    }

    private String resolveCurrentAdminEmail(String providedEmail) {
        if (providedEmail != null && !providedEmail.trim().isEmpty()) {
            return providedEmail.trim();
        }
        try {
            org.springframework.security.core.Authentication auth =
                    org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null) {
                if (auth.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails ud) {
                    return ud.getUsername();
                } else if (auth.getPrincipal() instanceof String principalStr) {
                    return principalStr;
                }
            }
        } catch (Exception ex) {
            log.debug("Unable to resolve current admin email from SecurityContext: {}", ex.getMessage());
        }
        return null;
    }

    private SubscriptionTransactionDto mapToTransactionDto(SubscriptionTransaction tx) {
        return SubscriptionTransactionDto.builder()
                .id(tx.getId())
                .transactionCode(tx.getTransactionCode())
                .userId(tx.getUserId())
                .userEmail(tx.getUserEmail())
                .userName(tx.getUserName())
                .planCode(tx.getPlanCode())
                .planName(tx.getPlanName())
                .amountVnd(tx.getAmountVnd())
                .transferContent(tx.getTransferContent())
                .status(tx.getStatus())
                .note(tx.getNote())
                .createdAt(tx.getCreatedAt())
                .approvedAt(tx.getApprovedAt())
                .build();
    }

    private AdminUserSummaryDto toDto(User user) {
        long vocabCount = 0;
        try {
            vocabCount = userVocabProgressRepository.countByUserId(user.getId());
        } catch (Exception e) {
            log.warn("Could not get vocab count for user {}: {}", user.getId(), e.getMessage());
        }

        return AdminUserSummaryDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .authProvider(user.getAuthProvider())
                .enabled(user.isEnabled())
                .englishLevel(user.getEnglishLevel())
                .createdAt(user.getCreatedAt())
                .savedVocabCount(vocabCount)
                .build();
    }
}
