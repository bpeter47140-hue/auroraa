CREATE TABLE project_artifact_versions (
 id BIGSERIAL PRIMARY KEY,
 workspace_id BIGINT NOT NULL REFERENCES tenant_workspaces(id) ON DELETE CASCADE,
 project_id BIGINT NOT NULL REFERENCES venture_projects(id) ON DELETE CASCADE,
 artifact_id BIGINT NOT NULL REFERENCES project_artifacts(id) ON DELETE CASCADE,
 version_number INTEGER NOT NULL,
 content TEXT NOT NULL,
 sha256 VARCHAR(64) NOT NULL,
 created_by VARCHAR(254) NOT NULL,
 created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT uq_project_artifact_version UNIQUE (workspace_id, artifact_id, version_number)
);
CREATE INDEX idx_project_artifact_versions_history ON project_artifact_versions(workspace_id,artifact_id,version_number DESC);
