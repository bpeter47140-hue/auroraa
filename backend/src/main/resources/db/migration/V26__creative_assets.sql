CREATE TABLE creative_assets (
    id BIGSERIAL PRIMARY KEY,
    workspace_id BIGINT NOT NULL REFERENCES tenant_workspaces(id) ON DELETE CASCADE,
    project_id BIGINT NOT NULL REFERENCES venture_projects(id) ON DELETE CASCADE,
    name VARCHAR(180) NOT NULL,
    media_type VARCHAR(40) NOT NULL CHECK (media_type IN ('IMAGE','VIDEO')),
    content_type VARCHAR(100) NOT NULL,
    storage_key VARCHAR(500) NOT NULL,
    size_bytes BIGINT NOT NULL CHECK (size_bytes > 0 AND size_bytes <= 5242880),
    notes VARCHAR(1000),
    uploaded_by VARCHAR(254) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_creative_assets_workspace_project_created ON creative_assets(workspace_id,project_id,created_at DESC);
