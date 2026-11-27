CREATE TABLE campaign_performance_metrics (
    id BIGSERIAL PRIMARY KEY,
    workspace_id BIGINT NOT NULL REFERENCES tenant_workspaces(id) ON DELETE CASCADE,
    campaign_id BIGINT NOT NULL REFERENCES marketing_campaigns(id) ON DELETE CASCADE,
    reported_date DATE NOT NULL,
    impressions BIGINT NOT NULL DEFAULT 0 CHECK (impressions >= 0),
    reach BIGINT NOT NULL DEFAULT 0 CHECK (reach >= 0 AND reach <= impressions),
    clicks BIGINT NOT NULL DEFAULT 0 CHECK (clicks >= 0 AND clicks <= impressions),
    likes BIGINT NOT NULL DEFAULT 0 CHECK (likes >= 0),
    comments BIGINT NOT NULL DEFAULT 0 CHECK (comments >= 0),
    shares BIGINT NOT NULL DEFAULT 0 CHECK (shares >= 0),
    conversions BIGINT NOT NULL DEFAULT 0 CHECK (conversions >= 0 AND conversions <= clicks),
    spend NUMERIC(14,2) NOT NULL DEFAULT 0 CHECK (spend >= 0),
    revenue NUMERIC(14,2) NOT NULL DEFAULT 0 CHECK (revenue >= 0),
    notes VARCHAR(1000),
    recorded_by VARCHAR(254) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_campaign_performance_day UNIQUE (workspace_id,campaign_id,reported_date)
);
CREATE INDEX idx_campaign_performance_workspace_campaign_date ON campaign_performance_metrics(workspace_id,campaign_id,reported_date DESC);
