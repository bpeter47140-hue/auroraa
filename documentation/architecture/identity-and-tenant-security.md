# AURORA identity, access control and tenant isolation

Status: Core identity and tenant controls implemented; production rollout gates remain open
Date: 2026-11-22

## 1. Current state (prototype)

The cloud profile uses account sessions and workspace membership authorization. The legacy shared owner token is isolated behind the explicit `legacy-owner-token` profile and is not the multi-user deployment mode. First-owner bootstrap is disabled by default in the cloud profile and must be temporarily enabled by the operator during initial setup.

Implemented controls include normalized unique user emails, BCrypt password hashes, hashed opaque session tokens with expiry/revocation, workspace memberships and roles, workspace-scoped business records and repository access, tenant-scoped agent context, invitation flows, and tests for role enforcement and cross-workspace isolation. The first-owner registration endpoint now locks a pre-created singleton database row inside the registration transaction; a concurrency test verifies that only one of two simultaneous first-owner registrations succeeds. PostgreSQL creates this row through Flyway V3; the local H2 profile seeds it during application startup. The test suite now includes HTTP-level checks that authenticated users cannot select another workspace and that missing or revoked sessions are rejected.

This is not a production security certification. The first-owner bootstrap setting must still be disabled immediately after setup. Do not open public registration or invite external customers until the remaining release gates—especially deployment security, rate limiting, data migration, backup/restore, monitoring and privacy review—are completed.

## 2. Target tenancy model

Use a tenant boundary called Workspace.

- User: an individual login identity, with a normalized unique email, password hash, status, and timestamps.
- Workspace: a customer/business boundary with a display name, status, plan/usage metadata, and timestamps.
- WorkspaceMembership: joins a user to a workspace with a role (OWNER, ADMIN, MEMBER, VIEWER); unique (workspace_id, user_id).
- AuthSession: opaque random bearer token represented in storage only by a cryptographic hash; expiry, revocation, creation time, and user reference.
- All business-owned records carry a non-null workspace_id foreign key. Audit events also record the acting user where available.

A user may belong to multiple workspaces. A workspace may have multiple members. Never infer workspace access from a client-supplied workspace ID alone; verify membership and role on every request.

## 3. Authentication

- Use Spring Security and a password encoder (Argon2id preferred; BCrypt acceptable if Argon2 support is not practical).
- Never store plaintext passwords or raw session tokens.
- Login issues a cryptographically random opaque token; store only its SHA-256 hash, with a finite expiry and revocation support.
- Keep access tokens out of localStorage. Prefer a Secure, HttpOnly, SameSite cookie in production; if bearer headers are retained for the local prototype, keep them in memory and require HTTPS outside localhost.
- Add login throttling, generic login errors, password length limits, account disablement and session revocation.
- Registration is invite-only during early access. Public signup is a separately approved product decision.
- Retain the existing owner token only as a temporary, explicitly documented break-glass operator credential. It must not become a shared customer login.

## 4. Authorization and tenant isolation

- Resolve authenticated user and selected workspace server-side.
- Check membership and role on every read/write.
- Repository queries must be scoped by workspace, not fetch globally and filter in the UI.
- Return 404 for resources outside the caller's workspace to avoid leaking existence.
- VIEWER: read only; MEMBER: normal work; ADMIN: manage workspace and members; OWNER: billing, ownership and destructive workspace actions.
- Require re-authentication or stronger confirmation for sensitive account/workspace actions.
- Agent prompts and retrieval must include only data from the active workspace. Never share memory or customer research between tenants.
- Audit sensitive actions with workspace and actor identifiers, while avoiding secrets and unnecessary personal data in logs.

## 5. Data migration plan

Do not add nullable tenant IDs and expose the app in the same deployment.

1. Introduce user, workspace, membership and session tables.
2. Create one initial owner and one default workspace for the existing installation.
3. Backfill every existing business-owned record to that workspace in a controlled migration.
4. Verify no business-owned row is left without a workspace and record counts match.
5. Add non-null constraints and foreign keys.
6. Update every repository/controller/service to scope by active workspace.
7. Add negative tests proving another workspace cannot read, update or delete records by guessing IDs.
8. Only then enable invite-based onboarding.

Use versioned database migrations (Flyway or Liquibase) for production schema changes. Hibernate ddl-auto=update is acceptable only for local development; production should validate a migration-managed schema.

## 6. Required security tests before external users

- Unauthenticated API requests are rejected.
- Invalid, expired, revoked and disabled-account sessions are rejected.
- Passwords are never returned by API responses or stored in plaintext.
- A user cannot access a workspace without membership.
- Viewer cannot mutate records.
- Cross-workspace list, detail, update and delete attempts disclose no data.
- Agent runs, memory context and research context never cross workspace boundaries.
- Workspace creation and invitation flows enforce role permissions.
- Security headers, HTTPS, request-size limits, rate limits, backups and restore procedure are verified.

## 7. Rollout gates

- Gate A: identity/session design and schema.
- Gate B: existing data safely migrated into the initial workspace.
- Gate C: every endpoint and agent context is tenant-scoped.
- Gate D: security and isolation tests pass.
- Gate E: invite-only external pilot with monitoring and backup/restore tested.

Until Gate D is complete, AURORA remains private to its owner. A temporary tunnel is not production hosting.
