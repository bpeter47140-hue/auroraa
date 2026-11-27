CREATE TABLE project_reviews (
 id BIGSERIAL PRIMARY KEY,
 workspace_id BIGINT NOT NULL REFERENCES tenant_workspaces(id) ON DELETE CASCADE,
 project_id BIGINT NOT NULL REFERENCES venture_projects(id) ON DELETE CASCADE,
 artifact_id BIGINT NOT NULL REFERENCES project_artifacts(id) ON DELETE CASCADE,
 artifact_name VARCHAR(180) NOT NULL,
 artifact_path VARCHAR(500) NOT NULL,
 artifact_sha256 CHAR(64) NOT NULL,
 status VARCHAR(24) NOT NULL,
 requested_by VARCHAR(254) NOT NULL,
 reviewed_by VARCHAR(254),
 feedback VARCHAR(4000),
 created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
 reviewed_at TIMESTAMP WITH TIME ZONE,
 CONSTRAINT ck_project_reviews_status CHECK (status IN ('PENDING_REVIEW','APPROVED','CHANGES_REQUESTED','REJECTED'))
);
CREATE INDEX idx_project_reviews_project ON project_reviews(workspace_id,project_id,created_at DESC);
CREATE INDEX idx_project_reviews_pending ON project_reviews(workspace_id,status,created_at DESC);
