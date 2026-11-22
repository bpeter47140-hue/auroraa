CREATE TABLE business_ai_budget_alerts (
    id BIGSERIAL PRIMARY KEY,
    workspace_id BIGINT NOT NULL REFERENCES tenant_workspaces(id) ON DELETE CASCADE,
    business_id BIGINT NOT NULL REFERENCES business_workspaces(id) ON DELETE CASCADE,
    period_start DATE NOT NULL,
    alert_level VARCHAR(16) NOT NULL,
    observed_usd NUMERIC(18,10) NOT NULL,
    monthly_limit_usd NUMERIC(18,10) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    acknowledged_at TIMESTAMPTZ,
    CONSTRAINT ck_business_ai_budget_alert_level CHECK (alert_level IN ('WARNING','CRITICAL','EXHAUSTED')),
    CONSTRAINT ck_business_ai_budget_alert_amounts CHECK (observed_usd >= 0 AND monthly_limit_usd > 0),
    CONSTRAINT uq_business_ai_budget_alert_period_level UNIQUE (business_id, period_start, alert_level)
);
CREATE INDEX idx_business_ai_budget_alerts_workspace_created
    ON business_ai_budget_alerts(workspace_id, created_at DESC);
