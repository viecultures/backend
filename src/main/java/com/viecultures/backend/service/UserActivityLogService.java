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
}
