ALTER TABLE project_workflow_steps
    ADD COLUMN agent_run_id BIGINT REFERENCES agent_runs(id) ON DELETE SET NULL,
    ADD COLUMN usage_status VARCHAR(24),
    ADD COLUMN prompt_tokens INTEGER,
    ADD COLUMN completion_tokens INTEGER,
    ADD COLUMN total_tokens INTEGER,
    ADD COLUMN provider_cost_usd NUMERIC(18, 10);

ALTER TABLE project_workflow_steps
    ADD CONSTRAINT ck_project_workflow_step_usage_nonnegative
    CHECK (
        (prompt_tokens IS NULL OR prompt_tokens >= 0) AND
        (completion_tokens IS NULL OR completion_tokens >= 0) AND
        (total_tokens IS NULL OR total_tokens >= 0) AND
        (provider_cost_usd IS NULL OR provider_cost_usd >= 0)
    );

CREATE INDEX idx_project_workflow_steps_agent_run
    ON project_workflow_steps(agent_run_id) WHERE agent_run_id IS NOT NULL;
