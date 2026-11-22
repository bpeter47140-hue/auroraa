package com.aurora.agents;

import com.aurora.identity.TenantWorkspace;
import com.aurora.identity.WorkspaceRole;
import com.aurora.security.WorkspaceContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ResearchAgentControllerTests {
    @AfterEach
    void clearWorkspace() {
        WorkspaceContextHolder.clear();
    }

    @Test
    void acceptsPlainTextProviderContent() {
        assertEquals("A research report", ResearchAgentController.extractTextContent("A research report"));
    }

    @Test
    void extractsOnlyTextBlocksFromStructuredProviderContent() {
        Object content = List.of(
            Map.of("type", "text", "text", "First section"),
            Map.of("type", "image", "image_url", "ignored"),
            Map.of("type", "text", "text", "Second section")
        );

        assertEquals("First section\nSecond section", ResearchAgentController.extractTextContent(content));
    }

    @Test
    void treatsMissingOrUnsupportedContentAsEmpty() {
        assertEquals("", ResearchAgentController.extractTextContent(null));
        assertEquals("", ResearchAgentController.extractTextContent(List.of(Map.of("type", "image"))));
    }

    @Test
    void delegatesResearchToSharedExecutionService() {
        AgentExecutionService agents = mock(AgentExecutionService.class);
        AgentRun run = mock(AgentRun.class);
        WorkspaceContextHolder.set(new TenantWorkspace("Research controller test"), WorkspaceRole.OWNER);
        when(agents.execute("research-agent", "Research a business idea.")).thenReturn(run);
        when(run.getStatus()).thenReturn("SUCCEEDED");
        when(run.getAgentName()).thenReturn("Research Agent");
        when(run.getModel()).thenReturn("test-model");
        when(run.getOutput()).thenReturn("A research report");
        when(run.getStartedAt()).thenReturn(Instant.parse("2026-11-22T16:00:00Z"));

        ResearchAgentController controller = new ResearchAgentController(agents);
        ResearchAgentController.ResearchResponse response =
            controller.run(new ResearchAgentController.ResearchRequest("Research a business idea."));

        assertEquals("A research report", response.report());
        assertEquals("test-model", response.model());
        verify(agents).execute("research-agent", "Research a business idea.");
    }

    @Test
    void doesNotReturnAResearchReportWhenTheSharedExecutionFails() {
        AgentExecutionService agents = mock(AgentExecutionService.class);
        AgentRun run = mock(AgentRun.class);
        WorkspaceContextHolder.set(new TenantWorkspace("Research controller test"), WorkspaceRole.OWNER);
        when(agents.execute("research-agent", "Research a business idea.")).thenReturn(run);
        when(run.getStatus()).thenReturn("FAILED");

        ResearchAgentController controller = new ResearchAgentController(agents);
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
            () -> controller.run(new ResearchAgentController.ResearchRequest("Research a business idea.")));

        assertEquals(502, error.getStatusCode().value());
        verify(agents).execute("research-agent", "Research a business idea.");
    }
}
