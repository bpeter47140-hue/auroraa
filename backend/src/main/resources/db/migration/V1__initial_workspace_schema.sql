CREATE TABLE tenant_workspaces (
 id BIGSERIAL PRIMARY KEY, name VARCHAR(140) NOT NULL, status VARCHAR(16) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL
);
CREATE TABLE aurora_users (
 id BIGSERIAL PRIMARY KEY, email VARCHAR(254) NOT NULL, password_hash VARCHAR(100) NOT NULL,
 display_name VARCHAR(100) NOT NULL, status VARCHAR(16) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL,
 CONSTRAINT uk_aurora_users_email UNIQUE(email)
);
CREATE TABLE workspace_memberships (
 id BIGSERIAL PRIMARY KEY, workspace_id BIGINT NOT NULL REFERENCES tenant_workspaces(id) ON DELETE CASCADE,
 user_id BIGINT NOT NULL REFERENCES aurora_users(id) ON DELETE CASCADE,
 role VARCHAR(16) NOT NULL, created_at TIMESTAMPTZ NOT NULL,
 CONSTRAINT uk_workspace_membership_user_workspace UNIQUE(workspace_id,user_id)
);
CREATE INDEX idx_membership_user ON workspace_memberships(user_id);
CREATE INDEX idx_membership_workspace ON workspace_memberships(workspace_id);
CREATE TABLE auth_sessions (
 id BIGSERIAL PRIMARY KEY, user_id BIGINT NOT NULL REFERENCES aurora_users(id) ON DELETE CASCADE,
 token_hash VARCHAR(64) NOT NULL, expires_at TIMESTAMPTZ NOT NULL, revoked_at TIMESTAMPTZ,
 created_at TIMESTAMPTZ NOT NULL, CONSTRAINT uk_auth_session_token_hash UNIQUE(token_hash)
);
CREATE INDEX idx_auth_session_user_expiry ON auth_sessions(user_id,expires_at);
CREATE TABLE workspace_invitations (
 id BIGSERIAL PRIMARY KEY, workspace_id BIGINT NOT NULL REFERENCES tenant_workspaces(id) ON DELETE CASCADE,
 email VARCHAR(254) NOT NULL, role VARCHAR(16) NOT NULL, token_hash VARCHAR(64) NOT NULL,
 expires_at TIMESTAMPTZ NOT NULL, created_at TIMESTAMPTZ NOT NULL,
 accepted_at TIMESTAMPTZ, revoked_at TIMESTAMPTZ,
 CONSTRAINT uk_invitation_token_hash UNIQUE(token_hash)
);
CREATE INDEX idx_invitation_workspace ON workspace_invitations(workspace_id);
CREATE INDEX idx_invitation_email ON workspace_invitations(email);
CREATE TABLE work_tasks (
 id BIGSERIAL PRIMARY KEY, workspace_id BIGINT, title VARCHAR(160) NOT NULL, description VARCHAR(2000),
 priority VARCHAR(20) NOT NULL, status VARCHAR(24) NOT NULL, created_at TIMESTAMPTZ NOT NULL,
 updated_at TIMESTAMPTZ NOT NULL, CONSTRAINT fk_work_tasks_workspace FOREIGN KEY(workspace_id) REFERENCES tenant_workspaces(id)
);
CREATE INDEX idx_work_tasks_workspace_created ON work_tasks(workspace_id,created_at DESC);
CREATE TABLE approval_requests (
 id BIGSERIAL PRIMARY KEY, workspace_id BIGINT, title VARCHAR(160) NOT NULL, description VARCHAR(2000) NOT NULL,
 action_type VARCHAR(40) NOT NULL, status VARCHAR(24) NOT NULL, decision_note VARCHAR(1000),
 created_at TIMESTAMPTZ NOT NULL, decided_at TIMESTAMPTZ,
 CONSTRAINT fk_approval_requests_workspace FOREIGN KEY(workspace_id) REFERENCES tenant_workspaces(id)
);
CREATE INDEX idx_approval_requests_workspace_created ON approval_requests(workspace_id,created_at DESC);
CREATE TABLE business_workspaces (
 id BIGSERIAL PRIMARY KEY, workspace_id BIGINT, name VARCHAR(140) NOT NULL, description VARCHAR(2500),
 status VARCHAR(24) NOT NULL, goal VARCHAR(1000), created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL,
 CONSTRAINT fk_business_workspaces_workspace FOREIGN KEY(workspace_id) REFERENCES tenant_workspaces(id)
);
CREATE INDEX idx_business_workspaces_workspace_created ON business_workspaces(workspace_id,created_at DESC);
CREATE TABLE memory_entries (
 id BIGSERIAL PRIMARY KEY, workspace_id BIGINT, category VARCHAR(100) NOT NULL, title VARCHAR(180) NOT NULL,
 content VARCHAR(3000) NOT NULL, source VARCHAR(180), created_at TIMESTAMPTZ NOT NULL,
 CONSTRAINT fk_memory_entries_workspace FOREIGN KEY(workspace_id) REFERENCES tenant_workspaces(id)
);
CREATE INDEX idx_memory_entries_workspace_created ON memory_entries(workspace_id,created_at DESC);
CREATE TABLE customer_insights (
 id BIGSERIAL PRIMARY KEY, workspace_id BIGINT, insight_type VARCHAR(40) NOT NULL,
 participant_label VARCHAR(100) NOT NULL, problem VARCHAR(2000) NOT NULL, current_solution VARCHAR(1500),
 frequency VARCHAR(1000), willingness VARCHAR(1000), evidence VARCHAR(2000), validation_status VARCHAR(30) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL,
 CONSTRAINT fk_customer_insights_workspace FOREIGN KEY(workspace_id) REFERENCES tenant_workspaces(id)
);
CREATE INDEX idx_customer_insights_workspace_created ON customer_insights(workspace_id,created_at DESC);
CREATE TABLE agent_runs (
 id BIGSERIAL PRIMARY KEY, workspace_id BIGINT, agent_id VARCHAR(60) NOT NULL, agent_name VARCHAR(40) NOT NULL,
 status VARCHAR(24) NOT NULL, input VARCHAR(5000) NOT NULL, output VARCHAR(30000), error VARCHAR(1200),
 model VARCHAR(160), started_at TIMESTAMPTZ NOT NULL, finished_at TIMESTAMPTZ,
 CONSTRAINT fk_agent_runs_workspace FOREIGN KEY(workspace_id) REFERENCES tenant_workspaces(id)
);
CREATE INDEX idx_agent_runs_workspace_started ON agent_runs(workspace_id,started_at DESC);
CREATE TABLE audit_events (
 id BIGSERIAL PRIMARY KEY, workspace_id BIGINT, event_type VARCHAR(60) NOT NULL, entity_type VARCHAR(40) NOT NULL,
 entity_id VARCHAR(80) NOT NULL, details VARCHAR(1000) NOT NULL, created_at TIMESTAMPTZ NOT NULL,
 CONSTRAINT fk_audit_events_workspace FOREIGN KEY(workspace_id) REFERENCES tenant_workspaces(id)
);
CREATE INDEX idx_audit_events_workspace_created ON audit_events(workspace_id,created_at DESC);
