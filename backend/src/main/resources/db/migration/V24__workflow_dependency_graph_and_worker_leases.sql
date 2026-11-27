ALTER TABLE project_workflow_steps ADD COLUMN depends_on_step_orders VARCHAR(500) NOT NULL DEFAULT '';
ALTER TABLE project_workflow_runs ADD COLUMN worker_id VARCHAR(80);
ALTER TABLE project_workflow_runs ADD COLUMN heartbeat_at TIMESTAMPTZ;
ALTER TABLE project_workflow_runs ADD COLUMN lease_expires_at TIMESTAMPTZ;
CREATE INDEX idx_workflow_run_lease_expiry ON project_workflow_runs(status, lease_expires_at);
