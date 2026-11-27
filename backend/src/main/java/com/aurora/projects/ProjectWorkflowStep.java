package com.aurora.projects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "project_workflow_steps")
public class ProjectWorkflowStep {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workflow_run_id", nullable = false) private ProjectWorkflowRun workflowRun;
    @Column(nullable = false) private Integer stepOrder;
    @Column(name = "depends_on_step_orders", length = 500) private String dependsOnStepOrders = "";
    @Column(nullable = false, length = 60) private String agentId;
    @Column(nullable = false, length = 100) private String title;
    @Column(nullable = false, length = 6000) private String instructions;
    @Column(nullable = false, length = 24) private String status = "PENDING";
    @Column(name = "approval_required", nullable = false) private boolean approvalRequired;
    @Column(name = "approval_status", length = 24) private String approvalStatus;
    @Column(name = "approval_decided_at") private Instant approvalDecidedAt;
    @Column(length = 30000) private String output;
    @Column(length = 1200) private String error;
    @Column(name = "agent_run_id") private Long agentRunId;
    @Column(name = "previous_agent_run_id") private Long previousAgentRunId;
    @Column(name = "retry_count", nullable = false) private int retryCount;
    @Column(name = "usage_status", length = 24) private String usageStatus;
    @Column(name = "prompt_tokens") private Integer promptTokens;
    @Column(name = "completion_tokens") private Integer completionTokens;
    @Column(name = "total_tokens") private Integer totalTokens;
    @Column(name = "provider_cost_usd", precision = 18, scale = 10) private java.math.BigDecimal providerCostUsd;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    private Instant startedAt;
    private Instant finishedAt;

    protected ProjectWorkflowStep() {}
    public ProjectWorkflowStep(ProjectWorkflowRun workflowRun, int stepOrder, String agentId, String title, String instructions) {
        this.workflowRun = workflowRun; this.stepOrder = stepOrder; this.agentId = agentId;
        this.title = title; this.instructions = instructions; this.status = "PENDING";
    }
    @PrePersist void onCreate() { createdAt = Instant.now(); }
    public void setDependencies(java.util.List<Integer> orders) { this.dependsOnStepOrders = orders == null ? "" : orders.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(",")); }
    public java.util.List<Integer> getDependsOnStepOrders() { if (dependsOnStepOrders == null || dependsOnStepOrders.isBlank()) return java.util.List.of(); return java.util.Arrays.stream(dependsOnStepOrders.split(",")).map(Integer::parseInt).toList(); }
    public void requireApproval() { approvalRequired = true; approvalStatus = "PENDING"; }
    public void awaitApproval() { if (!approvalRequired || !"PENDING".equals(approvalStatus) || !"PENDING".equals(status)) throw new IllegalStateException("Step is not awaiting approval."); status = "AWAITING_APPROVAL"; }
    public void approve() { if (!approvalRequired || !"AWAITING_APPROVAL".equals(status) || !"PENDING".equals(approvalStatus)) throw new IllegalStateException("Step is not awaiting approval."); approvalStatus = "APPROVED"; approvalDecidedAt = Instant.now(); status = "PENDING"; }
    public void prepareRetry() {
        if (!"FAILED".equals(status) || retryCount >= 1) throw new IllegalStateException("This workflow step is not eligible for another retry.");
        if (agentRunId != null) previousAgentRunId = agentRunId;
        agentRunId = null; usageStatus = null; promptTokens = null; completionTokens = null; totalTokens = null; providerCostUsd = null;
        output = null; error = null; startedAt = null; finishedAt = null; retryCount++; status = "PENDING";
    }
    public void start() { status = "RUNNING"; startedAt = Instant.now(); }
    public void succeed(String output) { status = "SUCCEEDED"; this.output = output; finishedAt = Instant.now(); }
    public void fail(String message) { status = "FAILED"; error = message; finishedAt = Instant.now(); }
    public void recordAgentRun(Long runId, com.aurora.agents.AiProviderUsage usage) {
        this.agentRunId = runId;
        if (usage == null) { this.usageStatus = "NOT_RECORDED"; return; }
        this.usageStatus = usage.getStatus();
        this.promptTokens = usage.getPromptTokens();
        this.completionTokens = usage.getCompletionTokens();
        this.totalTokens = usage.getTotalTokens();
        this.providerCostUsd = usage.getProviderCostUsd();
    }
    public void cancel() { if ("PENDING".equals(status) || "AWAITING_APPROVAL".equals(status)) status = "CANCELLED"; finishedAt = Instant.now(); }
    public Long getId() { return id; }
    public Integer getStepOrder() { return stepOrder; }
    public String getAgentId() { return agentId; }
    public String getTitle() { return title; }
    public String getInstructions() { return instructions; }
    public String getStatus() { return status; }
    public boolean isApprovalRequired() { return approvalRequired; }
    public String getApprovalStatus() { return approvalStatus; }
    public Instant getApprovalDecidedAt() { return approvalDecidedAt; }
    public String getOutput() { return output; }
    public String getError() { return error; }
    public Long getAgentRunId() { return agentRunId; }
    public Long getPreviousAgentRunId() { return previousAgentRunId; }
    public int getRetryCount() { return retryCount; }
    public String getUsageStatus() { return usageStatus; }
    public Integer getPromptTokens() { return promptTokens; }
    public Integer getCompletionTokens() { return completionTokens; }
    public Integer getTotalTokens() { return totalTokens; }
    public java.math.BigDecimal getProviderCostUsd() { return providerCostUsd; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getFinishedAt() { return finishedAt; }
    @JsonIgnore public ProjectWorkflowRun getWorkflowRun() { return workflowRun; }
}
