package com.aurora.agents;

import com.aurora.identity.TenantWorkspace;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "ai_provider_usage", indexes = {
    @Index(name = "idx_ai_provider_usage_workspace_time", columnList = "workspace_id, recorded_at"),
    @Index(name = "idx_ai_provider_usage_model_time", columnList = "model, recorded_at")
})
public class AiProviderUsage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private TenantWorkspace workspace;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agent_run_id", nullable = false, unique = true)
    private AgentRun agentRun;

    @Column(nullable = false, length = 40)
    private String provider;

    @Column(nullable = false, length = 160)
    private String model;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "prompt_tokens") private Integer promptTokens;
    @Column(name = "completion_tokens") private Integer completionTokens;
    @Column(name = "total_tokens") private Integer totalTokens;

    @Column(name = "provider_cost_usd", precision = 18, scale = 10)
    private BigDecimal providerCostUsd;

    @Column(name = "provider_request_id", length = 160)
    private String providerRequestId;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private Instant recordedAt;

    protected AiProviderUsage() {}

    public AiProviderUsage(AgentRun run, String provider, String model) {
        if (run == null || run.getWorkspace() == null) throw new IllegalArgumentException("An agent run with workspace is required.");
        this.agentRun = run;
        this.workspace = run.getWorkspace();
        this.provider = provider;
        this.model = model;
        this.status = "PENDING";
        this.recordedAt = Instant.now();
    }

    public void recordProviderResponse(Integer promptTokens, Integer completionTokens, Integer totalTokens,
                                       BigDecimal providerCostUsd, String providerRequestId, boolean usagePresent) {
        this.promptTokens = nonNegative(promptTokens);
        this.completionTokens = nonNegative(completionTokens);
        this.totalTokens = nonNegative(totalTokens);
        this.providerCostUsd = providerCostUsd != null && providerCostUsd.signum() >= 0 ? providerCostUsd : null;
        this.providerRequestId = providerRequestId;
        this.status = usagePresent ? "REPORTED" : "UNAVAILABLE";
    }

    public void markUnknown() { if ("PENDING".equals(status)) status = "UNKNOWN"; }

    public void recordManualReconciliationCost(BigDecimal cost) {
        if (cost == null || cost.signum() < 0 || cost.precision() > 18 || cost.scale() > 10)
            throw new IllegalArgumentException("A valid provider-confirmed USD cost is required.");
        this.providerCostUsd = cost;
        this.status = "MANUALLY_RECONCILED";
    }

    private static Integer nonNegative(Integer value) { return value != null && value >= 0 ? value : null; }

    public Long getId() { return id; }
    public AgentRun getAgentRun() { return agentRun; }
    public String getStatus() { return status; }
    public Integer getPromptTokens() { return promptTokens; }
    public Integer getCompletionTokens() { return completionTokens; }
    public Integer getTotalTokens() { return totalTokens; }
    public BigDecimal getProviderCostUsd() { return providerCostUsd; }
    public String getProviderRequestId() { return providerRequestId; }
    public Instant getRecordedAt() { return recordedAt; }
}
