package com.viecultures.backend.service.impl;

import com.viecultures.backend.dto.admin.*;
import com.viecultures.backend.entity.SubscriptionPlan;
import com.viecultures.backend.entity.SubscriptionTransaction;
import com.viecultures.backend.repository.SubscriptionPlanRepository;
import com.viecultures.backend.repository.SubscriptionTransactionRepository;
import com.viecultures.backend.service.AdminSubscriptionService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminSubscriptionServiceImpl implements AdminSubscriptionService {

    private final SubscriptionPlanRepository planRepository;
    private final SubscriptionTransactionRepository transactionRepository;

    @Override
    @Transactional(readOnly = true)
    public SubscriptionMetricsDto getMetrics() {
        try {
            Long totalRevenue = transactionRepository.sumTotalApprovedRevenue();
            long pending = transactionRepository.countByStatus("PENDING");
            long approved = transactionRepository.countByStatus("APPROVED");
            long rejected = transactionRepository.countByStatus("REJECTED");

            long totalDecided = approved + rejected;
            double approvalRate = totalDecided > 0 ? (approved * 100.0) / totalDecided : 0.0;

            List<SubscriptionTransaction> allApproved = transactionRepository.findByStatusOrderByCreatedAtDesc("APPROVED");
            Map<String, Long> tierDist = new HashMap<>();
            tierDist.put("FREE", 0L);
            tierDist.put("STARTER", 0L);
            tierDist.put("PRO", 0L);
            tierDist.put("LIFETIME", 0L);

            for (SubscriptionTransaction tx : allApproved) {
                String code = tx.getPlanCode() != null ? tx.getPlanCode().toUpperCase() : "STARTER";
                tierDist.put(code, tierDist.getOrDefault(code, 0L) + 1L);
            }

            return SubscriptionMetricsDto.builder()
                    .totalRevenueVnd(totalRevenue != null ? totalRevenue : 0L)
                    .pendingCount(pending)
                    .activeSubscribers(approved)
                    .approvalRatePercent(Math.round(approvalRate * 10.0) / 10.0)
                    .tierDistribution(tierDist)
                    .build();
        } catch (Exception e) {
            log.error("[AdminSubscriptionService] Lỗi khi tính toán chỉ số metrics: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionTransactionDto> getTransactions(String status, String query) {
        try {
            String sanitizedStatus = (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)) ? null : status.trim();
            String sanitizedQuery = (query == null || query.isBlank()) ? null : query.trim();

            List<SubscriptionTransaction> list;
            if (sanitizedStatus == null && sanitizedQuery == null) {
                list = transactionRepository.findAllByOrderByCreatedAtDesc();
            } else {
                list = transactionRepository.searchTransactions(sanitizedStatus, sanitizedQuery);
            }

            return list.stream().map(this::mapToTransactionDto).collect(Collectors.toList());
        } catch (Exception e) {
            log.error("[AdminSubscriptionService] Lỗi khi lấy danh sách giao dịch: status={}, query={}, err={}", status, query, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public SubscriptionTransactionDto approveTransaction(String id, ApproveTransactionRequest request) {
        try {
            SubscriptionTransaction tx = transactionRepository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy giao dịch với mã ID: " + id));

            tx.setStatus("APPROVED");
            tx.setApprovedAt(LocalDateTime.now());
            if (request != null && request.getNote() != null && !request.getNote().isBlank()) {
                tx.setNote(request.getNote().trim());
            }

            SubscriptionTransaction saved = transactionRepository.save(tx);
            log.info("[AdminSubscriptionService] Đã duyệt giao dịch thành công: id={}, code={}", saved.getId(), saved.getTransactionCode());
            return mapToTransactionDto(saved);
        } catch (Exception e) {
            log.error("[AdminSubscriptionService] Lỗi khi duyệt giao dịch: id={}, err={}", id, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public SubscriptionTransactionDto rejectTransaction(String id, RejectTransactionRequest request) {
        try {
            SubscriptionTransaction tx = transactionRepository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy giao dịch với mã ID: " + id));

            tx.setStatus("REJECTED");
            if (request != null && request.getReason() != null) {
                tx.setNote(request.getReason().trim());
            }

            SubscriptionTransaction saved = transactionRepository.save(tx);
            log.info("[AdminSubscriptionService] Đã từ chối giao dịch: id={}, code={}, lý do={}", saved.getId(), saved.getTransactionCode(), tx.getNote());
            return mapToTransactionDto(saved);
        } catch (Exception e) {
            log.error("[AdminSubscriptionService] Lỗi khi từ chối giao dịch: id={}, err={}", id, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionPlanDto> getPlans() {
        try {
            return planRepository.findAllByOrderBySortOrderAsc().stream()
                    .map(this::mapToPlanDto)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("[AdminSubscriptionService] Lỗi khi lấy danh sách gói cước: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public SubscriptionPlanDto savePlan(SubscriptionPlanDto planDto) {
        try {
            SubscriptionPlan plan;
            if (planDto.getId() != null && !planDto.getId().isBlank()) {
                plan = planRepository.findById(planDto.getId())
                        .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy gói cước với ID: " + planDto.getId()));
            } else {
                plan = new SubscriptionPlan();
                plan.setCode(planDto.getCode() != null ? planDto.getCode().trim().toUpperCase() : "TIER_" + System.currentTimeMillis());
            }

            if (planDto.getCode() != null && !planDto.getCode().isBlank()) {
                plan.setCode(planDto.getCode().trim().toUpperCase());
            }
            plan.setNameVi(planDto.getNameVi() != null ? planDto.getNameVi().trim() : "Gói chưa đặt tên");
            plan.setNameEn(planDto.getNameEn() != null ? planDto.getNameEn().trim() : "Unnamed Plan");
            plan.setDescriptionVi(planDto.getDescriptionVi() != null ? planDto.getDescriptionVi().trim() : "");
            plan.setPriceVnd(planDto.getPriceVnd() != null ? planDto.getPriceVnd() : 0L);
            plan.setDiscountPercent(planDto.getDiscountPercent() != null ? planDto.getDiscountPercent() : 0);
            plan.setBillingCycle(planDto.getBillingCycle() != null ? planDto.getBillingCycle().trim().toUpperCase() : "MONTHLY");
            plan.setIsActive(planDto.getIsActive() != null ? planDto.getIsActive() : true);
            plan.setIsPopularBadge(planDto.getIsPopularBadge() != null ? planDto.getIsPopularBadge() : false);
            plan.setSortOrder(planDto.getSortOrder() != null ? planDto.getSortOrder() : 0);

            if (planDto.getEntitlements() != null) {
                PlanFeatureEntitlementsDto ent = planDto.getEntitlements();
                plan.setMaxAiAudioMinutesPerMonth(ent.getMaxAiAudioMinutesPerMonth() != null ? ent.getMaxAiAudioMinutesPerMonth() : 30);
                plan.setMaxCefrLevelAccess(ent.getMaxCefrLevelAccess() != null ? ent.getMaxCefrLevelAccess() : "B1");
                plan.setUnlimitedJourneys(ent.getUnlimitedJourneys() != null ? ent.getUnlimitedJourneys() : false);
                plan.setOfflineDownloadsAllowed(ent.getOfflineDownloadsAllowed() != null ? ent.getOfflineDownloadsAllowed() : false);
                plan.setHighlightBadge(ent.getHighlightBadge() != null ? ent.getHighlightBadge() : false);
            }

            if (planDto.getPerksVi() != null) {
                plan.setPerksVi(new ArrayList<>(planDto.getPerksVi()));
            }
            if (planDto.getPerksEn() != null) {
                plan.setPerksEn(new ArrayList<>(planDto.getPerksEn()));
            }

            SubscriptionPlan saved = planRepository.save(plan);
            log.info("[AdminSubscriptionService] Đã lưu thông tin gói cước: id={}, code={}", saved.getId(), saved.getCode());
            return mapToPlanDto(saved);
        } catch (Exception e) {
            log.error("[AdminSubscriptionService] Lỗi khi lưu gói cước: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public SubscriptionPlanDto togglePlanStatus(String id) {
        try {
            SubscriptionPlan plan = planRepository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy gói cước với ID: " + id));

            boolean current = plan.getIsActive() != null ? plan.getIsActive() : true;
            plan.setIsActive(!current);

            SubscriptionPlan saved = planRepository.save(plan);
            log.info("[AdminSubscriptionService] Đã chuyển đổi trạng thái gói cước: id={}, isActive={}", saved.getId(), saved.getIsActive());
            return mapToPlanDto(saved);
        } catch (Exception e) {
            log.error("[AdminSubscriptionService] Lỗi khi chuyển đổi trạng thái gói: id={}, err={}", id, e.getMessage(), e);
            throw e;
        }
    }

    private SubscriptionPlanDto mapToPlanDto(SubscriptionPlan plan) {
        PlanFeatureEntitlementsDto entitlements = PlanFeatureEntitlementsDto.builder()
                .maxAiAudioMinutesPerMonth(plan.getMaxAiAudioMinutesPerMonth())
                .maxCefrLevelAccess(plan.getMaxCefrLevelAccess())
                .unlimitedJourneys(plan.getUnlimitedJourneys())
                .offlineDownloadsAllowed(plan.getOfflineDownloadsAllowed())
                .highlightBadge(plan.getHighlightBadge())
                .build();

        return SubscriptionPlanDto.builder()
                .id(plan.getId())
                .code(plan.getCode())
                .nameVi(plan.getNameVi())
                .nameEn(plan.getNameEn())
                .descriptionVi(plan.getDescriptionVi())
                .priceVnd(plan.getPriceVnd())
                .discountPercent(plan.getDiscountPercent())
                .billingCycle(plan.getBillingCycle())
                .isActive(plan.getIsActive())
                .isPopularBadge(plan.getIsPopularBadge())
                .sortOrder(plan.getSortOrder())
                .entitlements(entitlements)
                .perksVi(plan.getPerksVi() != null ? new ArrayList<>(plan.getPerksVi()) : new ArrayList<>())
                .perksEn(plan.getPerksEn() != null ? new ArrayList<>(plan.getPerksEn()) : new ArrayList<>())
                .build();
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
}
