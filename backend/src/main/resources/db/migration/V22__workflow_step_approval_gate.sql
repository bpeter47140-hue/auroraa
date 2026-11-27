ALTER TABLE project_workflow_steps
    ADD COLUMN approval_required BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN approval_status VARCHAR(24),
    ADD COLUMN approval_decided_at TIMESTAMPTZ;
