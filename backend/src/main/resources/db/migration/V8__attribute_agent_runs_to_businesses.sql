ALTER TABLE agent_runs
    ADD COLUMN business_id BIGINT;

ALTER TABLE agent_runs
    ADD CONSTRAINT fk_agent_runs_business
    FOREIGN KEY (business_id) REFERENCES business_workspaces(id) ON DELETE SET NULL;

CREATE INDEX idx_agent_runs_business_started
    ON agent_runs(business_id, started_at DESC);
