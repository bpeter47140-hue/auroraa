package com.aurora.agents;

import com.aurora.businesses.BusinessWorkspace;
import com.aurora.identity.TenantWorkspace;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "business_ai_budgets")
public class BusinessAiBudget {
    @Id @Column(name = "business_id") private Long businessId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false) private TenantWorkspace workspace;
    @Column(name = "monthly_limit_usd", nullable = false, precision = 18, scale = 10) private BigDecimal monthlyLimitUsd;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    protected BusinessAiBudget() {}
    public BusinessAiBudget(BusinessWorkspace business, BigDecimal limit) {
        if (business == null || business.getWorkspace() == null) throw new IllegalArgumentException("Business workspace is required.");
        this.businessId = business.getId(); this.workspace = business.getWorkspace(); setLimit(limit);
    }
    public void setLimit(BigDecimal limit) {
        if (limit == null || limit.signum() <= 0 || limit.scale() > 10 || limit.precision() > 18)
            throw new IllegalArgumentException("Monthly budget must be a positive amount with at most 10 decimal places.");
        this.monthlyLimitUsd = limit; this.updatedAt = Instant.now();
    }
    public Long getBusinessId() { return businessId; }
    public BigDecimal getMonthlyLimitUsd() { return monthlyLimitUsd; }
}
