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

@Service
public class AiUsageService {
    private final AiUsagePeriodRepository usage;
    private final TenantWorkspaceRepository workspaces;
    private final int monthlyLimit;

    public record UsageSummary(int used, int limit, int remaining, LocalDate periodStart) {}

    public AiUsageService(AiUsagePeriodRepository usage, TenantWorkspaceRepository workspaces,
                          @Value("${aurora.ai.monthly-request-limit:100}") int monthlyLimit) {
        this.usage = usage;
        this.workspaces = workspaces;
        this.monthlyLimit = Math.max(1, monthlyLimit);
    }

    @Transactional
    public void reserveRequest() {
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
        return new UsageSummary(used, monthlyLimit, Math.max(0, monthlyLimit - used), period);
    }
}
