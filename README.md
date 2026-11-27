# Project AURORA v0.5

AURORA is a multi-workspace AI business operating platform. It coordinates specialist agents to research, plan and support business operations while keeping consequential actions under human approval.

## Current capabilities

- Java 25 / Spring Boot API and React + TypeScript dashboard.
- Persistent local H2 development database and PostgreSQL deployment profile.
- PostgreSQL schema managed by Flyway migrations V1–V24; Hibernate validates the deployed schema.
- Account registration, login, password changes, one-time password recovery links and hashed, expiring, revocable sessions.
- OWNER, ADMIN, MEMBER and VIEWER workspace roles and one-time invitations.
- Workspace-scoped tasks, approvals, businesses, audit events, shared memory, customer research and agent runs.
- Seven specialist agents: research, strategy, content, operations, frontend developer, QA and security review. QA/security agents produce advisory reports and never modify source or publish.
- Customer interview capture, evidence analysis and CSV export.
- Durable venture projects and multi-stage workflows with saved outputs, dependency checks, worker leases, safe pause/resume checkpoints, bounded owner/admin retry, cancellation and conservative recovery that avoids replaying possibly billed AI requests.
- Project source workspaces, editable frontend files, immutable SHA-256 version history, version-pinned reviews, owner/admin approval decisions, ZIP export and local preview builds.
- Website generation through a restricted React/TypeScript developer agent, owner/admin code-generation gate, disposable network-isolated preview builds, source QA, security review and browser QA reports/screenshots.
- Persistent per-workspace monthly member AI request allowance (default 100), provider token/cost ledger, per-business usage attribution and dashboard display; workspace owners and administrators are exempt from the application allowance, but provider charges still apply.
- Optional atomic platform/per-business monetary budget reservations, unknown-cost holds, owner/admin reconciliation with evidence, and persistent in-app 80%/90%/100% business budget alerts with acknowledgement. Monetary limits remain disabled until a defensible maximum per-request reservation is configured.
- Shared AI provider client with 5-second connect timeout, 45-second response timeout, and agent-specific output budgets (up to 4,200 tokens for research and 1,800 for other agents).
- Nginx reverse proxy with authentication rate limits, security headers and Content Security Policy.
- PostgreSQL backup/restore scripts and setup/start/status/stop helpers.

Agents are advisory. They do not publish content, contact customers, spend money or execute external actions. Shared memory is prompt context, not model training or automatic fact-checking. The Research Agent is wired to the OpenRouter web-search tool, but live-source and citation behavior still requires an end-to-end provider test. Invitation delivery currently uses Gmail SMTP with a Google App Password; the separate Gmail OAuth helper is not wired to the mail sender. Video generation and social publishing are not integrated.

The current Kenya student-chama desk-research findings, competitor scan, evidence gaps and 14-day validation plan are in `documentation/research/chama-market-validation-brief-2026-11-22.md`. The ready-to-use interview, consent, recruitment and observation materials are in `documentation/research/chama-validation-kit.md`. The research verdict is INSUFFICIENT EVIDENCE — RUN VALIDATION; no student demand or willingness to pay has been established.

## Run locally

Backend terminal:

    cd ~/Projects/aurora/backend
    ./mvnw spring-boot:run

Dashboard terminal:

    cd ~/Projects/aurora/frontend
    npm run dev

Open http://127.0.0.1:5173. The backend uses the persistent local H2 database under backend/data/. Keep this directory private and back it up before schema experiments.

## PostgreSQL migrations

The postgres profile uses Flyway and Hibernate schema validation (ddl-auto=validate). Migrations are in backend/src/main/resources/db/migration/:

