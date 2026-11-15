package com.viecultures.backend.service.impl;

import com.viecultures.backend.entity.AiUsageLog;
import com.viecultures.backend.repository.AiUsageLogRepository;
import com.viecultures.backend.service.AiCostTrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiCostTrackingServiceImpl implements AiCostTrackingService {

    private final AiUsageLogRepository aiUsageLogRepository;

    private static final BigDecimal GEMINI_PROMPT_TOKEN_RATE = new BigDecimal("0.000000075");
    private static final BigDecimal GEMINI_COMPLETION_TOKEN_RATE = new BigDecimal("0.000000300");
    private static final BigDecimal AZURE_SPEECH_CHAR_RATE = new BigDecimal("0.000015000");

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AiUsageLog recordUsage(
            Long configId,
            String provider,
            String featureCode,
            String modelName,
            int promptTokens,
            int completionTokens,
            int characterCount,
            int latencyMs,
            int httpStatus
    ) {
        String safeProvider = (provider != null) ? provider.trim().toUpperCase() : "GEMINI";
        String safeFeature = (featureCode != null) ? featureCode.trim().toUpperCase() : "COMPANION_CHAT";

        BigDecimal estimatedCost = calculateEstimatedCost(safeProvider, modelName, promptTokens, completionTokens, characterCount);

        AiUsageLog usageLog = AiUsageLog.builder()
                .configId(configId)
                .provider(safeProvider)
                .featureCode(safeFeature)
                .modelName(modelName)
                .promptTokens(Math.max(0, promptTokens))
                .completionTokens(Math.max(0, completionTokens))
                .characterCount(Math.max(0, characterCount))
                .estimatedCostUsd(estimatedCost)
                .latencyMs(Math.max(0, latencyMs))
                .httpStatus(httpStatus)
                .build();

        try {
            AiUsageLog saved = aiUsageLogRepository.save(usageLog);
            log.debug("Ghi nhan su dung AI: id={}, provider={}, feature={}, costUsd={}, latencyMs={}",
                    saved.getId(), safeProvider, safeFeature, estimatedCost, latencyMs);
            return saved;
        } catch (Exception ex) {
            log.warn("Khong the luu ban ghi AI usage log (provider={}, feature={}): {}", safeProvider, safeFeature, ex.getMessage());
            return usageLog;
        }
    }

    @Override
    public BigDecimal calculateEstimatedCost(
            String provider,
            String modelName,
            int promptTokens,
            int completionTokens,
            int characterCount
    ) {
        String safeProvider = (provider != null) ? provider.trim().toUpperCase() : "GEMINI";
        BigDecimal cost = BigDecimal.ZERO;

        if ("GEMINI".equals(safeProvider)) {
            BigDecimal promptCost = GEMINI_PROMPT_TOKEN_RATE.multiply(BigDecimal.valueOf(Math.max(0, promptTokens)));
            BigDecimal completionCost = GEMINI_COMPLETION_TOKEN_RATE.multiply(BigDecimal.valueOf(Math.max(0, completionTokens)));
            cost = promptCost.add(completionCost);
        } else if ("AZURE".equals(safeProvider)) {
            cost = AZURE_SPEECH_CHAR_RATE.multiply(BigDecimal.valueOf(Math.max(0, characterCount)));
        }

        return cost.setScale(6, RoundingMode.HALF_UP);
    }
}
