# Deployment foundation

A Docker Compose deployment is provided for a small cloud VM or a compatible container host.

## Local container setup

1. Install Docker Engine and the Docker Compose plugin.
2. Run `./scripts/setup.sh` to create a private `.env` with a random database password.
3. Temporarily enable `AURORA_BOOTSTRAP_ENABLED=true` only while creating the first owner.
4. Run `./scripts/start.sh` and open the printed dashboard URL.
5. Immediately disable bootstrap registration and restart the backend/dashboard services.
6. Invite additional users from the Members screen.

PostgreSQL stores durable application data in a named volume. The backend and database are not directly published to the host. Nginx provides authentication rate limits, basic security headers and a Content Security Policy. HTTPS must be terminated by a separate trusted ingress before internet exposure.

## Operations

- `./scripts/status.sh` shows container status.
- `./scripts/backup-postgres.sh` creates a compressed backup and SHA-256 sidecar.
- `./scripts/stop.sh` stops services but retains the database volume.
- See [the production runbook](../documentation/operations/production-runbook.md) for restore drills, AI usage limits and deployment gates.

The cloud profile uses account sessions and workspace membership authorization. Flyway migrations V1–V4 manage PostgreSQL schema changes. AI execution is optional, has bounded network timeouts, a configurable persistent monthly per-workspace request allowance, and output token limits.

## Before public deployment

A cloud host/account and deployment credentials are required. Provision a domain, HTTPS/TLS ingress, external secret management, automated encrypted backups, monitoring and alerts. Complete independent security, privacy and data-retention reviews. Email delivery, live web research, video generation and social publishing are not configured by this repository and require external providers and explicit account authorization.

A local Compose deployment has not yet been executed on the development laptop because Docker/Podman are not installed.
