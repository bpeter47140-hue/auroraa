package com.aurora.agents;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/agents/research")
public class ResearchAgentController {
    private final AgentExecutionService agents;

    public ResearchAgentController(AgentExecutionService agents) {
        this.agents = agents;
    }

    public record ResearchRequest(@NotBlank @Size(max=5000) String question) {}
    public record ResearchResponse(String agent, String model, String report, Instant generatedAt) {}

    @PostMapping("/run")
    public ResearchResponse run(@Valid @RequestBody ResearchRequest request) {
        com.aurora.security.WorkspaceContextHolder.requireAnyRole(
            com.aurora.identity.WorkspaceRole.OWNER,
            com.aurora.identity.WorkspaceRole.ADMIN,
            com.aurora.identity.WorkspaceRole.MEMBER
        );
        final AgentRun run;
        try {
            // All AI execution paths must share quota, provider-usage ledger and budget guards.
            run = agents.execute("research-agent", request.question());
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "The Research Agent is not currently available.");
        }
        if (!"SUCCEEDED".equals(run.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                "The Research Agent could not complete the request. Try again later.");
        }
        return new ResearchResponse(run.getAgentName(), run.getModel(), run.getOutput(), run.getStartedAt());
    }

    // Retained as a small parsing helper for provider response compatibility tests.
    static String extractTextContent(Object content) {
        if (content instanceof String text) return text;
        if (content instanceof List<?> blocks) {
            return blocks.stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .filter(block -> "text".equals(block.get("type")) && block.get("text") instanceof String)
                .map(block -> (String) block.get("text"))
                .collect(java.util.stream.Collectors.joining("\n"));
        }
        return "";
    }
}
