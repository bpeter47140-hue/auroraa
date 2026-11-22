package com.aurora.agents;

import com.aurora.businesses.BusinessWorkspace;
import com.aurora.identity.TenantWorkspace;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "ai_budget_reservations", indexes = {
    @Index(name = "idx_ai_budget_reservations_status_time", columnList = "status, created_at"),
    @Index(name = "idx_ai_budget_reservations_business_status", columnList = "business_id, status, created_at")
})
public class AiBudgetReservation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "workspace_id", nullable = false) private TenantWorkspace workspace;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "business_id") private BusinessWorkspace business;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "agent_run_id", nullable = false, unique = true) private AgentRun agentRun;
    @Column(name = "reserved_usd", nullable = false, precision = 18, scale = 10) private BigDecimal reservedUsd;
    @Column(name = "actual_cost_usd", precision = 18, scale = 10) private BigDecimal actualCostUsd;
    @Column(nullable = false, length = 16) private String status;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "settled_at") private Instant settledAt;
    @Column(name = "settlement_source", length = 16) private String settlementSource;
    @Column(name = "reconciliation_reference", length = 160) private String reconciliationReference;
    @Column(name = "reconciliation_note", length = 1000) private String reconciliationNote;
    @Column(name = "reconciled_by_role", length = 16) private String reconciledByRole;
    protected AiBudgetReservation() {}
    public AiBudgetReservation(AgentRun run, BigDecimal reservedUsd) {
        this.agentRun = run; this.workspace = run.getWorkspace(); this.business = run.getBusiness();
        this.reservedUsd = reservedUsd; this.status = "HELD"; this.createdAt = Instant.now();
    }
    public void settle(BigDecimal actual) {
        if (actual == null || actual.signum() < 0 || actual.precision() > 18 || actual.scale() > 10)
            throw new IllegalArgumentException("A valid provider-reported cost is required.");
        // A late provider response is more authoritative than a manual reconciliation
        // performed for a stale run. Keep the manual evidence fields for audit history.
        if ("SETTLED".equals(this.status) && !"MANUAL".equals(this.settlementSource)) return;
        this.actualCostUsd = actual; this.status = "SETTLED"; this.settledAt = Instant.now();
        this.settlementSource = "PROVIDER";
    }
    public void reconcile(BigDecimal actual, String reference, String note, String role) {
        if (!"HELD".equals(this.status)) throw new IllegalStateException("Only held reservations can be reconciled.");
        if (actual == null || actual.signum() < 0 || actual.precision() > 18 || actual.scale() > 10)
            throw new IllegalArgumentException("A valid provider-confirmed USD amount is required.");
        String cleanReference = reference == null ? "" : reference.trim();
        String cleanNote = note == null ? "" : note.trim();
        if (cleanReference.length() < 3 || cleanReference.length() > 160)
            throw new IllegalArgumentException("A provider billing reference between 3 and 160 characters is required.");
        if (cleanNote.length() < 10 || cleanNote.length() > 1000)
            throw new IllegalArgumentException("An evidence note between 10 and 1000 characters is required.");
        this.actualCostUsd = actual;
        this.status = "SETTLED";
        this.settledAt = Instant.now();
        this.settlementSource = "MANUAL";
        this.reconciliationReference = cleanReference;
        this.reconciliationNote = cleanNote;
        this.reconciledByRole = role;
    }
    public Long getId() { return id; }
    public Long getWorkspaceId() { return workspace.getId(); }
    public Long getBusinessId() { return business == null ? null : business.getId(); }
    public String getBusinessName() { return business == null ? null : business.getName(); }
    public Long getAgentRunId() { return agentRun.getId(); }
    public String getAgentName() { return agentRun.getAgentName(); }
    public String getAgentRunStatus() { return agentRun.getStatus(); }
    public Instant getAgentRunStartedAt() { return agentRun.getStartedAt(); }
    public String getModel() { return agentRun.getModel(); }
    public BigDecimal getReservedUsd() { return reservedUsd; }
    public BigDecimal getActualCostUsd() { return actualCostUsd; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getSettledAt() { return settledAt; }
    public String getSettlementSource() { return settlementSource; }
    public String getReconciliationReference() { return reconciliationReference; }
    public String getReconciliationNote() { return reconciliationNote; }
    public String getReconciledByRole() { return reconciledByRole; }
}
