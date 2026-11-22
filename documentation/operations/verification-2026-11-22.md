# AURORA operational verification — 22 November 2026

**Scope:** Local development, local cloud-profile runner, Neon database and saved public tunnel.  
**No application/customer records were modified.** The additive Flyway V6 schema migration was applied to the Neon development database; verification queries were read-only. No AI-provider completion or email was sent.

## Service map

| Component | Address | Target / role | Verified |
|---|---|---|---|
| H2 backend | 127.0.0.1:8080 | Local development database | Readiness UP |
| H2 dashboard | 127.0.0.1:5173 | Vite; default proxy to backend 8080 | HTTP 200 locally |
| Neon-backed backend | 127.0.0.1:8081 | Spring profiles postgres,cloud; Neon PostgreSQL | Readiness UP; database pool connected |
| Neon-backed dashboard | 127.0.0.1:5174 | Vite proxy target configured as backend 8081 | HTTP 200 locally |
| Saved Cloudflare quick tunnel | Saved trycloudflare URL in local .env | Points to port 5173, not the Neon-backed dashboard | HTTP 530; tunnel logs report "Unauthorized: Tunnel not found" |

The saved public URL is **not a working public production endpoint**. Do not restart the quick tunnel blindly: a newly created quick-tunnel URL may change, and the existing tunnel points at the H2 development app. A proper deployment needs a stable HTTPS ingress routed to the intended cloud-backed service, plus the remaining release gates.

## Neon verification

- Read-only connection to database neondb succeeded.
- PostgreSQL server version: 18.7.
- Flyway schema version: 6.
- Migrations V1–V6 validated successfully at backend startup; V6 added the password-reset token table.
- Flyway reported schema version 5 before this change, then applied V6 successfully; Hibernate schema validation passed at version 6.
- Hibernate initialized against PostgreSQL successfully.
- Cloud-profile backend started on port 8081 and returned readiness UP.

## AI allowance verification

Read-only Neon query for the current period returned:

- Period start: 2026-11-01 (UTC).
- Workspace request counter: 37.
- Last update: 2026-11-22 10:46:19.617985 UTC (13:46:19 EAT).
- Counter remained 37 after the cloud-profile backend restart.
- The table stores a workspace-period aggregate, not per-user request attribution. The historical 37 cannot be split by member/admin from this table.
- The OWNER/ADMIN exemption was added to source after the counter's last update and is now loaded by the restarted cloud-profile process. Future OWNER/ADMIN requests bypass AURORA's application-level member allowance. Provider billing still applies to every AI request.
- The counter is **not** OpenRouter token usage, a monetary balance, or a provider invoice. The provider-usage ledger was added in the later follow-up section below; its V7 migration is still pending on Neon.

The cloud profile has no explicit AURORA_AI_MODEL override in its environment file; application configuration therefore defaults to openai/gpt-4.1-mini. Do not assume the openrouter/free model configured in the separate local .env applies to the Neon-backed process. Change the model only after an explicit cost/quality decision.

## Code and verification changes in this work session

- Dashboard usage wording now distinguishes the OWNER/ADMIN exemption, historical aggregate counter and provider charges.
- Regression test added: admin requests do not increase the member allowance counter.
- Password recovery added end-to-end: generic reset request response, 30-minute hashed one-time tokens, password update, session revocation, login-screen reset flow and Nginx rate-limit routes.
- Docker Compose and `.env.example` now pass Gmail SMTP host, port, sender, username and password; the previous unused OAuth variables were removed from the active container configuration.
- Backup creation now verifies the archive can be listed; restore requires a valid checksum, validates the archive and uses `--exit-on-error --single-transaction`.
- Research Agent catalog description now reflects web-search tool wiring while stating citation quality is unverified.
- Gmail documentation corrected: current sender is Spring JavaMailSender over Gmail SMTP with a Google App Password. The separate Gmail OAuth helper is not wired to the sender.
- Competitive/market research brief added at documentation/research/chama-market-validation-brief-2026-11-22.md.
- GitHub Actions now defines a container-build job for Compose validation, production image builds and Nginx syntax checks; the workflow has not yet been run on GitHub.
- README, operations runbook and release checklist updated to match verified state.

## Verification results

