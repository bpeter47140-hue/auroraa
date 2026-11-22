package com.aurora.agents;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/agent-workflows")
public class AgentWorkflowController {
    private final AgentExecutionService agents;
    public AgentWorkflowController(AgentExecutionService agents) { this.agents = agents; }

    public record WorkflowRequest(@NotBlank @Size(max = 5000) String brief) {}
    public record WorkflowResponse(String workflow, String status, List<AgentRun> runs) {}

    @PostMapping("/chama-validation")
    public WorkflowResponse chamaValidation(@Valid @RequestBody WorkflowRequest request) {
        com.aurora.security.WorkspaceContextHolder.requireAnyRole(com.aurora.identity.WorkspaceRole.OWNER, com.aurora.identity.WorkspaceRole.ADMIN, com.aurora.identity.WorkspaceRole.MEMBER);
        List<AgentRun> completed = new ArrayList<>();
        String context = "Business brief:\n" + request.brief()
            + "\n\nImportant: treat this as an early hypothesis. Separate facts from assumptions. Do not claim live research or customer interviews.";
        String[][] stages = {
            {"research-agent", "Research Agent: assess the opportunity, likely customer problems, alternatives to investigate, risks and low-cost validation experiments."},
            {"strategy-agent", "Strategy Agent: using the original brief and Research Agent output below, create a focused strategy with target segment, value proposition hypotheses, business model, milestones, metrics and stop conditions."},
            {"content-agent", "Content Agent: using the original brief and prior agent outputs below, draft a short, honest launch message for recruiting interview participants. Do not imply the product exists or make unverified claims."},
            {"operations-agent", "Operations Agent: using the original brief and all prior outputs below, create a prioritised 2-week execution checklist with role owners, dependencies, suggested timing and success metrics."}
        };
        for (String[] stage : stages) {
            AgentRun run = agents.execute(stage[0], stage[1] + "\n\n" + context);
            completed.add(run);
            if (!"SUCCEEDED".equals(run.getStatus())) {
                return new WorkflowResponse("Chama opportunity validation", "FAILED", List.copyOf(completed));
            }
            context += "\n\n--- " + run.getAgentName() + " output ---\n" + run.getOutput();
            if (context.length() > 18000) context = context.substring(0, 18000);
        }
        return new WorkflowResponse("Chama opportunity validation", "SUCCEEDED", List.copyOf(completed));
    }
}
