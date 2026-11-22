# AURORA release readiness checklist

Status: Private prototype. Do not expose to the public internet yet.

## Identity and access

- [x] User accounts, password hashing and session expiry/revocation
- [x] Workspace and membership roles (OWNER, ADMIN, MEMBER, VIEWER)
- [x] Invite-based onboarding after first-owner setup
- [x] Server-side authentication and workspace membership checks on protected API routes
- [x] Workspace-scoped agent memory and customer-research retrieval
- [x] Cross-workspace repository isolation tests
- [x] HTTP integration tests for workspace selection, unauthenticated access and revoked sessions
- [x] Concurrent first-owner registration serialized by a database lock
- [x] Cross-workspace update/delete isolation tests for tasks, businesses, memories, customer research and approvals
- [ ] Independent security review and multi-account browser test

## Data and operations

- [x] Versioned PostgreSQL migrations (Flyway V1–V4)
- [x] Hibernate schema validation in PostgreSQL profile
- [x] PostgreSQL backup and restore scripts with SHA-256 verification
- [ ] Automated encrypted backups and off-site retention
- [ ] Restore drill against a separate database, repeated and recorded
- [ ] H2-to-PostgreSQL export/import and reconciliation for existing local data
- [ ] Data export and account/workspace deletion policy
- [ ] Retention policy for research, audit and agent history

## Application security

- [x] Login errors do not reveal whether an email exists
- [x] Login, first-owner registration and invitation-registration rate limits at the Nginx layer
- [x] Request-size limits and DTO input validation
- [x] Baseline security headers and Content Security Policy at the Nginx edge
- [x] Secrets supplied through backend environment variables, not frontend assets
- [x] Workspace-scoped audit events for recorded application actions
- [x] Persistent per-workspace monthly AI request quotas with database locking and a usage API
- [ ] HTTPS, secure cookie policy and production token-storage review
- [ ] Dependency and container-image scanning
- [ ] Review API error handling to ensure provider and internal details are not exposed
- [ ] Record acting user IDs for privileged audit events

## AI governance and costs

- [x] Agents are advisory and cannot publish, contact customers, spend money or execute external actions
- [x] Human approval records exist for consequential proposed actions
- [x] Provider limitations and lack of live web research are disclosed
- [x] Per-workspace monthly AI request allowance (configurable; default 100 requests) and dashboard usage display
- [ ] Provider-specific monetary budget alerts and token/cost reconciliation
- [x] AI provider connection and response timeouts (5s connect, 45s response)
- [x] Provider failure messages are sanitized before persistence or API responses
- [x] Automatic provider retries disabled to avoid duplicate charges; network timeouts bound request duration
- [x] Prompt-injection boundary instructions and markup escaping for shared memory and customer research; user briefs explicitly treated as untrusted task data
- [x] Optional SMTP workspace-invitation delivery path with manual-link fallback (real-provider delivery still requires configuration and verification)
- [ ] Account-recovery email and password-reset token flow
- [ ] Live source retrieval and citation verification for the Research Agent
- [ ] Marketing video generation and social publishing integrations, with explicit approval

## Production operations

- [x] Application health endpoint and container health checks are configured
- [x] CI workflow is defined for backend tests, PostgreSQL startup and frontend checks
- [ ] Run the CI workflow on GitHub and protect the main branch
- [ ] Docker Compose build and end-to-end smoke test
- [ ] Production deployment, domain and HTTPS ingress
- [ ] External secret management
- [ ] Production metrics, structured logs, alerting and incident response
- [ ] Rollback plan and tested disaster-recovery procedure
- [ ] Pilot user support and feedback process
- [ ] Privacy, data-retention and legal review before storing real customer data

## Verified locally on 2026-11-22

- Backend: 25 tests passed, 0 failures, 0 errors, including prompt-data escaping, persisted monthly AI quota enforcement and cross-workspace write/delete isolation.
- SMTP invitation delivery code and manual fallback compiled successfully; mail health checks remain disabled until SMTP is enabled. No live email provider credentials were configured or tested.
- Frontend: Oxlint passed with 0 warnings/errors.
- Frontend: TypeScript production build and Vite build passed.
- PostgreSQL 18: clean database migrated through Flyway V1–V4; Hibernate schema validation passed and application health returned UP.
- Cloud bootstrap: confirmed open when explicitly enabled for first-owner setup.
- Docker/Podman are not installed on the development laptop; the Compose build and GitHub Actions workflow have not been executed here.

AURORA remains a private prototype until the outstanding release gates are completed. A working local build is not a production certification.