- Initial verification run: backend 15 test suites, 44 tests, 0 failures, 0 errors, 0 skipped. Later follow-ups expanded this to 48 tests across 17 suites (see dated sections below).
- Frontend Oxlint: 0 warnings, 0 errors.
- Frontend TypeScript/Vite production build: passed.
- git diff --check, shell-script syntax and Ruby YAML parsing for Compose/GitHub Actions: passed.
- Local readiness endpoints for both backends and both dashboards: HTTP 200; Neon-backed dashboard: HTTP 200.
- Password-reset request smoke test using a reserved `.invalid` address: HTTP 202 with the generic response; Neon contains zero password-reset token rows, confirming the smoke test did not create a token or send email.
- Cloud-profile startup: Neon connection succeeded, six migrations validated, V6 applied, schema current, Hibernate validation passed and readiness UP.
- Saved public tunnel: failed (HTTP 530 / Tunnel not found).
- Docker/Podman Compose build, GitHub Actions, real Gmail delivery, real AI-provider citation quality, automated backups and restore drill remain unverified.

## Next operational priorities

1. Keep the Neon-backed local dashboard on port 5174 for controlled testing; do not call the H2 dashboard on 5173 the production app.
2. Do not expose the local development server through a replacement quick tunnel. Provision a stable, protected HTTPS deployment target first.
3. Decide whether to keep the default model or explicitly set a different OpenRouter model; validate quality and provider price before changing it.
4. Apply V7 to the Neon development database, then verify the ledger through the cloud profile. Define a budget scope and provider-side hard limit; app-side budget reservations and alerts remain pending.
5. Implement or remove the unused Gmail OAuth helper; until then use the documented SMTP App Password path only.
6. Run the full end-to-end research workflow only when the owner approves provider usage; verify every source, date and citation before treating its output as research evidence.
7. Complete off-site encrypted backups, restore drill, external secrets, monitoring, independent security review, privacy/legal review and pilot-user validation before any public release.

**Release decision:** AURORA is a working private prototype with a Neon-backed local runner. It is not production-certified or publicly available through the saved tunnel.

## Follow-up hardening — 22 November 2026

- Local Compose `.env` now explicitly disables SMTP and uses a localhost placeholder URL; file permissions remain `0600`. Existing SMTP credentials were preserved but not printed or used.
- Gmail setup helper now requires a public HTTPS dashboard URL and rejects localhost, HTTP, embedded credentials, query strings and fragments before writing mail settings.
- Nginx now sends `Referrer-Policy: no-referrer` globally to reduce leakage of invitation and password-reset tokens carried in query strings.
- Invitation email input now has a 254-character maximum at request validation.
- Operations runbook corrected to document that the SMTP sender supports password-reset delivery as well as invitations.
- Re-ran backend suite: 44 tests, 0 failures, 0 errors, 0 skipped. Frontend production build and Oxlint both passed. URL guard test cases: 6/6 passed. Shell syntax and `git diff --check` passed.
- No email or AI-provider call was made. No database records were changed by this follow-up.

## Security audit follow-up — 22 November 2026
- Corrected the OpenRouter endpoint composition: the base URL is `https://openrouter.ai/api/v1` and the client appends only `/chat/completions`. The previous combination duplicated `/api/v1`; URL resolution was reproduced locally with Spring’s `DefaultUriBuilderFactory`. No provider request was made.
- Changed the Compose dashboard port binding to `127.0.0.1` only. This prevents accidental direct public HTTP exposure; public access must pass through a separately configured trusted HTTPS ingress. Compose image execution remains unverified because Docker/Podman are unavailable on this laptop.

- Reviewed the session-authentication filter, legacy owner-token filter, auth endpoints, password-reset flow, workspace membership controls, production properties, Nginx edge configuration and frontend token storage.
- Confirmed browser sessions are held in `sessionStorage` (not persistent `localStorage`), API calls use bearer authorization and the workspace header, the dashboard CSP restricts connections to the same origin, and Nginx denies public access to Actuator routes. HSTS remains a deployment task because the current Nginx configuration is HTTP-only and no stable HTTPS ingress exists yet.
- Hardened password-reset token consumption: the token lookup now obtains a pessimistic database write lock and eagerly loads its user within the transaction. Concurrent requests attempting to consume the same reset token are serialized, preventing a successful one-time token from being consumed twice.
- Full backend Maven suite after the change: 45 tests across 16 suites, 0 failures, 0 errors, 0 skipped; process exit code 0.
- No production database records were changed, no email was sent and no AI-provider request was made.


