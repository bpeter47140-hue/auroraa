package com.aurora.tasks;

import com.aurora.audit.AuditService;
import com.aurora.security.WorkspaceContextHolder;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {
    private final TaskRepository tasks;
    private final AuditService audit;

    public TaskController(TaskRepository tasks, AuditService audit) {
        this.tasks = tasks; this.audit = audit;
    }

    public record CreateTaskRequest(@NotBlank @Size(max=160) String title,
                                    @Size(max=2000) String description,
                                    String priority) {}
    public record UpdateTaskStatusRequest(@NotBlank String status) {}

    @GetMapping
    public List<TaskItem> list() { return tasks.findAllByWorkspace_IdOrderByCreatedAtDesc(WorkspaceContextHolder.id()); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskItem create(@Valid @RequestBody CreateTaskRequest request) {
        WorkspaceContextHolder.requireAnyRole(com.aurora.identity.WorkspaceRole.OWNER, com.aurora.identity.WorkspaceRole.ADMIN, com.aurora.identity.WorkspaceRole.MEMBER);
        String priority = request.priority() == null ? "NORMAL" : request.priority().toUpperCase();
        if (!List.of("LOW", "NORMAL", "HIGH", "URGENT").contains(priority))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Priority must be LOW, NORMAL, HIGH or URGENT");
        TaskItem item = new TaskItem(request.title().trim(), request.description(), priority);
        item.assignWorkspace(WorkspaceContextHolder.require());
        TaskItem saved = tasks.save(item);
        audit.record("TASK_CREATED", "TASK", saved.getId().toString(), "Created task: " + saved.getTitle());
        return saved;
    }

    @PatchMapping("/{id}/status")
    public TaskItem updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateTaskStatusRequest request) {
        WorkspaceContextHolder.requireAnyRole(com.aurora.identity.WorkspaceRole.OWNER, com.aurora.identity.WorkspaceRole.ADMIN, com.aurora.identity.WorkspaceRole.MEMBER);
        String status = request.status().toUpperCase();
        if (!List.of("PENDING", "IN_PROGRESS", "COMPLETED", "CANCELLED").contains(status))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid task status");
        TaskItem task = tasks.findByIdAndWorkspace_Id(id, WorkspaceContextHolder.id()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        task.setStatus(status);
        TaskItem saved = tasks.save(task);
        audit.record("TASK_STATUS_CHANGED", "TASK", id.toString(), "Task status changed to " + status);
        return saved;
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        WorkspaceContextHolder.requireAnyRole(com.aurora.identity.WorkspaceRole.OWNER, com.aurora.identity.WorkspaceRole.ADMIN);
        TaskItem task = tasks.findByIdAndWorkspace_Id(id, WorkspaceContextHolder.id()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        tasks.delete(task);
        audit.record("TASK_DELETED", "TASK", id.toString(), "Deleted task: " + task.getTitle());
    }
}
