package com.aurora.agents;

import com.aurora.identity.TenantWorkspace;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "ai_usage_periods", uniqueConstraints = @UniqueConstraint(
    name = "uk_ai_usage_workspace_period", columnNames = {"workspace_id", "period_start"}))
public class AiUsagePeriod {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private TenantWorkspace workspace;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "request_count", nullable = false)
    private int requestCount;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AiUsagePeriod() {}

    public AiUsagePeriod(TenantWorkspace workspace, LocalDate periodStart) {
        this.workspace = workspace;
        this.periodStart = periodStart;
        this.requestCount = 0;
        this.updatedAt = Instant.now();
    }

    public void recordRequest() {
        requestCount++;
        updatedAt = Instant.now();
    }

    public int getRequestCount() { return requestCount; }
    public LocalDate getPeriodStart() { return periodStart; }
}
