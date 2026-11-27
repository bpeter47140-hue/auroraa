ALTER TABLE project_workflow_steps
    ADD COLUMN retry_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN previous_agent_run_id BIGINT;

ALTER TABLE project_workflow_steps
    ADD CONSTRAINT chk_project_workflow_step_retry_count CHECK (retry_count BETWEEN 0 AND 1);
