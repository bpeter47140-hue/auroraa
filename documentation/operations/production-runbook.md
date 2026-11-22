# AURORA v0.5 operations runbook

## Local container setup

1. Install Docker Engine and the Docker Compose plugin.
2. Run ./scripts/setup.sh. It creates a private .env with a random database password and leaves first-owner registration disabled.
3. Temporarily change AURORA_BOOTSTRAP_ENABLED=false to true in .env for the first registration only.
4. If this is a remote server, create an SSH local port forward (for example, ssh -L 8088:127.0.0.1:8088 operator@your-server). Never expose the dashboard publicly until the first platform-owner account is created and bootstrap is disabled.
5. Run ./scripts/start.sh and open the printed dashboard URL through the SSH-forwarded local port or on the server itself.
6. Create the owner account. Immediately change AURORA_BOOTSTRAP_ENABLED=true back to false.
7. Run docker compose up -d backend dashboard to apply the closed bootstrap setting.
8. Public signup is then available: each new registrant receives a separate private workspace and OWNER role, without platform-admin privileges. Workspace owners can invite additional users from the Members screen. With Gmail SMTP configured using a Google App Password, AURORA sends a seven-day, single-use invitation link; otherwise the dashboard offers a manual share link.
9. Configure an AI provider key only if model execution is wanted. AURORA_AI_MONTHLY_REQUEST_LIMIT defaults to 100 member requests per workspace per UTC month. OWNER and ADMIN requests are exempt from this application allowance; provider charges still apply. A four-stage workflow makes four provider calls, but only member calls count against the member allowance.

## Public HTTPS deployment with Caddy

The optional `public` Compose profile runs Caddy as the public TLS ingress and obtains/renews certificates automatically. Before enabling it:
1. Point the domain's A/AAAA records to the server and allow inbound TCP 80/443 (and UDP 443 for HTTP/3).
2. Set `AURORA_DOMAIN` to the real hostname in the mode-600 `.env`.
3. Set `AURORA_APP_BASE_URL=https://your-real-domain` and keep `AURORA_MAIL_ENABLED=false` until SMTP is separately verified.
4. Run ./scripts/start-public.sh. It checks the domain, HTTPS base URL, database password and closed bootstrap setting, validates Compose configuration, builds/starts the stack and waits for a valid HTTPS response. Confirm Caddy obtains a trusted certificate.
5. The dashboard remains bound to host loopback; do not add a public port mapping to the dashboard or backend.

Do not use the example hostname. A real domain, DNS, reachable server and firewall are prerequisites. Public deployment has not been performed merely by enabling this profile.

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
2. Verify the required backup sidecar with `sha256sum -c backup.dump.sha256`; the restore script refuses archives without a matching sidecar and validates the archive before prompting.
3. Restore into the isolated database, not the live database. Restore uses `--exit-on-error` and `--single-transaction` so a failed restore does not leave a partially applied archive.
4. Start a matching AURORA release against that database.
5. Verify health, login, workspace membership, tasks, approvals, memories and audit history.
6. Record the date, backup identifier, migration version and restore outcome.

The provided restore script targets the configured Compose database and is destructive after confirmation. Do not use it for routine restore drills against production.

## Email invitations

The current invitation sender uses Spring JavaMailSender over Gmail SMTP with a Google App Password. Enable 2-Step Verification on the sending Google account, create an App Password, then run `./scripts/configure-gmail.sh`. The script saves `AURORA_MAIL_ENABLED`, `AURORA_MAIL_FROM`, `AURORA_MAIL_USERNAME`, `AURORA_MAIL_PASSWORD` and `AURORA_APP_BASE_URL` to the mode-600 `.env` file. The setup helper now requires a public HTTPS dashboard URL and rejects localhost or insecure HTTP. Keep mail disabled until that URL is stable. Test delivery to a controlled mailbox and verify the invitation link and expiry. Never commit or share `.env` or the App Password. The separate `configure-gmail-oauth.sh` helper is not wired to the current sender and must not be treated as a working OAuth integration. The same SMTP sender also supports password-reset email. Reset tokens are hashed, expire after 30 minutes, are single-use, and revoke active sessions when used. If delivery is disabled or fails, the invitation remains pending and can be shared manually.

## First-owner recovery

If no owner exists and bootstrap has been disabled, stop the backend, take a verified database backup, and follow a reviewed administrative recovery procedure. Do not expose the first-owner bootstrap endpoint publicly. Normal customer signup is intentionally available once the platform owner exists; the first-owner bootstrap switch must remain disabled.

## Public deployment gates

The Compose dashboard port binds to `127.0.0.1` only. Before public internet exposure, provision a domain and a trusted TLS ingress on the host that proxies to the loopback port, plus external secret management, monitored backups, alerts and a tested restore plan. Review rate limits, account recovery, session storage, privacy and data retention. Review the current deployment status in `README.md`; a temporary tunnel is not production hosting. Public signup must be protected by the documented authentication, tenant-isolation, backup and monitoring controls. This repository does not provision cloud infrastructure, generate marketing videos, or publish to social networks. Workspace invitation email is available when Gmail SMTP is configured with a Google App Password.

## Provider-side hard spending limit

For the OpenRouter provider, use a dedicated API key for each AURORA environment and set a key-level credit limit with an appropriate reset interval in the OpenRouter key settings. The current OpenRouter pricing comparison lists Budgets & Spend Controls on Standard/Business/Enterprise, not Free; if the account is on Free, do not assume a provider-side hard spend cap is available. Keep auto top-up disabled unless an owner has explicitly approved it. OpenRouter documents per-key credit caps and organization/workspace budgets; these are independent of AURORA application reservation guard. Confirm the configured key cap in the provider dashboard before enabling AURORA monetary budgets. Do not share a personal key across development and production. See the official [OpenRouter limits documentation](https://github.com/OpenRouterTeam/docs/blob/main/api_reference/limits.mdx) and [pricing/plan comparison](https://openrouter.ai/pricing).

## AI safety and cost controls

- AI calls use bounded network timeouts and no automatic retries.
- Each workspace has a persistent monthly member request allowance. The default is 100 member requests per UTC month.
- OWNER and ADMIN requests bypass the application allowance; provider charges still apply to every AI request.
- For members, the quota counts attempted requests, including provider failures, to prevent retry loops from bypassing limits.
- Research requests allow up to 4,200 output tokens; other agents allow up to 1,800. These are maximum completion budgets, not guaranteed output sizes. Provider-specific monetary costs can still vary.
- Agents only prepare recommendations and drafts. They do not send messages, publish content, make purchases or move money.
