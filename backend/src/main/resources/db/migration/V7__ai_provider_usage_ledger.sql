CREATE TABLE ai_provider_usage (
    id BIGSERIAL PRIMARY KEY,
    workspace_id BIGINT NOT NULL REFERENCES tenant_workspaces(id) ON DELETE CASCADE,
    agent_run_id BIGINT NOT NULL UNIQUE REFERENCES agent_runs(id) ON DELETE CASCADE,
    provider VARCHAR(40) NOT NULL,
    model VARCHAR(160) NOT NULL,
    status VARCHAR(20) NOT NULL,
    prompt_tokens INTEGER,
    completion_tokens INTEGER,
    total_tokens INTEGER,
    provider_cost_usd NUMERIC(18,10),
    provider_request_id VARCHAR(160),
    recorded_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_ai_provider_prompt_tokens CHECK (prompt_tokens IS NULL OR prompt_tokens >= 0),
    CONSTRAINT ck_ai_provider_completion_tokens CHECK (completion_tokens IS NULL OR completion_tokens >= 0),
    CONSTRAINT ck_ai_provider_total_tokens CHECK (total_tokens IS NULL OR total_tokens >= 0),
    CONSTRAINT ck_ai_provider_cost CHECK (provider_cost_usd IS NULL OR provider_cost_usd >= 0),
    CONSTRAINT ck_ai_provider_status CHECK (status IN ('PENDING','REPORTED','UNAVAILABLE','UNKNOWN'))
);
CREATE INDEX idx_ai_provider_usage_workspace_time ON ai_provider_usage(workspace_id, recorded_at);
CREATE INDEX idx_ai_provider_usage_model_time ON ai_provider_usage(model, recorded_at);