- V1 creates the initial PostgreSQL schema.
- V2 assigns legacy unscoped operational rows to a migration workspace and makes workspace ownership mandatory.
- V3 creates the singleton database guard row used to serialize first-owner registration.
- V4 creates persistent per-workspace monthly AI usage counters.
- V5 expands agent-run input storage for longer multi-agent handoffs.
- V6 creates hashed, expiring, single-use password-reset tokens.
- V7 creates the provider token/cost usage ledger.
- V8 attributes new agent runs to optional business workspaces.
- V9 adds serialized monetary budget reservations and per-business budget policies.
- V10 adds auditable manual reconciliation evidence for unknown provider costs.
- V11 adds persistent, deduplicated monthly business budget alerts.
- V12 adds platform-admin flags and authenticated-session activity timestamps.
- V13 creates workspace-scoped venture projects.
- V14 creates durable project workflows and ordered steps.
- V15 creates project artifact metadata.
- V16 creates project review decisions.
- V17 corrects the review hash column to VARCHAR(64).
- V18 stores immutable project artifact version contents and SHA-256 hashes.
- V19 pins review requests to the exact artifact version.
- V20 adds workspace-scoped marketing campaigns and owner/admin review/queue states.
- V21 attributes provider usage and cost to workflow steps.
- V22 adds workflow approval gates before website source generation.
- V23 adds bounded, spend-safe owner/admin retry for failed workflow steps.
- V24 adds dependency checks and worker leases for conservative multi-instance coordination.

Fresh PostgreSQL databases are migrated automatically. Existing PostgreSQL databases previously created with Hibernate ddl-auto=update must not simply be pointed at Flyway: take a verified backup, compare the schema and adopt it through a reviewed migration procedure. Do not enable baseline-on-migrate as a shortcut.

The current H2 development database is not automatically copied to PostgreSQL. Export/import and identity/workspace ownership reconciliation are required before moving real local data.

## Container deployment

1. Install Docker Engine and the Docker Compose plugin.
2. Run ./scripts/setup.sh to create `.env` with unique 256-bit database, preview-runner, preview-signing and browser-QA secrets, detected Docker socket GID when available, and restrictive permissions (600).
3. Temporarily set AURORA_BOOTSTRAP_ENABLED=true in .env for initial owner registration only.
4. Run ./scripts/start.sh and open the dashboard URL it prints.
5. Create the first platform-owner account, immediately set AURORA_BOOTSTRAP_ENABLED=false, then run docker compose up -d backend dashboard.
6. Public signup remains available after the first account exists: each new person gets a separate private workspace and OWNER role, but never platform-admin access. Workspace owners can invite their own members from the Members screen. With Gmail SMTP configured using a Google App Password, AURORA sends a seven-day, single-use invitation link; otherwise the dashboard offers a manual share link.
7. For public HTTPS, point a real domain at the host, configure AURORA_DOMAIN in .env, open TCP 80/443 (and UDP 443 if using HTTP/3), set AURORA_APP_BASE_URL to the HTTPS origin, then run ./scripts/start-public.sh. Caddy handles certificate issuance/renewal; the dashboard remains loopback-bound and is reached publicly through Caddy.

The dashboard host mapping remains loopback-only; PostgreSQL and the backend have no public ports. Nginx rate-limits login, open account registration and invitation registration. For public access, the optional Caddy profile exposes TCP 80/443 (and UDP 443) and terminates HTTPS before proxying to the internal dashboard. Never expose the Vite development server.

**Local development status (2026-11-27):** Docker Compose runs PostgreSQL 17, the Spring Boot backend, React dashboard, isolated preview runner and isolated browser-QA service on the Arch development machine. Dashboard is bound to `127.0.0.1:8088`; database/backend/preview-runner/browser-QA ports are not published. The local PostgreSQL database has migrations through V24 applied. Local dashboard and system-status checks return HTTP 200. The existing Netlify frontend URL responds with HTTP 200; the Render API status request timed out during this check and must be re-tested after its service wakes. The preview-runner Docker socket access remains a production blocker. Public social publishing, production deployment and media generation are not connected.

## Gmail SMTP invitation delivery

The current invitation sender uses Spring JavaMailSender over Gmail SMTP with a Google App Password. It does not currently use the Gmail API or OAuth refresh tokens.

