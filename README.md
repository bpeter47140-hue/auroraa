# Project AURORA v0.5

AURORA is a multi-workspace AI business operating platform. It coordinates specialist agents to research, plan and support business operations while keeping consequential actions under human approval.

## Current capabilities

- Java 25 / Spring Boot API and React + TypeScript dashboard.
- Persistent local H2 development database and PostgreSQL deployment profile.
- PostgreSQL schema managed by Flyway migrations V1–V4; Hibernate validates the deployed schema.
- Account registration, login, password changes and hashed, expiring, revocable sessions.
- OWNER, ADMIN, MEMBER and VIEWER workspace roles and one-time invitations.
- Workspace-scoped tasks, approvals, businesses, audit events, shared memory, customer research and agent runs.
- Specialist agents for research, strategy, content and operations, plus a sequential Chama validation workflow.
- Customer interview capture, evidence analysis and CSV export.
- Persistent per-workspace monthly AI request quota (default 100), usage API and dashboard display.
- Shared AI provider client with 5-second connect timeout, 45-second response timeout and 1,400-token output cap.
- Nginx reverse proxy with authentication rate limits, security headers and Content Security Policy.
- PostgreSQL backup/restore scripts and setup/start/status/stop helpers.

Agents are advisory. They do not publish content, contact customers, spend money or execute external actions. Shared memory is prompt context, not model training or automatic fact-checking. Live web research, email delivery, video generation and social publishing are not yet integrated.

## Run locally

Backend terminal:

    cd ~/Projects/aurora/backend
    ./mvnw spring-boot:run

Dashboard terminal:

    cd ~/Projects/aurora/frontend
    npm run dev -- --host 0.0.0.0

Open http://localhost:5173. The backend uses the persistent local H2 database under backend/data/. Keep this directory private and back it up before schema experiments.

## PostgreSQL migrations

The postgres profile uses Flyway and Hibernate schema validation (ddl-auto=validate). Migrations are in backend/src/main/resources/db/migration/:

- V1 creates the initial PostgreSQL schema.
- V2 assigns legacy unscoped operational rows to a migration workspace and makes workspace ownership mandatory.
- V3 creates the singleton database guard row used to serialize first-owner registration.
- V4 creates persistent per-workspace monthly AI usage counters.

Fresh PostgreSQL databases are migrated automatically. Existing PostgreSQL databases previously created with Hibernate ddl-auto=update must not simply be pointed at Flyway: take a verified backup, compare the schema and adopt it through a reviewed migration procedure. Do not enable baseline-on-migrate as a shortcut.

The current H2 development database is not automatically copied to PostgreSQL. Export/import and identity/workspace ownership reconciliation are required before moving real local data.

## Container deployment

1. Install Docker Engine and the Docker Compose plugin.
2. Run ./scripts/setup.sh to create .env with a random database password and restrictive permissions.
3. Temporarily set AURORA_BOOTSTRAP_ENABLED=true in .env for initial owner registration only.
4. Run ./scripts/start.sh and open the dashboard URL it prints.
5. Create the first owner account, immediately set AURORA_BOOTSTRAP_ENABLED=false, then run docker compose up -d backend dashboard.
6. Invite additional users from the Members screen. With Gmail OAuth configured, AURORA sends a seven-day, single-use invitation link; otherwise the dashboard offers a manual share link.

Only the dashboard reverse proxy is published by Compose; PostgreSQL and the backend remain on the internal Compose network. Nginx rate-limits login, first-owner registration and invitation registration. It does not provide HTTPS. Put a TLS-terminating reverse proxy or managed HTTPS ingress in front of the published dashboard before internet exposure.

**Deployment status:** A clean PostgreSQL 18 database has been verified against Flyway V1–V4; all migrations applied successfully and Hibernate schema validation passed. The full Compose build has not been run on this laptop because Docker/Podman are not installed.

## Verification

- Backend: 25 tests passed, with zero failures and zero errors. Includes prompt-data escaping, monthly quota enforcement and cross-workspace read/update/delete isolation.
- Frontend: Oxlint and TypeScript/Vite production build are run for each release check.
- PostgreSQL: clean database migration and application health are verified separately from H2 tests.
- Docker Compose build and GitHub Actions execution remain unverified on this laptop.

## Gmail OAuth email delivery

AURORA sends workspace invitations through the Gmail API using OAuth 2.0. It does not use SMTP or a Google App Password.

1. In Google Cloud Console, create/select a project and enable the Gmail API.
2. Configure the OAuth consent screen. If the app is in Testing mode, add the Gmail sender account as a test user. Google may expire refresh tokens after seven days for external apps in Testing mode.
3. Create an OAuth Client ID of type Desktop app and download its JSON file.
4. Run ./scripts/configure-gmail-oauth.sh on the AURORA laptop and enter the path to that downloaded JSON file. It opens Google authorization in your browser, obtains a refresh token, and stores the client details in .env with mode 600.
5. Set AURORA_APP_BASE_URL to the dashboard URL recipients can reach. localhost is only suitable for testing on the same computer.
6. Restart the backend. Test by inviting an address you control, then verify the received link and expiry.

The requested OAuth scope is gmail.send, which permits sending mail but not reading inbox messages. Treat the OAuth client secret and refresh token like passwords; never share them or commit .env. If Google blocks the consent flow, check the OAuth audience/test-user settings and Gmail API enablement. If email delivery is disabled or fails, the dashboard retains its manual invitation-link fallback. Password-reset email is not implemented.

For local development, load environment variables before starting Spring Boot:

    set -a
    source .env
    set +a
    cd backend
    ./mvnw spring-boot:run

For Docker deployment, run ./scripts/start.sh after Docker is installed.

## AI provider configuration

Configure on the backend host only:

- AURORA_AI_API_KEY
- AURORA_AI_MODEL (defaults to openrouter/free)
- AURORA_AI_BASE_URL (defaults to https://openrouter.ai/api/v1)
- AURORA_AI_MONTHLY_REQUEST_LIMIT (defaults to 100 requests per workspace per UTC month)

Provider availability and charges vary. The quota counts attempted calls, including failed provider requests. A four-stage workflow consumes four requests. Output is limited to 1,400 tokens per call. Provider-specific monetary budgets and cost reconciliation are not implemented yet.

## Backups and restore

After configuring .env and starting the Compose stack:

    ./scripts/backup-postgres.sh

Backups are written to backups/ with restrictive permissions and a SHA-256 sidecar. Store copies off the laptop in encrypted, access-controlled storage. A backup is not proven until a restore has been tested against a separate database.

Restore is destructive and requires typing RESTORE:

    ./scripts/restore-postgres.sh backups/aurora-postgres-YYYYMMDDTHHMMSSZ.dump

See documentation/operations/production-runbook.md for operational procedures and restore drills.

## Before a public release

- Configure a real domain and HTTPS ingress.
- Review session lifetime, token storage and account recovery for the intended threat model.
- Configure and verify Gmail OAuth invitation delivery; implement account recovery email and token flow.
- Add approved live source retrieval and citation verification for Research Agent.
- Add marketing video generation and social publishing integrations with explicit owner approval.
- Configure encrypted off-site backups, external secret management, monitoring and alerting.
- Complete cross-workspace security review, multi-account browser testing and dependency/container scanning.
- Complete privacy, data-retention and legal reviews before storing real customer data.
- Run the GitHub Actions workflow and protect the main branch.

AURORA is a private prototype, not a production-certified or publicly hosted service. Cloud hosting, provider accounts and social account authorizations require user-owned credentials and explicit setup.
