CREATE TABLE ai_usage_periods (
    id BIGSERIAL PRIMARY KEY,
    workspace_id BIGINT NOT NULL REFERENCES tenant_workspaces(id) ON DELETE CASCADE,
    period_start DATE NOT NULL,
    request_count INTEGER NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_ai_usage_workspace_period UNIQUE(workspace_id, period_start),
    CONSTRAINT ck_ai_usage_request_count CHECK (request_count >= 0)
);
CREATE INDEX idx_ai_usage_workspace_period ON ai_usage_periods(workspace_id, period_start);