## Provider usage ledger — 22 November 2026

- Added additive Flyway migration V7 for `ai_provider_usage`, linked one-to-one with an agent run and scoped to its workspace. It stores provider/model, request state, prompt/completion/total tokens, provider request ID, reported USD cost and timestamp.
- Agent execution now creates a `PENDING` ledger entry before the provider call, records returned usage metadata when available, and marks attempts `UNKNOWN` when no response usage can be confirmed. Missing cost is represented as unknown/null, never as free usage.
- Extended the existing `/api/v1/ai-usage` response without removing the existing request-allowance fields. The dashboard now shows monthly provider calls, reported tokens, reported USD cost and calls without a reported cost.
- Added three ledger tests covering reported token/cost capture, missing usage metadata and failed/unknown attempts.
- Full backend Maven suite: 48 tests across 17 suites, 0 failures, 0 errors, 0 skipped; process exit code 0.
- Frontend lint and production build passed; `git diff --check` passed.
- Flyway V7 was subsequently applied successfully to the Neon development database. A fresh cloud-profile backend startup validated all seven migrations, confirmed schema version 7, passed Hibernate schema validation and reached readiness UP on port 8081. The backend was restarted with the new code; dashboard port 5174 remains available. No provider request, email, payment, or customer/business data change was made. The ledger records provider-reported costs; it is not yet reconciled against invoices or provider account billing exports.
- At this earlier V7 checkpoint, hard spend caps were not implemented. See the later V9 section for the application-side reservation guard; provider-side limits remain outstanding.


## Business-level AI attribution — 22 November 2026

- Added Flyway V8, linking new agent runs optionally to a business workspace. Existing records remain unassigned; no historical attribution is guessed.
- Agent-run API validates that a selected business belongs to the active workspace. Cross-workspace business IDs are rejected.
- Agent dashboard now offers a business selector for manual runs and the four-stage Chama validation workflow, and shows business association in execution history.
- Monthly AI usage response now includes per-business request, token and provider-reported cost summaries, plus a separate workspace-level bucket for unassigned work.
- Added tests for business association, cross-workspace rejection, and business-level ledger aggregation. Full backend suite: 50 tests across 17 suites, 0 failures/errors/skipped; a subsequent package compile also passed.
- V8 successfully applied to Neon development. Fresh cloud-profile startup validated all eight migrations, confirmed schema version 8, passed Hibernate schema validation and reached readiness UP on port 8081.
- At the time of this V8 checkpoint, business-level monetary budgets and platform hard caps were not yet enforced. The subsequent V9 section below records the reservation guard implementation and its remaining provider-side controls.
- No provider request, email, payment, or customer/business content was created or changed during this work.


## Monetary AI budget guard — 22 November 2026

- Added Flyway V9 for a singleton database lock, per-business monthly USD policies and durable per-run budget reservations.
- Agent execution now commits a maximum-cost reservation before provider dispatch whenever a platform or business monetary ceiling is enabled. The singleton row is pessimistically locked, so concurrent requests across application instances cannot independently consume the same remaining budget.
- Added deployment-controlled platform monthly ceiling (`AURORA_AI_PLATFORM_MONTHLY_BUDGET_USD`) and a per-request reservation maximum (`AURORA_AI_MAX_REQUEST_RESERVATION_USD`). The platform ceiling is intentionally not editable by workspace owners/admins; there is no global platform-owner role in the current identity model.
- Added owner/admin API and Business portfolio controls to set or remove monthly USD ceilings for individual businesses. The dashboard shows reported spend, held reservations, remaining amount and 80%/90%/100% warning levels.
- Provider-reported cost settles the reservation. If cost is absent or malformed, the reservation remains held, including across month boundaries; unknown cost is never silently released. Actual provider cost above the reservation is reflected in the usage ledger and blocks future calls, but cannot undo the charge from that already completed call.
- Added tests for reservation-before-dispatch accounting, business ceiling enforcement, settlement from reported cost, conservative handling of unknown cost, and simultaneous requests competing for the same remaining business budget. Latest full backend suite: 53 tests across 18 suites, 0 failures, 0 errors, 0 skipped, including a simultaneous-reservation concurrency regression. Frontend Oxlint and TypeScript/Vite production build passed; `git diff --check` passed.
- V9 successfully applied to Neon development. Fresh cloud-profile startup validated all nine migrations, applied V9, passed Hibernate schema validation and reached readiness UP on port 8081. Protected usage and budget endpoints return HTTP 401 without a session, as expected.
- Monetary limits remain disabled by default (`0`); the current Neon development environment has not been assigned a per-request reservation amount, so business budget controls remain intentionally disabled there. Enabling either platform or business budgets requires a defensible per-request maximum. That maximum is an operator-supplied bound, not a provider guarantee; independently configure provider-side billing hard limits. A single request can still exceed the reservation if the chosen bound is too low.
- Budget alerts are currently dashboard threshold warnings only. Email/push alert delivery, platform-owner UI, provider invoice reconciliation and provider-side hard billing limits remain outstanding. The manual reconciliation workflow is documented in the subsequent V10 section. Held reservations must not be released without checking provider billing records.
- No AI-provider request, email, payment, or customer/business content change was made.

