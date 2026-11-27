package com.aurora.projects;

import com.aurora.agents.AiProviderUsage;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProjectWorkflowStepUsageTests {
    @Test
    void storesReportedAgentRunUsageWithoutInventingMissingCost() {
        AiProviderUsage usage = mock(AiProviderUsage.class);
        when(usage.getStatus()).thenReturn("REPORTED");
        when(usage.getPromptTokens()).thenReturn(1200);
        when(usage.getCompletionTokens()).thenReturn(340);
        when(usage.getTotalTokens()).thenReturn(1540);
        when(usage.getProviderCostUsd()).thenReturn(new BigDecimal("0.0023400000"));

        ProjectWorkflowStep step = new ProjectWorkflowStep(mock(ProjectWorkflowRun.class), 1,
            "research-agent", "Research", "Research the market");
        step.recordAgentRun(42L, usage);

        assertEquals(42L, step.getAgentRunId());
        assertEquals("REPORTED", step.getUsageStatus());
        assertEquals(1200, step.getPromptTokens());
        assertEquals(340, step.getCompletionTokens());
        assertEquals(1540, step.getTotalTokens());
        assertEquals(new BigDecimal("0.0023400000"), step.getProviderCostUsd());
    }

    @Test
    void retryPreservesPriorRunAndUsageAndCanOnlyBeUsedOnce() {
        ProjectWorkflowStep step = new ProjectWorkflowStep(mock(ProjectWorkflowRun.class), 1,
            "research-agent", "Research", "Research the market");
        step.recordAgentRun(88L, null);
        step.fail("Provider request failed");
        step.prepareRetry();
        assertEquals("PENDING", step.getStatus());
        assertEquals(1, step.getRetryCount());
        assertEquals(88L, step.getPreviousAgentRunId());
        assertNull(step.getAgentRunId());
        assertNull(step.getUsageStatus());
        assertNull(step.getError());
        assertThrows(IllegalStateException.class, step::prepareRetry);
    }

    @Test
    void explicitStepDependenciesRoundTripOrder() {
        ProjectWorkflowStep step = new ProjectWorkflowStep(mock(ProjectWorkflowRun.class), 3,
            "content-agent", "Content", "Produce content");
        step.setDependencies(java.util.List.of(1, 2));
        assertEquals(java.util.List.of(1, 2), step.getDependsOnStepOrders());
        step.setDependencies(java.util.List.of());
        assertEquals(java.util.List.of(), step.getDependsOnStepOrders());
    }

    @Test
    void distinguishesUnavailableLedgerFromReportedZeroCost() {
        ProjectWorkflowStep step = new ProjectWorkflowStep(mock(ProjectWorkflowRun.class), 1,
            "content-agent", "Copy", "Write copy");
        step.recordAgentRun(43L, null);

        assertEquals(43L, step.getAgentRunId());
        assertEquals("NOT_RECORDED", step.getUsageStatus());
        assertNull(step.getProviderCostUsd());
        assertNull(step.getTotalTokens());
    }
}
