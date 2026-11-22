CREATE TABLE ai_budget_guard_lock (
    id BIGINT PRIMARY KEY,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_ai_budget_guard_singleton CHECK (id = 1)
);
INSERT INTO ai_budget_guard_lock (id, updated_at) VALUES (1, CURRENT_TIMESTAMP);

CREATE TABLE business_ai_budgets (
    business_id BIGINT PRIMARY KEY REFERENCES business_workspaces(id) ON DELETE CASCADE,
    workspace_id BIGINT NOT NULL REFERENCES tenant_workspaces(id) ON DELETE CASCADE,
    monthly_limit_usd NUMERIC(18,10) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_business_ai_budget_positive CHECK (monthly_limit_usd > 0)
);
CREATE INDEX idx_business_ai_budgets_workspace ON business_ai_budgets(workspace_id);

CREATE TABLE ai_budget_reservations (
    id BIGSERIAL PRIMARY KEY,
    workspace_id BIGINT NOT NULL REFERENCES tenant_workspaces(id) ON DELETE CASCADE,
    business_id BIGINT REFERENCES business_workspaces(id) ON DELETE SET NULL,
    agent_run_id BIGINT NOT NULL UNIQUE REFERENCES agent_runs(id) ON DELETE CASCADE,
    reserved_usd NUMERIC(18,10) NOT NULL,
    actual_cost_usd NUMERIC(18,10),
    status VARCHAR(16) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    settled_at TIMESTAMPTZ,
    CONSTRAINT ck_ai_budget_reservation_amount CHECK (reserved_usd > 0),
    CONSTRAINT ck_ai_budget_actual_cost CHECK (actual_cost_usd IS NULL OR actual_cost_usd >= 0),
    CONSTRAINT ck_ai_budget_reservation_status CHECK (status IN ('HELD','SETTLED'))
);
CREATE INDEX idx_ai_budget_reservations_status_time ON ai_budget_reservations(status, created_at);
CREATE INDEX idx_ai_budget_reservations_business_status ON ai_budget_reservations(business_id, status, created_at);
