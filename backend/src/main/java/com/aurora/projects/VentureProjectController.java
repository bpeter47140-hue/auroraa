package com.aurora.projects;

import com.aurora.audit.AuditService;
import com.aurora.identity.WorkspaceRole;
import com.aurora.security.WorkspaceContextHolder;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/projects")
public class VentureProjectController {
    private final VentureProjectRepository projects;
    private final AuditService audit;
    public VentureProjectController(VentureProjectRepository projects, AuditService audit) {
        this.projects = projects; this.audit = audit;
    }
    public record CreateProjectRequest(@NotBlank @Size(max=160) String name,
        @NotBlank @Size(max=4000) String goal, @Size(max=40) String projectType) {}
    public record UpdateProjectRequest(@NotBlank @Size(max=160) String name,
        @NotBlank @Size(max=4000) String goal, @Size(max=40) String projectType) {}
    public record UpdateProjectStatusRequest(@NotBlank String status) {}

    @GetMapping public List<VentureProject> list() {
        return projects.findAllByWorkspace_IdOrderByUpdatedAtDesc(WorkspaceContextHolder.id());
    }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public VentureProject create(@Valid @RequestBody CreateProjectRequest request) {
        WorkspaceContextHolder.requireAnyRole(WorkspaceRole.OWNER, WorkspaceRole.ADMIN, WorkspaceRole.MEMBER);
        VentureProject project = new VentureProject(request.name(), request.goal(), request.projectType());
        project.assignWorkspace(WorkspaceContextHolder.require());
        VentureProject saved = projects.save(project);
        audit.record("PROJECT_CREATED", "PROJECT", saved.getId().toString(), "Created project: " + saved.getName());
        return saved;
    }
    @GetMapping("/{id}") public VentureProject get(@PathVariable Long id) { return findInCurrentWorkspace(id); }
    @PutMapping("/{id}")
    public VentureProject update(@PathVariable Long id, @Valid @RequestBody UpdateProjectRequest request) {
        WorkspaceContextHolder.requireAnyRole(WorkspaceRole.OWNER, WorkspaceRole.ADMIN, WorkspaceRole.MEMBER);
        VentureProject project = findInCurrentWorkspace(id);
        project.updateDetails(request.name(), request.goal(), request.projectType());
        VentureProject saved = projects.save(project);
        audit.record("PROJECT_UPDATED", "PROJECT", id.toString(), "Updated project details.");
        return saved;
    }
    @PatchMapping("/{id}/status")
    public VentureProject updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateProjectStatusRequest request) {
        WorkspaceContextHolder.requireAnyRole(WorkspaceRole.OWNER, WorkspaceRole.ADMIN, WorkspaceRole.MEMBER);
        String status = request.status().trim().toUpperCase();
        if (!Set.of("PLANNING", "ACTIVE", "PAUSED", "COMPLETED", "ARCHIVED").contains(status))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status must be PLANNING, ACTIVE, PAUSED, COMPLETED or ARCHIVED.");
        VentureProject project = findInCurrentWorkspace(id);
        project.updateStatus(status);
        VentureProject saved = projects.save(project);
        audit.record("PROJECT_STATUS_CHANGED", "PROJECT", id.toString(), "Project status changed to " + status);
        return saved;
    }
    private VentureProject findInCurrentWorkspace(Long id) {
        return projects.findByIdAndWorkspace_Id(id, WorkspaceContextHolder.id())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found."));
    }
}
