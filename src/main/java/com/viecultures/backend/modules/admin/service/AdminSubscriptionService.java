package com.viecultures.backend.modules.admin.service;

import com.viecultures.backend.modules.admin.dto.*;
import com.viecultures.backend.modules.subscription.dto.*;

import java.util.List;

public interface AdminSubscriptionService {

    SubscriptionMetricsDto getMetrics();

    List<SubscriptionTransactionDto> getTransactions(String status, String query);

    SubscriptionTransactionDto approveTransaction(String id, ApproveTransactionRequest request);

    SubscriptionTransactionDto rejectTransaction(String id, RejectTransactionRequest request);

    List<SubscriptionPlanDto> getPlans();

    List<SubscriptionPlanDto> getActivePlans();

    SubscriptionPlanDto savePlan(SubscriptionPlanDto planDto);

    SubscriptionPlanDto togglePlanStatus(String id);
}
