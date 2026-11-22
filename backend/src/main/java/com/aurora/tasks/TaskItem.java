package com.aurora.tasks;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.aurora.identity.TenantWorkspace;
import java.time.Instant;

@Entity
@Table(name = "work_tasks")
public class TaskItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", foreignKey = @ForeignKey(name = "fk_work_tasks_workspace"))
    private TenantWorkspace workspace;
    @Column(nullable = false, length = 160)
    private String title;
    @Column(length = 2000)
    private String description;
    @Column(nullable = false, length = 20)
    private String priority = "NORMAL";
    @Column(nullable = false, length = 24)
    private String status = "PENDING";
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private Instant updatedAt;

    protected TaskItem() {}
    public TaskItem(String title, String description, String priority) {
        this.title = title; this.description = description;
        this.priority = priority == null ? "NORMAL" : priority;
    }
    @PrePersist void onCreate() { createdAt = Instant.now(); updatedAt = createdAt; }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }
    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getPriority() { return priority; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setStatus(String status) { this.status = status; }

    @JsonIgnore
    public TenantWorkspace getWorkspace() { return workspace; }
    public void assignWorkspace(TenantWorkspace workspace) { if (workspace == null) throw new IllegalArgumentException("Workspace is required."); this.workspace = workspace; }
}
