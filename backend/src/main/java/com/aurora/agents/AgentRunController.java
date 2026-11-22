package com.aurora.agents;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/agent-runs")
public class AgentRunController {
    private final AgentExecutionService service;
    public AgentRunController(AgentExecutionService service) { this.service = service; }
    public record RunRequest(@NotBlank String agentId, @NotBlank @Size(max = 5000) String input, Long businessId) {}
    @GetMapping public List<AgentRun> recent() { return service.recent(); }
    @PostMapping public AgentRun execute(@Valid @RequestBody RunRequest request) {
        com.aurora.security.WorkspaceContextHolder.requireAnyRole(com.aurora.identity.WorkspaceRole.OWNER, com.aurora.identity.WorkspaceRole.ADMIN, com.aurora.identity.WorkspaceRole.MEMBER);
        try { return service.execute(request.agentId(), request.input(), request.businessId()); }
        catch (IllegalArgumentException ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage()); }
        catch (IllegalStateException ex) { throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage()); }
    }
}
