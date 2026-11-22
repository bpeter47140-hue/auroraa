package com.aurora.agents;

import com.aurora.identity.TenantWorkspace;
import com.aurora.identity.TenantWorkspaceRepository;
import com.aurora.security.WorkspaceContextHolder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.math.BigDecimal;

@Service
public class AiUsageService {
    private final AiUsagePeriodRepository usage;
    private final TenantWorkspaceRepository workspaces;
    private final int monthlyLimit;
    private final AiProviderUsageLedgerService providerUsage;

    public record UsageSummary(int used, int limit, int remaining, LocalDate periodStart, boolean exempt,
                               long providerRequests, long reportedRequests, long requestsWithUnknownUsage,
                               long promptTokens, long completionTokens, long totalTokens,
                               BigDecimal reportedCostUsd, long requestsWithoutReportedCost,
                               java.util.List<AiProviderUsageLedgerService.BusinessSummary> businessUsage) {}

    public AiUsageService(AiUsagePeriodRepository usage, TenantWorkspaceRepository workspaces,
                          AiProviderUsageLedgerService providerUsage,
                          @Value("${aurora.ai.monthly-request-limit:100}") int monthlyLimit) {
        this.usage = usage;
        this.workspaces = workspaces;
        this.providerUsage = providerUsage;
        this.monthlyLimit = Math.max(1, monthlyLimit);
    }

    @Transactional
    public void reserveRequest() {
        // Workspace owners and administrators are exempt from the application-level
        // allowance. Provider billing still applies to every request.
        if (isPrivileged()) return;
        Long workspaceId = WorkspaceContextHolder.id();
        TenantWorkspace workspace = workspaces.findByIdForUpdate(workspaceId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Workspace is unavailable."));
        LocalDate period = LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1);
        AiUsagePeriod counter = usage.findByWorkspace_IdAndPeriodStart(workspaceId, period)
            .orElseGet(() -> usage.save(new AiUsagePeriod(workspace, period)));
        if (counter.getRequestCount() >= monthlyLimit) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                "This workspace has reached its monthly AI request allowance. The allowance resets next month.");
        }
        counter.recordRequest();
        usage.save(counter);
    }

    @Transactional(readOnly = true)
    public UsageSummary currentUsage() {
        Long workspaceId = WorkspaceContextHolder.id();
        LocalDate period = LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1);
        int used = usage.findByWorkspace_IdAndPeriodStart(workspaceId, period)
            .map(AiUsagePeriod::getRequestCount).orElse(0);
        var spend = providerUsage.currentMonth(workspaceId);
        return new UsageSummary(used, monthlyLimit, Math.max(0, monthlyLimit - used), period, isPrivileged(),
            spend.providerRequests(), spend.reportedRequests(), spend.requestsWithUnknownUsage(),
            spend.promptTokens(), spend.completionTokens(), spend.totalTokens(),
            spend.reportedCostUsd(), spend.requestsWithoutReportedCost(), spend.businessUsage());
    }

    private boolean isPrivileged() {
        return WorkspaceContextHolder.role() == com.aurora.identity.WorkspaceRole.OWNER
            || WorkspaceContextHolder.role() == com.aurora.identity.WorkspaceRole.ADMIN;
    }
}
