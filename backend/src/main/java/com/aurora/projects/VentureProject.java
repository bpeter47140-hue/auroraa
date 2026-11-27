package com.aurora.projects;

import com.aurora.identity.TenantWorkspace;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "venture_projects")
public class VentureProject {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false, foreignKey = @ForeignKey(name = "fk_venture_projects_workspace"))
    private TenantWorkspace workspace;
    @Column(nullable = false, length = 160) private String name;
    @Column(nullable = false, length = 4000) private String goal;
    @Column(length = 40) private String projectType;
    @Column(nullable = false, length = 24) private String status = "PLANNING";
    @Column(nullable = false, updatable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;

    protected VentureProject() {}
    public VentureProject(String name, String goal, String projectType) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Project name is required.");
        if (goal == null || goal.isBlank()) throw new IllegalArgumentException("Project goal is required.");
        this.name = name.trim(); this.goal = goal.trim();
        this.projectType = projectType == null || projectType.isBlank() ? "GENERAL" : projectType.trim().toUpperCase();
        this.status = "PLANNING";
    }
    @PrePersist void onCreate() { createdAt = Instant.now(); updatedAt = createdAt; }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }
    public void updateDetails(String name, String goal, String projectType) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Project name is required.");
        if (goal == null || goal.isBlank()) throw new IllegalArgumentException("Project goal is required.");
        this.name = name.trim(); this.goal = goal.trim();
        if (projectType != null && !projectType.isBlank()) this.projectType = projectType.trim().toUpperCase();
    }
    public void updateStatus(String status) {
        if (!java.util.Set.of("PLANNING", "ACTIVE", "PAUSED", "COMPLETED", "ARCHIVED").contains(status))
            throw new IllegalArgumentException("Invalid project status.");
        this.status = status;
    }
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getGoal() { return goal; }
    public String getProjectType() { return projectType; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    @JsonIgnore public TenantWorkspace getWorkspace() { return workspace; }
    public void assignWorkspace(TenantWorkspace workspace) {
        if (workspace == null) throw new IllegalArgumentException("Workspace is required.");
        this.workspace = workspace;
    }
}
