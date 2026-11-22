package com.aurora.identity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "tenant_workspaces")
public class TenantWorkspace {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 140)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private WorkspaceStatus status = WorkspaceStatus.ACTIVE;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected TenantWorkspace() {}

    public TenantWorkspace(String name) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Workspace name is required.");
        this.name = name.trim();
        this.status = WorkspaceStatus.ACTIVE;
    }

    @PrePersist
    void onCreate() { createdAt = Instant.now(); updatedAt = createdAt; }

    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public String getName() { return name; }
    public WorkspaceStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void rename(String name) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Workspace name is required.");
        this.name = name.trim();
    }
    public void suspend() { this.status = WorkspaceStatus.SUSPENDED; }
    public void activate() { this.status = WorkspaceStatus.ACTIVE; }
    public void close() { this.status = WorkspaceStatus.CLOSED; }
}
