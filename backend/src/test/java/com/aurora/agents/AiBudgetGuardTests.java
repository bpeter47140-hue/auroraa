package com.aurora.agents;

import com.aurora.businesses.BusinessRepository;
import com.aurora.businesses.BusinessWorkspace;
import com.aurora.identity.TenantWorkspace;
import com.aurora.identity.TenantWorkspaceRepository;
import com.aurora.identity.WorkspaceRole;
import com.aurora.security.WorkspaceContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class AiBudgetGuardTests {
    @Autowired AiBudgetGuardService guard;
    @Autowired AiBudgetGuardLockRepository locks;
    @Autowired TenantWorkspaceRepository workspaces;
    @Autowired BusinessRepository businesses;
    @Autowired AgentRunRepository runs;
    @Autowired AiProviderUsageLedgerService usageLedger;
    @Autowired BusinessAiBudgetAlertRepository budgetAlerts;
    @Autowired AiBudgetAlertController alertController;

    @AfterEach void clearContext() { WorkspaceContextHolder.clear(); }

    @Test
    void reservesMaximumBeforeDispatchAndBlocksWhenBusinessCeilingWouldBeExceeded() {
        TenantWorkspace workspace = workspaces.saveAndFlush(new TenantWorkspace("Budget guard workspace"));
        BusinessWorkspace business = new BusinessWorkspace("Budgeted business", null, null);
        business.assignWorkspace(workspace);
        business = businesses.saveAndFlush(business);
        locks.saveAndFlush(new AiBudgetGuardLock(1L));
        WorkspaceContextHolder.set(workspace, WorkspaceRole.OWNER);
        guard.setBusinessBudget(business.getId(), new BigDecimal("0.015"));

        AgentRun first = createRun(workspace, business);
        Long reservationId = guard.reserve(first);
        assertNotNull(reservationId);
        Long budgetedBusinessId = business.getId();
        var view = guard.businessBudget(budgetedBusinessId);
        assertEquals(new BigDecimal("0.0100000000"), view.heldUsd());
        assertEquals(new BigDecimal("0.0050000000"), view.availableUsd());
        ResponseStatusException removalBlocked = assertThrows(ResponseStatusException.class,
            () -> guard.removeBusinessBudget(budgetedBusinessId));
        assertEquals(HttpStatus.CONFLICT, removalBlocked.getStatusCode());

        AgentRun second = createRun(workspace, business);
        ResponseStatusException blocked = assertThrows(ResponseStatusException.class, () -> guard.reserve(second));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, blocked.getStatusCode());
    }

    @Test
    void providerReportedCostSettlesReservationAndUnknownCostRemainsHeld() {
        TenantWorkspace workspace = workspaces.saveAndFlush(new TenantWorkspace("Budget settlement workspace"));
        BusinessWorkspace business = new BusinessWorkspace("Settlement business", null, null);
        business.assignWorkspace(workspace);
        business = businesses.saveAndFlush(business);
        locks.saveAndFlush(new AiBudgetGuardLock(1L));
        WorkspaceContextHolder.set(workspace, WorkspaceRole.OWNER);
        guard.setBusinessBudget(business.getId(), new BigDecimal("0.05"));

        AgentRun settledRun = createRun(workspace, business);
        Long settledReservation = guard.reserve(settledRun);
        Long ledgerId = usageLedger.begin(settledRun, "test/model");
        usageLedger.complete(ledgerId, Map.of("usage", Map.of("prompt_tokens", 10, "completion_tokens", 5,
            "total_tokens", 15, "cost", new BigDecimal("0.003"))));
        guard.settle(settledReservation, new BigDecimal("0.003"));
        var afterSettlement = guard.businessBudget(business.getId());
        assertEquals(0, afterSettlement.heldUsd().compareTo(BigDecimal.ZERO));
        assertEquals(new BigDecimal("0.0030000000"), afterSettlement.reportedSpendUsd());

        AgentRun unknownRun = createRun(workspace, business);
        guard.reserve(unknownRun);
        var afterUnknown = guard.businessBudget(business.getId());
        assertEquals(new BigDecimal("0.0100000000"), afterUnknown.heldUsd());
        assertEquals(new BigDecimal("0.0370000000"), afterUnknown.availableUsd());
    }

    @Test
    void ownerCanReconcileUnknownCostOnlyAfterRecordingBillingEvidence() {
        TenantWorkspace workspace = workspaces.saveAndFlush(new TenantWorkspace("Manual reconciliation workspace"));
        BusinessWorkspace business = new BusinessWorkspace("Reconciliation business", null, null);
        business.assignWorkspace(workspace);
        business = businesses.saveAndFlush(business);
        locks.saveAndFlush(new AiBudgetGuardLock(1L));
        WorkspaceContextHolder.set(workspace, WorkspaceRole.OWNER);
        guard.setBusinessBudget(business.getId(), new BigDecimal("0.05"));
        AgentRun run = createRun(workspace, business);
        Long reservationId = guard.reserve(run);
        Long usageId = usageLedger.begin(run, "test/model");
        usageLedger.markUnknown(usageId);
        assertTrue(guard.unresolvedReservations().isEmpty(), "An active provider request must not be offered for manual reconciliation.");
        run.fail("Provider request timed out; awaiting billing reconciliation.");
        runs.saveAndFlush(run);
        assertEquals(1, guard.unresolvedReservations().size());

        guard.reconcile(reservationId, new BigDecimal("0.004"), "invoice-line-2026-11-22", "Matched provider invoice line to the recorded request ID.");
        // A delayed provider response overrides the manual value with the authoritative provider cost.
        usageLedger.complete(usageId, Map.of("usage", Map.of("prompt_tokens", 10, "completion_tokens", 5,
            "total_tokens", 15, "cost", new BigDecimal("0.005"))));
        guard.settle(reservationId, new BigDecimal("0.005"));
        var view = guard.businessBudget(business.getId());
        assertEquals(BigDecimal.ZERO, view.heldUsd());
        assertEquals(new BigDecimal("0.0050000000"), view.reportedSpendUsd());
        assertTrue(guard.unresolvedReservations().isEmpty());
        var ledgerSummary = usageLedger.currentMonth(workspace.getId());
        assertEquals(0, ledgerSummary.reportedCostUsd().compareTo(new BigDecimal("0.005")));
        assertEquals(0, ledgerSummary.requestsWithoutReportedCost());
        Long budgetedBusinessId = business.getId();
        guard.removeBusinessBudget(budgetedBusinessId);
        assertFalse(guard.businessBudget(budgetedBusinessId).enabled());
        ResponseStatusException duplicate = assertThrows(ResponseStatusException.class,
            () -> guard.reconcile(reservationId, new BigDecimal("0.004"), "invoice-line-2026-11-22", "Duplicate reconciliation attempt.") );
        assertEquals(HttpStatus.CONFLICT, duplicate.getStatusCode());
    }

    @Test
    void reconciliationQueueIsOwnerAdminOnlyAndWorkspaceScoped() {
        TenantWorkspace ownerWorkspace = workspaces.saveAndFlush(new TenantWorkspace("Owner reconciliation workspace"));
        BusinessWorkspace business = new BusinessWorkspace("Private reconciliation business", null, null);
        business.assignWorkspace(ownerWorkspace);
        business = businesses.saveAndFlush(business);
        locks.saveAndFlush(new AiBudgetGuardLock(1L));
        WorkspaceContextHolder.set(ownerWorkspace, WorkspaceRole.OWNER);
        guard.setBusinessBudget(business.getId(), new BigDecimal("0.05"));
        AgentRun run = createRun(ownerWorkspace, business);
        Long reservationId = guard.reserve(run);

        WorkspaceContextHolder.set(ownerWorkspace, WorkspaceRole.MEMBER);
        ResponseStatusException memberDenied = assertThrows(ResponseStatusException.class, guard::unresolvedReservations);
        assertEquals(HttpStatus.FORBIDDEN, memberDenied.getStatusCode());
        ResponseStatusException memberReconcileDenied = assertThrows(ResponseStatusException.class,
            () -> guard.reconcile(reservationId, new BigDecimal("0.002"), "billing-reference-01", "A provider invoice was checked."));
        assertEquals(HttpStatus.FORBIDDEN, memberReconcileDenied.getStatusCode());

        TenantWorkspace otherWorkspace = workspaces.saveAndFlush(new TenantWorkspace("Other workspace"));
        WorkspaceContextHolder.set(otherWorkspace, WorkspaceRole.OWNER);
        ResponseStatusException crossWorkspace = assertThrows(ResponseStatusException.class,
            () -> guard.reconcile(reservationId, new BigDecimal("0.002"), "billing-reference-01", "A provider invoice was checked."));
        assertEquals(HttpStatus.NOT_FOUND, crossWorkspace.getStatusCode());
    }

    @Test
    void persistentBudgetAlertsAreCreatedAtEachThresholdAndDeduplicatedPerMonth() {
        TenantWorkspace workspace = workspaces.saveAndFlush(new TenantWorkspace("Budget alerts workspace"));
        BusinessWorkspace business = new BusinessWorkspace("Alerted business", null, null);
        business.assignWorkspace(workspace);
        business = businesses.saveAndFlush(business);
        locks.saveAndFlush(new AiBudgetGuardLock(1L));
        WorkspaceContextHolder.set(workspace, WorkspaceRole.OWNER);
        guard.setBusinessBudget(business.getId(), new BigDecimal("0.01"));
        AgentRun run = createRun(workspace, business);
        guard.reserve(run);

        var alerts = budgetAlerts.findTop50ByWorkspace_IdOrderByCreatedAtDesc(workspace.getId());
        assertEquals(3, alerts.size());
        assertEquals(java.util.Set.of("WARNING", "CRITICAL", "EXHAUSTED"),
            alerts.stream().map(BusinessAiBudgetAlert::getAlertLevel).collect(java.util.stream.Collectors.toSet()));

        guard.setBusinessBudget(business.getId(), new BigDecimal("0.01"));
        assertEquals(3, budgetAlerts.findTop50ByWorkspace_IdOrderByCreatedAtDesc(workspace.getId()).size());

        BusinessAiBudgetAlert warning = alerts.stream().filter(a -> "WARNING".equals(a.getAlertLevel())).findFirst().orElseThrow();
        assertEquals(3, alertController.list().size());
        alertController.acknowledge(warning.getId());
        assertNotNull(budgetAlerts.findById(warning.getId()).orElseThrow().getAcknowledgedAt());

        WorkspaceContextHolder.set(workspace, WorkspaceRole.MEMBER);
        ResponseStatusException memberDenied = assertThrows(ResponseStatusException.class, alertController::list);
        assertEquals(HttpStatus.FORBIDDEN, memberDenied.getStatusCode());

        TenantWorkspace otherWorkspace = workspaces.saveAndFlush(new TenantWorkspace("Other alert workspace"));
        WorkspaceContextHolder.set(otherWorkspace, WorkspaceRole.OWNER);
        ResponseStatusException crossWorkspace = assertThrows(ResponseStatusException.class,
            () -> alertController.acknowledge(warning.getId()));
        assertEquals(HttpStatus.NOT_FOUND, crossWorkspace.getStatusCode());
    }

    @Test
    void concurrentRequestsCannotBothReserveTheSameRemainingBusinessBudget() throws Exception {
        TenantWorkspace workspace = workspaces.saveAndFlush(new TenantWorkspace("Concurrent budget workspace"));
        BusinessWorkspace business = new BusinessWorkspace("Concurrent budget business", null, null);
        business.assignWorkspace(workspace);
        business = businesses.saveAndFlush(business);
        locks.saveAndFlush(new AiBudgetGuardLock(1L));
        WorkspaceContextHolder.set(workspace, WorkspaceRole.OWNER);
        guard.setBusinessBudget(business.getId(), new BigDecimal("0.015"));
        AgentRun first = createRun(workspace, business);
        AgentRun second = createRun(workspace, business);
        Long businessId = business.getId();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<Boolean> a = executor.submit(() -> tryReserve(first, workspace, ready, start));
            Future<Boolean> b = executor.submit(() -> tryReserve(second, workspace, ready, start));
            ready.await();
            start.countDown();
            assertNotEquals(a.get(), b.get(), "Exactly one concurrent request should reserve the remaining budget.");
        }
        var view = guard.businessBudget(businessId);
        assertEquals(new BigDecimal("0.0100000000"), view.heldUsd());
    }

    private boolean tryReserve(AgentRun run, TenantWorkspace workspace, CountDownLatch ready, CountDownLatch start) {
        WorkspaceContextHolder.set(workspace, WorkspaceRole.OWNER);
        ready.countDown();
        try {
            start.await();
            guard.reserve(run);
            return true;
        } catch (ResponseStatusException blocked) {
            assertEquals(HttpStatus.TOO_MANY_REQUESTS, blocked.getStatusCode());
            return false;
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(interrupted);
        } finally { WorkspaceContextHolder.clear(); }
    }

    private AgentRun createRun(TenantWorkspace workspace, BusinessWorkspace business) {
        AgentRun run = new AgentRun("research-agent", "Research Agent", "budget test", "test/model");
        run.assignWorkspace(workspace); run.assignBusiness(business);
        return runs.saveAndFlush(run);
    }
}
