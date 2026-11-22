package com.aurora.agents;

import com.aurora.businesses.BusinessWorkspace;
import com.aurora.identity.TenantWorkspace;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "business_ai_budget_alerts",
    uniqueConstraints = @UniqueConstraint(name = "uq_business_ai_budget_alert_period_level",
        columnNames = {"business_id", "period_start", "alert_level"}),
    indexes = @Index(name = "idx_business_ai_budget_alerts_workspace_created",
        columnList = "workspace_id, created_at"))
public class BusinessAiBudgetAlert {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "workspace_id", nullable = false)
    private TenantWorkspace workspace;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "business_id", nullable = false)
    private BusinessWorkspace business;
    @Column(name = "period_start", nullable = false) private LocalDate periodStart;
    @Column(name = "alert_level", nullable = false, length = 16) private String alertLevel;
    @Column(name = "observed_usd", nullable = false, precision = 18, scale = 10) private BigDecimal observedUsd;
    @Column(name = "monthly_limit_usd", nullable = false, precision = 18, scale = 10) private BigDecimal monthlyLimitUsd;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "acknowledged_at") private Instant acknowledgedAt;

    protected BusinessAiBudgetAlert() {}
    public BusinessAiBudgetAlert(TenantWorkspace workspace, BusinessWorkspace business, LocalDate periodStart,
                                 String alertLevel, BigDecimal observedUsd, BigDecimal monthlyLimitUsd) {
        this.workspace = workspace; this.business = business; this.periodStart = periodStart;
        this.alertLevel = alertLevel; this.observedUsd = observedUsd; this.monthlyLimitUsd = monthlyLimitUsd;
        this.createdAt = Instant.now();
    }
    public void acknowledge() { if (acknowledgedAt == null) acknowledgedAt = Instant.now(); }
    public Long getId() { return id; }
    public Long getBusinessId() { return business.getId(); }
    public String getBusinessName() { return business.getName(); }
    public LocalDate getPeriodStart() { return periodStart; }
    public String getAlertLevel() { return alertLevel; }
    public BigDecimal getObservedUsd() { return observedUsd; }
    public BigDecimal getMonthlyLimitUsd() { return monthlyLimitUsd; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getAcknowledgedAt() { return acknowledgedAt; }
}
