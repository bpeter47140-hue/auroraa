package com.aurora.projects;

import com.aurora.identity.WorkspaceRole;
import com.aurora.security.WorkspaceContextHolder;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/workflows")
public class ProjectWorkflowController {
    private final ProjectWorkflowService service;
    private final ProjectWorkflowRunRepository runs;
    private final ProjectWorkflowStepRepository steps;
    private final VentureProjectRepository projects;

    public ProjectWorkflowController(ProjectWorkflowService service, ProjectWorkflowRunRepository runs,
        ProjectWorkflowStepRepository steps, VentureProjectRepository projects) {
        this.service = service; this.runs = runs; this.steps = steps; this.projects = projects;
    }

    public record CreateWorkflowRequest(@NotBlank @Size(max=60) String template) {}
    public record WorkflowDetail(ProjectWorkflowRun run, List<ProjectWorkflowStep> steps) {}

    @GetMapping("/templates")
    public List<String> templates(@PathVariable Long projectId) {
        requireProjectAccess(projectId);
        return service.availableTemplates();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public WorkflowDetail start(@PathVariable Long projectId, @Valid @RequestBody CreateWorkflowRequest request) {
        WorkspaceContextHolder.requireAnyRole(WorkspaceRole.OWNER, WorkspaceRole.ADMIN, WorkspaceRole.MEMBER);
        ProjectWorkflowRun run;
        try { run = service.create(projectId, request.template().trim().toLowerCase()); }
        catch (IllegalArgumentException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage()); }
        service.executeAsync(run.getId());
        return detail(run.getId());
    }

    @GetMapping
    public List<WorkflowDetail> list(@PathVariable Long projectId) {
        requireProjectAccess(projectId);
        return runs.findAllByProject_IdAndWorkspace_IdOrderByCreatedAtDesc(projectId, WorkspaceContextHolder.id()).stream()
            .map(run -> detail(run.getId())).toList();
    }

    @GetMapping("/{workflowId}")
    public WorkflowDetail get(@PathVariable Long projectId, @PathVariable Long workflowId) {
        ProjectWorkflowRun run = find(workflowId);
        if (!run.getProjectId().equals(projectId)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return detail(workflowId);
    }

    @PostMapping("/{workflowId}/steps/{stepId}/approve")
    public WorkflowDetail approveStep(@PathVariable Long projectId, @PathVariable Long workflowId, @PathVariable Long stepId) {
        WorkspaceContextHolder.requireAnyRole(WorkspaceRole.OWNER, WorkspaceRole.ADMIN);
        ProjectWorkflowRun run = find(workflowId);
        if (!run.getProjectId().equals(projectId)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        service.approveStep(workflowId, stepId);
        service.executeAsync(workflowId);
        return detail(workflowId);
    }

    @PostMapping("/{workflowId}/steps/{stepId}/retry")
    public WorkflowDetail retryFailedStep(@PathVariable Long projectId, @PathVariable Long workflowId, @PathVariable Long stepId) {
        WorkspaceContextHolder.requireAnyRole(WorkspaceRole.OWNER, WorkspaceRole.ADMIN);
        ProjectWorkflowRun run = find(workflowId);
        if (!run.getProjectId().equals(projectId)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        service.retryFailedStep(workflowId, stepId);
        service.executeAsync(workflowId);
        return detail(workflowId);
    }

    @PostMapping("/{workflowId}/pause")
    public WorkflowDetail pause(@PathVariable Long projectId, @PathVariable Long workflowId) {
        WorkspaceContextHolder.requireAnyRole(WorkspaceRole.OWNER, WorkspaceRole.ADMIN, WorkspaceRole.MEMBER);
        ProjectWorkflowRun run = find(workflowId);
        if (!run.getProjectId().equals(projectId)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        service.requestPause(workflowId);
        return detail(workflowId);
    }

    @PostMapping("/{workflowId}/resume")
    public WorkflowDetail resume(@PathVariable Long projectId, @PathVariable Long workflowId) {
        WorkspaceContextHolder.requireAnyRole(WorkspaceRole.OWNER, WorkspaceRole.ADMIN, WorkspaceRole.MEMBER);
        ProjectWorkflowRun run = find(workflowId);
        if (!run.getProjectId().equals(projectId)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        service.resume(workflowId);
        service.executeAsync(workflowId);
        return detail(workflowId);
    }

    @PostMapping("/{workflowId}/cancel")
    public WorkflowDetail cancel(@PathVariable Long projectId, @PathVariable Long workflowId) {
        WorkspaceContextHolder.requireAnyRole(WorkspaceRole.OWNER, WorkspaceRole.ADMIN, WorkspaceRole.MEMBER);
        ProjectWorkflowRun run = find(workflowId);
        if (!run.getProjectId().equals(projectId)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        service.requestCancel(workflowId);
        return detail(workflowId);
    }

    private ProjectWorkflowRun find(Long id) {
        return runs.findByIdAndWorkspace_Id(id, WorkspaceContextHolder.id())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow not found."));
    }
    private WorkflowDetail detail(Long id) {
        return new WorkflowDetail(find(id), steps.findAllByWorkflowRun_IdOrderByStepOrderAsc(id));
    }
    private void requireProjectAccess(Long projectId) {
        WorkspaceContextHolder.requireAnyRole(WorkspaceRole.OWNER, WorkspaceRole.ADMIN, WorkspaceRole.MEMBER, WorkspaceRole.VIEWER);
        if (!projects.existsByIdAndWorkspace_Id(projectId, WorkspaceContextHolder.id()))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found.");
    }
}
