package com.aurora.agents;

import com.aurora.identity.TenantWorkspace;
import com.aurora.identity.TenantWorkspaceRepository;
import com.aurora.businesses.BusinessRepository;
import com.aurora.businesses.BusinessWorkspace;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class AiProviderUsageLedgerTests {
    @Autowired AiProviderUsageLedgerService ledger;
    @Autowired AgentRunRepository runs;
    @Autowired TenantWorkspaceRepository workspaces;
    @Autowired BusinessRepository businesses;

    @Test
    void recordsProviderTokensAndReportedCostAgainstWorkspaceAndRun() {
        TenantWorkspace workspace = workspaces.saveAndFlush(new TenantWorkspace("Provider ledger test"));
        AgentRun run = new AgentRun("research-agent", "Research Agent", "test prompt", "test/model");
        run.assignWorkspace(workspace);
        run = runs.saveAndFlush(run);

        Long id = ledger.begin(run, "test/model");
        ledger.complete(id, Map.of(
            "id", "req_test_123",
            "usage", Map.of("prompt_tokens", 120, "completion_tokens", 45,
                "total_tokens", 165, "cost", new BigDecimal("0.0012345"))));

        var summary = ledger.currentMonth(workspace.getId());
        assertEquals(1, summary.providerRequests());
        assertEquals(1, summary.reportedRequests());
        assertEquals(0, summary.requestsWithUnknownUsage());
        assertEquals(120, summary.promptTokens());
        assertEquals(45, summary.completionTokens());
        assertEquals(165, summary.totalTokens());
        assertEquals(new BigDecimal("0.0012345000"), summary.reportedCostUsd());
        assertEquals(0, summary.requestsWithoutReportedCost());
    }

    @Test
    void missingProviderUsageIsNotCountedAsZeroCostOrZeroTokens() {
        TenantWorkspace workspace = workspaces.saveAndFlush(new TenantWorkspace("Missing provider usage test"));
        AgentRun run = new AgentRun("content-agent", "Content Agent", "test prompt", "test/model");
        run.assignWorkspace(workspace);
        run = runs.saveAndFlush(run);

        Long id = ledger.begin(run, "test/model");
        ledger.complete(id, Map.of("id", "req_without_usage"));

        var summary = ledger.currentMonth(workspace.getId());
        assertEquals(1, summary.providerRequests());
        assertEquals(0, summary.reportedRequests());
        assertEquals(1, summary.requestsWithUnknownUsage());
        assertEquals(0, summary.totalTokens());
        assertEquals(BigDecimal.ZERO, summary.reportedCostUsd());
        assertEquals(1, summary.requestsWithoutReportedCost());
    }

    @Test
    void agentRunCanBeAttributedToBusinessInsideItsWorkspace() {
        TenantWorkspace workspace = workspaces.saveAndFlush(new TenantWorkspace("Business attribution test"));
        BusinessWorkspace business = new BusinessWorkspace("Test business", "description", "goal");
        business.assignWorkspace(workspace);
        business = businesses.saveAndFlush(business);

        AgentRun run = new AgentRun("research-agent", "Research Agent", "test prompt", "test/model");
        run.assignWorkspace(workspace);
        run.assignBusiness(business);
        run = runs.saveAndFlush(run);

        assertEquals(business.getId(), run.getBusinessId());
        Long ledgerId = ledger.begin(run, "test/model");
        ledger.complete(ledgerId, Map.of("usage", Map.of("prompt_tokens", 80,
            "completion_tokens", 20, "total_tokens", 100, "cost", new BigDecimal("0.0009"))));
        var summary = ledger.currentMonth(workspace.getId());
        assertEquals(1, summary.businessUsage().size());
        assertEquals(business.getId(), summary.businessUsage().get(0).businessId());
        assertEquals("Test business", summary.businessUsage().get(0).businessName());
        assertEquals(100, summary.businessUsage().get(0).totalTokens());
        assertEquals(new BigDecimal("0.0009000000"), summary.businessUsage().get(0).reportedCostUsd());
    }

    @Test
    void agentRunRejectsBusinessFromAnotherWorkspace() {
        TenantWorkspace first = workspaces.saveAndFlush(new TenantWorkspace("First workspace"));
        TenantWorkspace second = workspaces.saveAndFlush(new TenantWorkspace("Second workspace"));
        BusinessWorkspace business = new BusinessWorkspace("Other tenant business", null, null);
        business.assignWorkspace(second);
        business = businesses.saveAndFlush(business);

        AgentRun run = new AgentRun("research-agent", "Research Agent", "test prompt", "test/model");
        run.assignWorkspace(first);
        BusinessWorkspace foreignBusiness = business;
        assertThrows(IllegalArgumentException.class, () -> run.assignBusiness(foreignBusiness));
    }

    @Test
    void failedProviderAttemptRemainsVisibleAsUnknownUsage() {
        TenantWorkspace workspace = workspaces.saveAndFlush(new TenantWorkspace("Unknown provider usage test"));
        AgentRun run = new AgentRun("strategy-agent", "Strategy Agent", "test prompt", "test/model");
        run.assignWorkspace(workspace);
        run = runs.saveAndFlush(run);

        Long id = ledger.begin(run, "test/model");
        ledger.markUnknown(id);

        var summary = ledger.currentMonth(workspace.getId());
        assertEquals(1, summary.providerRequests());
        assertEquals(1, summary.requestsWithUnknownUsage());
        assertEquals(1, summary.requestsWithoutReportedCost());
    }
}