## Unknown-cost reconciliation workflow — 22 November 2026

- Added Flyway V10 to record settlement source, provider billing reference, evidence note and reconciling workspace role for manual settlement.
- Added owner/admin-only workspace-scoped unresolved-reservation queue (oldest first, up to 100) and a manual reconciliation endpoint. A reservation can only be reconciled once; cross-workspace IDs return not found, duplicate settlement returns conflict, and the request requires a non-negative actual USD cost, billing reference and evidence note.
- Added an Unresolved AI Charges panel in the Business portfolio. It shows the provider request ID when available, and explicitly instructs the operator to verify the provider invoice/request record and never enter estimates. Reconciliation is not an automated provider query and does not contact the provider.
- Active requests are excluded from the reconciliation queue. A request must have finished or remained RUNNING for at least one hour before manual reconciliation is permitted. If a late provider response arrives after a stale-run manual reconciliation, the provider-reported cost overrides the manual amount while the manual evidence remains in the audit fields. When a matching usage-ledger row exists, its missing cost is updated as `MANUALLY_RECONCILED`, so the dashboard no longer reports that call as cost-unknown.
- Fixed budget accounting so settled reservations are the source of truth for calls that were reserved, while provider ledger costs are included only for runs without reservations. This prevents double counting provider-reported costs and ensures manually reconciled costs continue counting against the monthly budget. Settled reservation cost is attributed to the month the request was created, not the later reconciliation date.
- Removing a business budget is blocked while that business has held reservations; the operator must reconcile them first.
- Targeted budget-guard tests passed (4 tests), including manual reconciliation, duplicate rejection and usage-ledger cost accounting. Final backend suite: 55 tests across 18 suites, 0 failures, 0 errors, 0 skipped. Targeted budget guard suite also passed after the final queue query and provider-request-ID changes. Frontend lint/build and `git diff --check` passed. Neon development applied V10 successfully; Flyway validated all 10 migrations, Hibernate schema validation passed, and the cloud-profile app reached readiness UP on port 8081. Health and dashboard returned HTTP 200; protected usage and reconciliation endpoints returned HTTP 401 without a session, as expected.


## Persistent budget alerts and deployment preparation — 22 November 2026

