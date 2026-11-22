package com.aurora.agents;

import com.aurora.businesses.BusinessRepository;
import com.aurora.businesses.BusinessWorkspace;
import com.aurora.security.WorkspaceContextHolder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.Duration;

@Service
public class AiBudgetGuardService {
    private final AiBudgetGuardLockRepository locks;
    private final AiBudgetReservationRepository reservations;
    private final BusinessAiBudgetRepository businessBudgets;
    private final BusinessRepository businesses;
    private final AiProviderUsageRepository providerUsage;
    private final BusinessAiBudgetAlertRepository budgetAlerts;
    private final BigDecimal platformMonthlyLimit;
    private final BigDecimal maxRequestReservation;

    public record BusinessBudgetView(Long businessId, BigDecimal monthlyLimitUsd,
                                     BigDecimal reportedSpendUsd, BigDecimal heldUsd,
                                     BigDecimal availableUsd, boolean enabled, String alertLevel,
                                     boolean reservationConfigured) {}

    public AiBudgetGuardService(AiBudgetGuardLockRepository locks, AiBudgetReservationRepository reservations,
                                BusinessAiBudgetRepository businessBudgets, BusinessRepository businesses,
                                AiProviderUsageRepository providerUsage, BusinessAiBudgetAlertRepository budgetAlerts,
                                @Value("${aurora.ai.platform-monthly-budget-usd:0}") BigDecimal platformMonthlyLimit,
                                @Value("${aurora.ai.max-request-reservation-usd:0}") BigDecimal maxRequestReservation) {
        this.locks = locks; this.reservations = reservations; this.businessBudgets = businessBudgets;
        this.businesses = businesses;
        this.providerUsage = providerUsage;
        this.budgetAlerts = budgetAlerts;
        this.platformMonthlyLimit = configuredAmount(platformMonthlyLimit, "AURORA_AI_PLATFORM_MONTHLY_BUDGET_USD");
        this.maxRequestReservation = configuredAmount(maxRequestReservation, "AURORA_AI_MAX_REQUEST_RESERVATION_USD");
        if (this.platformMonthlyLimit.signum() > 0 && this.maxRequestReservation.signum() <= 0)
            throw new IllegalStateException("AURORA_AI_MAX_REQUEST_RESERVATION_USD must be positive when a platform AI budget is enabled.");
    }

    /** Commits a database-backed reservation before any request is sent to the provider. */
    @Transactional
    public Long reserve(AgentRun run) {
        Long businessId = run.getBusinessId();
        boolean platformEnabled = platformMonthlyLimit.signum() > 0;
        if (!platformEnabled && businessId == null) return null;
        // Read the policy only after taking the shared lock. Policy edits use the same
        // lock, so a request cannot race with a newly enabled or raised/lowered cap.
        locks.findForUpdate(1L).orElseThrow(() -> new IllegalStateException("AI budget lock row is missing; refusing provider request."));
        BusinessAiBudget businessPolicy = businessId == null ? null
            : businessBudgets.findByBusinessIdAndWorkspace_Id(businessId, run.getWorkspace().getId()).orElse(null);
        if (!platformEnabled && businessPolicy == null) return null;
        requireReservationConfigured();
        Instant[] month = currentMonthBounds();
        BigDecimal requestHold = maxRequestReservation;
        if (platformEnabled) {
            BigDecimal platformUsed = reservations.sumSettledCost(month[0], month[1]).add(reservations.sumUnreservedProviderCost(month[0], month[1])).add(reservations.sumAllHeld());
            if (platformUsed.add(requestHold).compareTo(platformMonthlyLimit) > 0)
                throw budgetExhausted("The platform-wide monthly AI spending ceiling has been reached.");
        }
        if (businessPolicy != null) {
            BigDecimal businessUsed = reservations.sumSettledCostForBusiness(businessId, month[0], month[1]).add(reservations.sumUnreservedProviderCostForBusiness(businessId, month[0], month[1]))
                .add(reservations.sumHeldForBusiness(businessId));
            evaluateBusinessAlerts(businessId, run.getWorkspace().getId(), businessPolicy.getMonthlyLimitUsd());
            if (businessUsed.add(requestHold).compareTo(businessPolicy.getMonthlyLimitUsd()) > 0)
                throw budgetExhausted("This business has reached its monthly AI spending ceiling.");
        }
        Long reservationId = reservations.save(new AiBudgetReservation(run, requestHold)).getId();
        if (businessPolicy != null) evaluateBusinessAlerts(businessId, run.getWorkspace().getId(), businessPolicy.getMonthlyLimitUsd());
        return reservationId;
    }

