package com.viecultures.backend.service;

import com.viecultures.backend.dto.analytics.ActivityLogBatchRequest;
import com.viecultures.backend.dto.analytics.ActivityLogResponse;
import com.viecultures.backend.dto.analytics.ActivityStatsSummaryDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserActivityLogService {

    void saveBatch(ActivityLogBatchRequest request, String ipAddress, String userAgent, String authUserId, String authUserEmail);

    Page<ActivityLogResponse> getLogs(String eventType, String eventCategory, String userId, String search, String fromDate, String toDate, Pageable pageable);

    ActivityStatsSummaryDto getStatsSummary();

    /**
     * Ghi nhan truc tiep mot thao tac quan tri (Admin/Moderator action) vao bang nhat ky user_activity_logs.
     *
     * @param userId      ID cua Admin/Moderator thuc hien
     * @param userEmail   Email cua Admin/Moderator
     * @param action      Ten hanh dong / loai su kien (vi du: CREATE_POST, UPDATE_USER_STATUS)
     * @param entity      Loai thuc the chiu tac dong (vi du: POST, USER, VOCABULARY)
     * @param path        Duong dan API duoc goi
     * @param payloadJson Thong tin chi tiet payload (tham so, id, ket qua) duoi dang JSON chuoi
     * @param ipAddress   Dia chi IP cua client
     * @param userAgent   Thong tin User-Agent trinh duyet / client
     */
    void logAdminAction(
            String userId,
            String userEmail,
            String action,
            String entity,
            String path,
            String payloadJson,
            String ipAddress,
            String userAgent
    );
}
