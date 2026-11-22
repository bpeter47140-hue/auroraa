# AURORA v0.5 operations runbook

## Local container setup

1. Install Docker Engine and the Docker Compose plugin.
2. Run `./scripts/setup.sh`. It creates a private `.env` with a random database password and leaves first-owner registration disabled.
3. Temporarily change `AURORA_BOOTSTRAP_ENABLED=false` to `true` in `.env` for the first registration only.
4. Run `./scripts/start.sh` and open the printed dashboard URL.
5. Create the owner account. Immediately change `AURORA_BOOTSTRAP_ENABLED=true` back to `false`.
6. Run `docker compose up -d backend dashboard` to apply the closed bootstrap setting.
7. For email invitations, configure Gmail OAuth using ./scripts/configure-gmail-oauth.sh and set AURORA_APP_BASE_URL to the externally reachable HTTPS dashboard URL. If email is disabled or delivery fails, the Members screen provides a manual invitation link.
8. Configure an AI provider key only if model execution is wanted. Set a monthly workspace request allowance in `AURORA_AI_MONTHLY_REQUEST_LIMIT` (default 100). A four-stage workflow consumes four requests.

## Daily operations

- `./scripts/status.sh` — inspect container state.
- `docker compose logs --tail=150 backend` — inspect backend logs.
- `docker compose logs --tail=150 dashboard` — inspect reverse-proxy logs.
- `./scripts/backup-postgres.sh` — create a compressed PostgreSQL backup and SHA-256 sidecar.
- `./scripts/stop.sh` — stop services without deleting the database volume.
- `docker compose up -d` — restart stopped services.

Never use `docker compose down -v` on a database you want to keep.

## Restore drill

1. Create a fresh isolated PostgreSQL database or separate Compose project.
2. Verify the backup sidecar with `sha256sum -c backup.dump.sha256`.
3. Restore into the isolated database, not the live database.
4. Start a matching AURORA release against that database.
5. Verify health, login, workspace membership, tasks, approvals, memories and audit history.
6. Record the date, backup identifier, migration version and restore outcome.

The provided restore script targets the configured Compose database and is destructive after confirmation. Do not use it for routine restore drills against production.

## Email invitations

AURORA uses the Gmail API with OAuth 2.0 and the narrow gmail.send scope. Enable Gmail API in Google Cloud, configure the OAuth consent screen and create a Desktop app OAuth client. Run ./scripts/configure-gmail-oauth.sh to authorize the sender account and save the client ID, client secret and refresh token to the mode-600 .env file. The setup script binds a temporary callback listener to localhost; do not expose that port publicly. If an external OAuth app remains in Testing mode, Google may expire refresh tokens after seven days. Never commit or share OAuth secrets. Use an HTTPS dashboard URL for real recipients. Test delivery to a controlled mailbox, verify the invitation link and expiry, then revoke the test invitation. AURORA currently sends workspace invitations only; password-reset email is not yet implemented. If Gmail delivery fails, the invitation remains pending and can be shared manually.

## First-owner recovery

If no owner exists and bootstrap has been disabled, stop the backend, take a verified database backup, and follow a reviewed administrative recovery procedure. Do not expose the bootstrap endpoint publicly or leave registration open.

## Public deployment gates

Before public internet exposure, provision a domain, TLS ingress, external secret management, monitored backups, alerts and a tested restore plan. Review rate limits, account recovery, session storage, privacy and data retention. This repository does not provision cloud infrastructure, send password-reset email, generate marketing videos, or publish to social networks. Workspace invitation email is available when Gmail OAuth is configured.

## AI safety and cost controls

- AI calls use bounded network timeouts and no automatic retries.
- Each workspace has a persistent monthly request cap. The default is 100 requests per UTC month.
- The quota counts attempted requests, including provider failures, to prevent retry loops from bypassing limits.
- Output is capped at 1,400 tokens per request. Provider-specific monetary costs can still vary.
- Agents only prepare recommendations and drafts. They do not send messages, publish content, make purchases or move money.