    /** Unknown/missing cost deliberately leaves the reservation held; it is never treated as free. */
    @Transactional
    public void settle(Long reservationId, BigDecimal providerReportedCost) {
        if (reservationId == null || providerReportedCost == null || providerReportedCost.signum() < 0) return;
        locks.findForUpdate(1L).orElseThrow(() -> new IllegalStateException("AI budget lock row is missing."));
        reservations.findById(reservationId).ifPresent(reservation -> {
            reservation.settle(providerReportedCost);
            if (reservation.getBusinessId() != null) {
                businessBudgets.findByBusinessIdAndWorkspace_Id(reservation.getBusinessId(), reservation.getWorkspaceId())
                    .ifPresent(policy -> evaluateBusinessAlerts(reservation.getBusinessId(), reservation.getWorkspaceId(), policy.getMonthlyLimitUsd()));
            }
        });
    }

    @Transactional
    public BusinessBudgetView setBusinessBudget(Long businessId, BigDecimal limit) {
        WorkspaceContextHolder.requireAnyRole(com.aurora.identity.WorkspaceRole.OWNER, com.aurora.identity.WorkspaceRole.ADMIN);
        BusinessWorkspace business = businesses.findByIdAndWorkspace_Id(businessId, WorkspaceContextHolder.id())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Business not found."));
        requireReservationConfigured();
        locks.findForUpdate(1L).orElseThrow(() -> new IllegalStateException("AI budget lock row is missing."));
        BusinessAiBudget policy = businessBudgets.findByBusinessIdAndWorkspace_Id(businessId, WorkspaceContextHolder.id())
            .orElseGet(() -> new BusinessAiBudget(business, limit));
        policy.setLimit(limit);
        businessBudgets.save(policy);
        evaluateBusinessAlerts(businessId, WorkspaceContextHolder.id(), policy.getMonthlyLimitUsd());
        return businessBudget(businessId);
    }

    @Transactional(readOnly = true)
    public BusinessBudgetView businessBudget(Long businessId) {
        BusinessWorkspace business = businesses.findByIdAndWorkspace_Id(businessId, WorkspaceContextHolder.id())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Business not found."));
        var policy = businessBudgets.findByBusinessIdAndWorkspace_Id(businessId, WorkspaceContextHolder.id());
        Instant[] month = currentMonthBounds();
        BigDecimal reported = reservations.sumSettledCostForBusiness(businessId, month[0], month[1]).add(reservations.sumUnreservedProviderCostForBusiness(businessId, month[0], month[1]));
        BigDecimal held = reservations.sumHeldForBusiness(businessId);
        BigDecimal limit = policy.map(BusinessAiBudget::getMonthlyLimitUsd).orElse(BigDecimal.ZERO);
        BigDecimal available = limit.subtract(reported).subtract(held).max(BigDecimal.ZERO);
        BigDecimal used = reported.add(held);
        String alert = !policy.isPresent() ? "DISABLED"
            : used.compareTo(limit) >= 0 ? "EXHAUSTED"
            : used.multiply(BigDecimal.valueOf(100)).compareTo(limit.multiply(BigDecimal.valueOf(90))) >= 0 ? "CRITICAL"
            : used.multiply(BigDecimal.valueOf(100)).compareTo(limit.multiply(BigDecimal.valueOf(80))) >= 0 ? "WARNING" : "NORMAL";
        return new BusinessBudgetView(business.getId(), limit, reported, held, available, policy.isPresent(), alert,
            maxRequestReservation.signum() > 0);
    }

