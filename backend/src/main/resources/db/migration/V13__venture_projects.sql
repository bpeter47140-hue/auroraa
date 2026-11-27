CREATE TABLE venture_projects (
    id BIGSERIAL PRIMARY KEY,
    workspace_id BIGINT NOT NULL REFERENCES tenant_workspaces(id) ON DELETE CASCADE,
    name VARCHAR(160) NOT NULL,
    goal VARCHAR(4000) NOT NULL,
    project_type VARCHAR(40),
    status VARCHAR(24) NOT NULL DEFAULT 'PLANNING',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_venture_projects_workspace_updated
    ON venture_projects(workspace_id, updated_at DESC);