- Added Flyway V11, creating workspace-scoped business AI budget alert records with unique business/month/threshold keys, observed USD amount, configured monthly limit, creation time and acknowledgement time.
- Budget evaluation now records 80% WARNING, 90% CRITICAL and 100% EXHAUSTED alerts after budget configuration, before a denied request when current usage has already crossed a threshold, after a reservation, after provider settlement and after manual reconciliation. A shared database lock serializes creation and the database unique key prevents duplicate alerts per business, UTC month and threshold.
- Added owner/admin-only alert listing and acknowledgement endpoints. Acknowledgement is transactional, idempotent and audited; cross-workspace IDs return not found. Dashboard includes persistent alert history and acknowledgement controls. No email, push notification or provider request is triggered.
- Added regression coverage for threshold creation, per-period deduplication, acknowledgement persistence, member access denial and cross-workspace denial.
- V11 successfully applied to the Neon development database. Flyway validated all 11 migrations, applied V11 and Hibernate schema validation passed. The cloud-profile backend restarted on port 8081 and reached readiness UP. Dashboard ports 5173 and 5174 returned HTTP 200; protected alert and usage endpoints returned HTTP 401 without a session, as expected.
- Final backend suite before public signup: 57 tests across 18 suites, zero failures/errors/skipped. This includes persistent alert thresholds, acknowledgement authorization, cross-workspace denial and bounded shared-context tests. Frontend lint and production build passed. Compose YAML parses successfully and git diff --check passes.
- Added an optional Caddy Compose profile for public HTTPS ingress. It proxies to the internal dashboard service, while the dashboard's host port remains loopback-only. Nginx now trusts forwarded client addresses only from the private Docker bridge address range so authentication rate limits can distinguish client IPs behind Caddy, and preserves the original HTTPS scheme when proxying to Spring Boot.
- Added a guarded public-start script that refuses the example domain, mismatched/non-HTTPS application base URL, weak database password or enabled first-owner bootstrap. It never sources .env, explicitly passes the validated hostname into Compose to avoid shell-environment overrides, runs Compose validation/build and waits for HTTPS. Shell syntax and mocked preflight tests passed; actual container execution is blocked locally because Docker is unavailable.
- Added CI validation for the Caddyfile using the official Caddy container, alongside Nginx configuration validation, and included mocked public-start preflight tests in the backend job. CI has not yet been run on GitHub.
- Public-start preflight is now covered by scripts/test-public-preflight.sh and the GitHub Actions backend job. Mocked Docker/curl tests verify the validated .env hostname overrides conflicting shell variables, bootstrap-enabled startup is refused before Docker is invoked, and a shell-expression hostname is rejected without sourcing .env. All preflight tests passed locally.
- Bounded shared-memory and customer-research context to 7,000 characters each before AI dispatch, preserving newest records and marking omitted older context. This prevents unbounded retrieval context from inflating prompts; it is not a tokenizer-level token cap or a substitute for a provider-side spend limit.
- Docker/Podman are not installed on this laptop. Therefore image builds, Nginx container validation, Caddy certificate issuance and full Compose startup have not been executed. A real domain, DNS records, firewall access and external host are still required.
- Monetary budgets remain disabled in the development environment. No defensible maximum per-request reservation or independent provider-side hard billing limit has been configured. No AI-provider request, email, payment or public deployment was performed.


## Public signup update (2026-11-22)

- Public signup now remains enabled after the first platform owner is created, even with `AURORA_BOOTSTRAP_ENABLED=false`.
- Each new account receives a newly created private workspace and OWNER membership. Only the first account on a fresh installation receives platform-admin privileges.
- Concurrent signup tests verify successful independent registrations and distinct workspace assignments.
- Latest backend suite: 60 tests across 19 suites, zero failures/errors/skipped. Frontend lint and production build passed.
- Local H2 and Neon-backed backend processes were restarted with the latest JAR. Both health endpoints return HTTP 200 and both `/api/v1/auth/bootstrap` endpoints return `registrationOpen=true`.
- No public tunnel is running; public internet exposure remains pending a production reverse proxy and staging/security checks.


## Follow-up verification and AI dispatch consolidation — 22 November 2026

- Re-ran the complete backend suite after the latest changes: 62 tests across 19 suites, 0 failures, 0 errors, 0 skipped.
- Added regression tests proving the legacy `/api/v1/agents/research/run` endpoint delegates to the shared `AgentExecutionService`, and does not return a report when the shared execution fails.
- Removed the legacy controller's direct provider call. A source scan now finds exactly one `aiProvider.complete(...)` dispatch in production code, inside `AgentExecutionService`, which applies the member allowance, provider usage ledger and configured monetary budget guard before dispatch.
- Frontend Oxlint passed with 0 warnings and 0 errors. TypeScript and Vite production build passed. Both local dashboards on ports 5173 and 5174 returned HTTP 200.
- Rebuilt the backend JAR and restarted only the Neon-backed local development process on port 8081, preserving its existing `postgres,cloud` profile and environment. It validated all 12 Flyway migrations, confirmed schema version 12 was current, passed Hibernate schema validation, and reached readiness UP. The H2 backend on port 8080 was not restarted or modified.
- Both backends continue to report `registrationOpen=true`. No production deployment, public tunnel, email delivery or AI-provider request was performed.
- The legacy research endpoint now shares the same accounting and budget enforcement as all other AI agent execution paths. Live research citation quality still requires an explicitly approved provider run and manual source verification.
