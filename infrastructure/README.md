# Deployment foundation

AURORA is packaged as a Docker Compose deployment for a small cloud VM or compatible container host.

## Local container setup

1. Install Docker Engine and the Docker Compose plugin.
2. Run ./scripts/setup.sh to create a private .env with a random 256-bit database password.
3. Temporarily set AURORA_BOOTSTRAP_ENABLED=true only while creating the first owner. For a remote host, use an SSH local port forward; do not expose the dashboard while registration is open.
4. Run ./scripts/start.sh and open the printed dashboard URL.
5. Immediately set AURORA_BOOTSTRAP_ENABLED=false and run docker compose up -d backend dashboard.
6. Public signup is enabled after the initial platform owner exists. Each new registrant receives a separate private workspace and OWNER role; platform-admin privileges are never granted through public signup. Workspace owners can invite their own members from the Members screen.

PostgreSQL stores durable application data in a named volume. The backend and database have no published host ports. The dashboard is bound to 127.0.0.1 only. Nginx rate-limits login, public registration, invitation registration and password reset. It also provides, security headers, a Content Security Policy and the internal API reverse proxy.

## Public HTTPS ingress

After provisioning a real DNS hostname pointing to the host, set both AURORA_DOMAIN and AURORA_APP_BASE_URL=https://<hostname> in .env, open inbound TCP 80/443 (and UDP 443 if HTTP/3 is enabled), and ensure first-owner bootstrap is disabled. Normal account registration remains open after initial owner creation. Run ./scripts/start-public.sh. It validates the domain, HTTPS origin, database password and closed bootstrap setting before validating Compose and starting the optional Caddy profile. Caddy terminates HTTPS and proxies to the internal dashboard service; the dashboard host port remains loopback-only.

The public start script has been syntax-checked and its safe preflight paths tested. The full Docker build, Caddy certificate issuance and public HTTPS smoke test have not yet run because Docker is not installed on the development laptop and no production domain/host has been configured.

## Operations

- ./scripts/status.sh shows container status.
- ./scripts/backup-postgres.sh creates a compressed backup and SHA-256 sidecar.
- ./scripts/restore-postgres.sh validates a backup and requires explicit destructive confirmation.
- ./scripts/stop.sh stops services but retains the database volume.
- See the production runbook for restore drills, AI usage limits and deployment gates.

## Application and AI controls

The cloud profile uses account sessions and workspace membership authorization. Flyway migrations V1–V12 manage PostgreSQL schema changes. AI execution is optional, has bounded network timeouts, a persistent monthly member request allowance (OWNER/ADMIN exempt from this application allowance), agent-specific output token budgets, provider token/cost accounting, business attribution, atomic monetary reservation hooks, unknown-cost holds, evidence-backed owner/admin reconciliation and persistent in-app budget alerts.

Monetary spending limits remain disabled until a defensible maximum per-request reservation and a suitable independent provider-side billing limit are configured. Provider invoice reconciliation remains manual. No AI provider request is made by deployment or startup.

## External services and release gates

A cloud host/account, real domain, DNS and deployment credentials are required. Configure external secret management, encrypted off-site backups, monitoring, alerts and a tested restore plan. Complete independent security, privacy and data-retention reviews. Invitation email can use Gmail SMTP with a Google App Password configured through scripts/configure-gmail.sh; real delivery has not been tested. The Research Agent is wired to an OpenRouter web-search tool, but live source/citation quality still needs end-to-end verification. Video generation and social publishing are not integrated and require external providers and explicit account authorization.

The Compose deployment has not been executed on the development laptop because Docker/Podman are not installed. Keep .env mode 600 and never commit it.
