CREATE TABLE project_workflow_runs (
    id BIGSERIAL PRIMARY KEY,
    workspace_id BIGINT NOT NULL REFERENCES tenant_workspaces(id) ON DELETE CASCADE,
    project_id BIGINT NOT NULL REFERENCES venture_projects(id) ON DELETE CASCADE,
    workflow_type VARCHAR(60) NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'QUEUED',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP WITH TIME ZONE,
    finished_at TIMESTAMP WITH TIME ZONE,
    error VARCHAR(1200)
);

CREATE INDEX idx_project_workflows_workspace_created
    ON project_workflow_runs(workspace_id, created_at DESC);
CREATE INDEX idx_project_workflows_status
    ON project_workflow_runs(status, created_at);

CREATE TABLE project_workflow_steps (
    id BIGSERIAL PRIMARY KEY,
    workflow_run_id BIGINT NOT NULL REFERENCES project_workflow_runs(id) ON DELETE CASCADE,
    step_order INTEGER NOT NULL,
    agent_id VARCHAR(60) NOT NULL,
    title VARCHAR(100) NOT NULL,
    instructions VARCHAR(6000) NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'PENDING',
    output VARCHAR(30000),
    error VARCHAR(1200),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP WITH TIME ZONE,
    finished_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_project_workflow_step_order UNIQUE (workflow_run_id, step_order)
);

CREATE INDEX idx_project_workflow_steps_run_order
    ON project_workflow_steps(workflow_run_id, step_order);
