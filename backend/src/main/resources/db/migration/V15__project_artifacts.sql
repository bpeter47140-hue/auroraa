CREATE TABLE project_artifacts (
 id BIGSERIAL PRIMARY KEY,
 workspace_id BIGINT NOT NULL REFERENCES tenant_workspaces(id) ON DELETE CASCADE,
 project_id BIGINT NOT NULL REFERENCES venture_projects(id) ON DELETE CASCADE,
 artifact_type VARCHAR(40) NOT NULL,
 name VARCHAR(180) NOT NULL,
 relative_path VARCHAR(500) NOT NULL,
 description VARCHAR(1000),
 size_bytes BIGINT NOT NULL DEFAULT 0,
 created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_project_artifacts_project ON project_artifacts(workspace_id,project_id,created_at DESC);
CREATE UNIQUE INDEX uq_project_artifacts_path ON project_artifacts(workspace_id,project_id,relative_path);
