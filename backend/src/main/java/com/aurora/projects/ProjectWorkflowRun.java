package com.aurora.projects;

import com.aurora.identity.TenantWorkspace;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "project_workflow_runs")
public class ProjectWorkflowRun {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false) private TenantWorkspace workspace;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false) private VentureProject project;
    @Column(nullable = false, length = 60) private String workflowType;
    @Column(nullable = false, length = 24) private String status = "QUEUED";
    @Column(nullable = false, updatable = false) private Instant createdAt;
    private Instant startedAt;
    private Instant finishedAt;
    @Column(name = "worker_id", length = 80) private String workerId;
    @Column(name = "heartbeat_at") private Instant heartbeatAt;
    @Column(name = "lease_expires_at") private Instant leaseExpiresAt;
    @Column(length = 1200) private String error;

    protected ProjectWorkflowRun() {}
    public ProjectWorkflowRun(TenantWorkspace workspace, VentureProject project, String workflowType) {
        this.workspace = workspace; this.project = project; this.workflowType = workflowType; this.status = "QUEUED";
    }
    @PrePersist void onCreate() { createdAt = Instant.now(); }
    public void start() { status = "RUNNING"; startedAt = Instant.now(); }
    public void heartbeat(String worker, Instant expires) { if (!java.util.Objects.equals(workerId, worker)) throw new IllegalStateException("Lease ownership changed."); heartbeatAt = Instant.now(); leaseExpiresAt = expires; }
    public void clearLease() { workerId = null; heartbeatAt = null; leaseExpiresAt = null; }
    public String getWorkerId() { return workerId; }
    public Instant getHeartbeatAt() { return heartbeatAt; }
    public Instant getLeaseExpiresAt() { return leaseExpiresAt; }
    public void complete() { status = "SUCCEEDED"; finishedAt = Instant.now(); clearLease(); }
    public void fail(String message) { status = "FAILED"; error = message; finishedAt = Instant.now(); clearLease(); }
    public void awaitApproval() { status = "AWAITING_APPROVAL"; }
    public void approvalGranted() { if (!"AWAITING_APPROVAL".equals(status)) throw new IllegalStateException("Workflow is not awaiting approval."); status = "QUEUED"; }
    public void requestCancel() { status = "CANCEL_REQUESTED"; }
    public void requestPause() { status = "PAUSE_REQUESTED"; }
    public void pause() { status = "PAUSED"; clearLease(); }
    public void resume() { if (!"PAUSED".equals(status)) throw new IllegalStateException("Workflow is not paused."); status = "QUEUED"; finishedAt = null; }
    public void cancel() { status = "CANCELLED"; finishedAt = Instant.now(); clearLease(); }
    public Long getId() { return id; }
    public String getWorkflowType() { return workflowType; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getFinishedAt() { return finishedAt; }
    public String getError() { return error; }
    public Long getProjectId() { return project.getId(); }
    public String getProjectName() { return project.getName(); }
    @JsonIgnore public TenantWorkspace getWorkspace() { return workspace; }
    @JsonIgnore public VentureProject getProject() { return project; }
}