1. Enable 2-Step Verification on the sending Google account and create an App Password for AURORA.
2. Run `./scripts/configure-gmail.sh` and enter the sender address, 16-character App Password and the externally reachable HTTPS dashboard URL.
3. The script stores `AURORA_MAIL_ENABLED`, `AURORA_MAIL_FROM`, `AURORA_MAIL_USERNAME`, `AURORA_MAIL_PASSWORD` and `AURORA_APP_BASE_URL` in `.env`, then restricts the file to mode 600.
4. Restart the backend and test an invitation to a mailbox you control. Verify the recipient, link, expiry and manual fallback.

Never commit or share `.env` or the App Password. If delivery is disabled or fails, the Members screen retains a manual invitation-link fallback. Password-reset requests use a generic response to avoid account-existence disclosure; reset tokens are hashed, expire after 30 minutes, are single-use, and revoke all active sessions when used. Real SMTP delivery still requires configuration and verification. `scripts/configure-gmail-oauth.sh` is an unused helper: its OAuth credentials are not consumed by the current mail sender. Do not use it as the active email setup until OAuth delivery is implemented and tested.

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
- AURORA_AI_MODEL (Docker Compose defaults to openrouter/free; direct Spring Boot runs default to openai/gpt-4.1-mini)
- AURORA_AI_BASE_URL (Docker Compose defaults to https://openrouter.ai/api/v1)
- AURORA_AI_MONTHLY_REQUEST_LIMIT (defaults to 100 member requests per workspace per UTC month)

Provider availability and charges vary. For ordinary members, the quota counts attempted calls, including failed provider requests. OWNER and ADMIN calls bypass this application allowance but remain billable by the provider. A four-stage workflow makes four provider calls. Output budgets are up to 4,200 tokens for Research and 1,800 for other agents. Monetary budget reservations and persistent in-app alerts are implemented, but remain disabled until a defensible per-request maximum and provider-side billing hard limit are configured. Provider invoice reconciliation remains manual. For OpenRouter, set a dedicated per-environment API-key credit cap in the provider dashboard; AURORA cannot enforce or verify that external setting. The current OpenRouter plan comparison lists Budgets & Spend Controls on Standard/Business/Enterprise, not Free.

## Backups and restore

After configuring .env and starting the Compose stack:

    ./scripts/backup-postgres.sh

Backups are written to backups/ with restrictive permissions and a SHA-256 sidecar; the backup script also checks that the PostgreSQL archive can be listed before reporting success. Restore refuses a missing or mismatched checksum and validates the archive before prompting. Restore applies with `--exit-on-error` and `--single-transaction`. Store copies off the laptop in encrypted, access-controlled storage. A backup is not proven until a restore has been tested against a separate database.

Restore is destructive and requires typing RESTORE:

    ./scripts/restore-postgres.sh backups/aurora-postgres-YYYYMMDDTHHMMSSZ.dump

See documentation/operations/production-runbook.md for operational procedures and restore drills.

## Before a public release

- Configure a real domain and verify the optional Caddy HTTPS profile obtains a trusted certificate.
- Configure a defensible maximum per-request reservation and a dedicated OpenRouter API key with an independent provider-side credit cap before enabling monetary budgets. Keep automatic top-up off unless explicitly approved.
- Verify the complete V1–V24 migration chain on a fresh PostgreSQL database and run a full clean Compose startup before a public release.
- Review session lifetime, token storage and account recovery for the intended threat model.
- Configure and verify Gmail SMTP App Password invitation and password-reset email delivery.
- Complete an end-to-end live Research Agent run and verify every returned source URL, publication date, claim label and citation; web-search tool wiring alone is not proof of citation quality.
- Add image/design/video generation and social publishing integrations with explicit owner approval.
- Configure encrypted off-site backups, external secret management, monitoring and alerting.
- Complete cross-workspace security review, multi-account browser testing and dependency/container scanning.
- Complete privacy, data-retention and legal reviews before storing real customer data.
- Run the GitHub Actions workflow and protect the main branch.

AURORA is a prototype and is not production-certified. The Netlify frontend is publicly reachable, but the Render API timed out in the latest external check; backend availability and cloud configuration must be verified before inviting users. Cloud hosting, AI-provider accounts, creative-generation services and social account authorizations require user-owned credentials and explicit setup.
