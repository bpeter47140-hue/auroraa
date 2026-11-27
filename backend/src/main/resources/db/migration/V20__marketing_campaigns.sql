CREATE TABLE marketing_campaigns (
    id BIGSERIAL PRIMARY KEY,
    workspace_id BIGINT NOT NULL REFERENCES tenant_workspaces(id) ON DELETE CASCADE,
    project_id BIGINT NOT NULL REFERENCES venture_projects(id) ON DELETE CASCADE,
    title VARCHAR(180) NOT NULL,
    channel VARCHAR(24) NOT NULL,
    format VARCHAR(24) NOT NULL,
    content TEXT NOT NULL,
    call_to_action VARCHAR(500),
    status VARCHAR(24) NOT NULL DEFAULT 'DRAFT',
    created_by VARCHAR(254) NOT NULL,
    reviewed_by VARCHAR(254),
    feedback VARCHAR(4000),
    scheduled_at TIMESTAMP WITH TIME ZONE,
    approved_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_marketing_campaign_status CHECK (status IN ('DRAFT','PENDING_REVIEW','APPROVED','CHANGES_REQUESTED','REJECTED','SCHEDULED')),
    CONSTRAINT ck_marketing_campaign_channel CHECK (channel IN ('INSTAGRAM','FACEBOOK','LINKEDIN','TIKTOK','YOUTUBE','X','EMAIL','OTHER')),
    CONSTRAINT ck_marketing_campaign_format CHECK (format IN ('POST','STORY','REEL','VIDEO','CAROUSEL','EMAIL','OTHER'))
);
CREATE INDEX idx_marketing_campaigns_workspace_project_created ON marketing_campaigns(workspace_id, project_id, created_at DESC);
CREATE INDEX idx_marketing_campaigns_scheduled ON marketing_campaigns(workspace_id, status, scheduled_at) WHERE status = 'SCHEDULED';
