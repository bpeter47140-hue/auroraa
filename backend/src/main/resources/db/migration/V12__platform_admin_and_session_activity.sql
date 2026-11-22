ALTER TABLE aurora_users
    ADD COLUMN platform_admin BOOLEAN NOT NULL DEFAULT FALSE;

-- The original first account is the platform owner for existing installations.
UPDATE aurora_users
SET platform_admin = TRUE
WHERE id = (SELECT id FROM aurora_users ORDER BY created_at, id LIMIT 1);

ALTER TABLE aurora_users
    ADD COLUMN last_login_at TIMESTAMP WITH TIME ZONE;

UPDATE aurora_users u
SET last_login_at = (SELECT MAX(s.created_at) FROM auth_sessions s WHERE s.user_id = u.id)
WHERE EXISTS (SELECT 1 FROM auth_sessions s WHERE s.user_id = u.id);

ALTER TABLE auth_sessions
    ADD COLUMN last_seen_at TIMESTAMP WITH TIME ZONE;

-- Existing sessions receive their login time as the initial activity timestamp.
UPDATE auth_sessions SET last_seen_at = created_at WHERE last_seen_at IS NULL;

ALTER TABLE auth_sessions
    ALTER COLUMN last_seen_at SET NOT NULL;

CREATE INDEX idx_auth_session_online
    ON auth_sessions(revoked_at, expires_at, last_seen_at);