    @Transactional
    public void removeBusinessBudget(Long businessId) {
        WorkspaceContextHolder.requireAnyRole(com.aurora.identity.WorkspaceRole.OWNER, com.aurora.identity.WorkspaceRole.ADMIN);
        businesses.findByIdAndWorkspace_Id(businessId, WorkspaceContextHolder.id())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Business not found."));
        locks.findForUpdate(1L).orElseThrow(() -> new IllegalStateException("AI budget lock row is missing."));
        if (reservations.sumHeldForBusiness(businessId).signum() > 0)
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "This business has unresolved AI cost reservations. Reconcile provider billing before removing its spending ceiling.");
        businessBudgets.deleteByBusinessIdAndWorkspace_Id(businessId, WorkspaceContextHolder.id());
    }

    public record ReconciliationItem(Long id, Long businessId, String businessName, Long agentRunId,
                                     String agentName, String model, String providerRequestId, String usageStatus,
                                     BigDecimal reservedUsd, Instant createdAt) {}

    @Transactional(readOnly = true)
    public java.util.List<ReconciliationItem> unresolvedReservations() {
        WorkspaceContextHolder.requireAnyRole(com.aurora.identity.WorkspaceRole.OWNER, com.aurora.identity.WorkspaceRole.ADMIN);
        Instant staleBefore = Instant.now().minus(Duration.ofHours(1));
        var held = reservations.findUnresolvedForWorkspace(WorkspaceContextHolder.id(), staleBefore, PageRequest.of(0, 100));
        var usageByRun = providerUsage.findAllByAgentRun_IdIn(held.stream().map(AiBudgetReservation::getAgentRunId).toList())
            .stream().collect(java.util.stream.Collectors.toMap(entry -> entry.getAgentRun().getId(), entry -> entry));
        return held.stream().map(r -> {
            AiProviderUsage usage = usageByRun.get(r.getAgentRunId());
            return new ReconciliationItem(r.getId(), r.getBusinessId(), r.getBusinessName(), r.getAgentRunId(),
                r.getAgentName(), r.getModel(), usage == null ? null : usage.getProviderRequestId(),
                usage == null ? null : usage.getStatus(), r.getReservedUsd(), r.getCreatedAt());
        }).toList();
    }

    @Transactional
    public void reconcile(Long reservationId, BigDecimal actualCost, String reference, String note) {
        WorkspaceContextHolder.requireAnyRole(com.aurora.identity.WorkspaceRole.OWNER, com.aurora.identity.WorkspaceRole.ADMIN);
        locks.findForUpdate(1L).orElseThrow(() -> new IllegalStateException("AI budget lock row is missing; refusing reconciliation."));
        AiBudgetReservation reservation = reservations.findByIdAndWorkspace_Id(reservationId, WorkspaceContextHolder.id())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reservation not found."));
        if (!"HELD".equals(reservation.getStatus()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This reservation has already been settled.");
        if ("RUNNING".equals(reservation.getAgentRunStatus())
            && reservation.getAgentRunStartedAt().isAfter(Instant.now().minus(Duration.ofHours(1))))
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "This AI request is still running. Reconcile only after it has finished or remained stuck for at least one hour.");
        reservation.reconcile(actualCost, reference, note, WorkspaceContextHolder.role().name());
        providerUsage.findByAgentRun_Id(reservation.getAgentRunId())
            .ifPresent(entry -> entry.recordManualReconciliationCost(actualCost));
        if (reservation.getBusinessId() != null) {
            businessBudgets.findByBusinessIdAndWorkspace_Id(reservation.getBusinessId(), WorkspaceContextHolder.id())
                .ifPresent(policy -> evaluateBusinessAlerts(reservation.getBusinessId(), WorkspaceContextHolder.id(), policy.getMonthlyLimitUsd()));
        }
    }


    private void evaluateBusinessAlerts(Long businessId, Long workspaceId, BigDecimal limit) {
        LocalDate periodStart = LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1);
        Instant[] month = currentMonthBounds();
        BigDecimal reported = reservations.sumSettledCostForBusiness(businessId, month[0], month[1])
            .add(reservations.sumUnreservedProviderCostForBusiness(businessId, month[0], month[1]));
        BigDecimal held = reservations.sumHeldForBusiness(businessId);
        BigDecimal observed = reported.add(held);
        String[] levels = {"WARNING", "CRITICAL", "EXHAUSTED"};
        int[] thresholds = {80, 90, 100};
        BusinessWorkspace business = businesses.findByIdAndWorkspace_Id(businessId, workspaceId).orElseThrow();
        for (int i = 0; i < levels.length; i++) {
            if (observed.multiply(BigDecimal.valueOf(100)).compareTo(limit.multiply(BigDecimal.valueOf(thresholds[i]))) >= 0
                && !budgetAlerts.existsByBusiness_IdAndPeriodStartAndAlertLevel(businessId, periodStart, levels[i])) {
                budgetAlerts.save(new BusinessAiBudgetAlert(business.getWorkspace(), business, periodStart,
                    levels[i], observed, limit));
            }
        }
    }

    private void requireReservationConfigured() {
        if (maxRequestReservation.signum() <= 0)
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "AI spending limits are configured without a safe per-request reservation. Provider execution is disabled until AURORA_AI_MAX_REQUEST_RESERVATION_USD is configured.");
    }

    private static ResponseStatusException budgetExhausted(String message) {
        return new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, message);
    }

    private static BigDecimal configuredAmount(BigDecimal value, String setting) {
        BigDecimal amount = value == null ? BigDecimal.ZERO : value;
        if (amount.signum() < 0 || amount.precision() > 18 || amount.scale() > 10)
            throw new IllegalStateException(setting + " must be non-negative and fit NUMERIC(18,10).");
        return amount;
    }

    private static Instant[] currentMonthBounds() {
        LocalDate first = LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1);
        return new Instant[]{first.atStartOfDay().toInstant(ZoneOffset.UTC),
            first.plusMonths(1).atStartOfDay().toInstant(ZoneOffset.UTC)};
    }
}
