package com.aurora.agents;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.aurora.identity.TenantWorkspace;
import java.time.Instant;

@Entity
@Table(name = "agent_runs")
public class AgentRun {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", foreignKey = @ForeignKey(name = "fk_agent_runs_workspace"))
    private TenantWorkspace workspace;
    @Column(nullable = false, length = 60) private String agentId;
    @Column(nullable = false, length = 40) private String agentName;
    @Column(nullable = false, length = 24) private String status;
    @Column(nullable = false, length = 5000) private String input;
    @Column(length = 30000) private String output;
    @Column(length = 1200) private String error;
    @Column(length = 160) private String model;
    @Column(nullable = false, updatable = false) private Instant startedAt;
    private Instant finishedAt;

    protected AgentRun() {}
    public AgentRun(String agentId, String agentName, String input, String model) {
        this.agentId = agentId; this.agentName = agentName; this.input = input;
        this.model = model; this.status = "RUNNING"; this.startedAt = Instant.now();
    }
    public void succeed(String output) { this.output = output; this.status = "SUCCEEDED"; this.finishedAt = Instant.now(); }
    public void fail(String error) { this.error = error; this.status = "FAILED"; this.finishedAt = Instant.now(); }
    public Long getId() { return id; }
    public String getAgentId() { return agentId; }
    public String getAgentName() { return agentName; }
    public String getStatus() { return status; }
    public String getInput() { return input; }
    public String getOutput() { return output; }
    public String getError() { return error; }
    public String getModel() { return model; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getFinishedAt() { return finishedAt; }

    @JsonIgnore
    public TenantWorkspace getWorkspace() { return workspace; }
    public void assignWorkspace(TenantWorkspace workspace) { if (workspace == null) throw new IllegalArgumentException("Workspace is required."); this.workspace = workspace; }
}
