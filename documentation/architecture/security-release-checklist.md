# AURORA release readiness checklist

**Current status: private release candidate. Do not expose publicly until the external production gates below are completed.**

## Identity, access and application security

- [x] Password hashing, seven-day expiring sessions, session revocation and one-time password recovery tokens
- [x] OWNER, ADMIN, MEMBER and VIEWER workspace roles
- [x] One-time invitations and first-owner registration guard
- [x] Public signup creates a private workspace and OWNER membership without platform-admin privilege
- [x] Server-side authentication and workspace membership checks
- [x] Workspace-scoped repository isolation and cross-workspace integration tests
- [x] Login, registration, invitation and password-reset rate limits at the Nginx edge
- [x] DTO validation, request-size limits, security headers and Content Security Policy
- [ ] Independent security review and multi-account browser testing
- [ ] Review bearer-token storage in the browser and production cookie/session policy
- [ ] Dependency and container-image vulnerability scanning
- [ ] Review API error handling and privileged audit actor attribution

## Data and operations

- [x] PostgreSQL Flyway migrations V1–V12
- [x] Hibernate schema validation in the PostgreSQL cloud profile
- [x] PostgreSQL backup/restore scripts with SHA-256 verification and archive validation
- [ ] Encrypted off-site backup automation and retention
- [ ] Restore drill against a separate database, recorded and repeated
- [ ] H2-to-PostgreSQL export/import and identity/workspace reconciliation for existing local data
- [ ] Data export, account/workspace deletion and data-retention policies

## AI governance and financial controls

- [x] Agents are advisory; publishing, customer contact, spending and external actions are not autonomously executed
- [x] Human approval records for consequential proposed actions
- [x] Persistent monthly member request allowance; OWNER/ADMIN exemption is an application quota exemption, not a billing exemption
- [x] Provider usage ledger, provider-reported token/cost accounting and business attribution
- [x] Atomic application reservation hooks, unknown-cost holds and evidence-backed owner/admin reconciliation
- [x] Persistent business budget alerts at 80%, 90% and 100%, with acknowledgement
- [x] Prompt-injection boundary instructions, untrusted-data handling, markup escaping and bounded retrieved context
- [ ] Configure a defensible maximum per-request reservation and activate appropriate non-zero platform/business budgets
- [ ] Configure and verify independent provider-side hard billing limits; do not rely on AURORA's application guard alone
- [ ] Reconcile provider ledger against billing exports/invoices
- [ ] Implement secure platform-owner controls for platform-wide spending limits
- [ ] End-to-end verify Research Agent sources, publication dates, claim labels and citations with a deliberately approved provider test
- [ ] Video generation and social publishing integrations, each behind explicit owner approval

## External services

- [x] Optional Gmail SMTP invitation and password-reset code paths
- [ ] Configure and verify real SMTP delivery to controlled mailboxes
- [ ] External secrets management and credential rotation
- [ ] Real domain, DNS and trusted HTTPS certificate issuance

## Build, deployment and reliability

- [x] Backend automated test suite
- [x] Frontend lint and TypeScript/Vite production build
- [x] GitHub Actions workflow defined for backend tests, PostgreSQL startup, frontend checks, Compose validation, production image builds, Nginx and Caddy validation
- [x] Guarded public-start preflight rejects example domains, mismatched/non-HTTPS origins, weak database passwords and open first-owner bootstrap
- [ ] Run GitHub Actions on GitHub and protect the main branch
- [ ] Run Docker Compose production image build and end-to-end smoke test on a Docker host
- [ ] Deploy to a production host and verify DNS, HTTPS, health checks and restart behavior
- [ ] Production metrics, structured logs, alerting, incident response and rollback drill
- [ ] Pilot-user support and feedback process
- [ ] Privacy, data-retention and legal review before real customer data is stored

## Verified locally on 2026-11-22

- Backend: 60 tests across 19 suites; zero failures, errors or skipped tests.
- Backend executable JAR packaged successfully.
- Frontend: Oxlint passed with zero warnings/errors; TypeScript and Vite production build passed.
- Neon PostgreSQL 18 development database: Flyway V1–V12 validated and applied; Hibernate schema validation passed; cloud-profile backend started and health returned HTTP 200.
- Protected AI usage and budget alert endpoints returned HTTP 401 without authentication, as expected.
- Dashboard dev servers returned HTTP 200.
- Compose and CI YAML parsed; deployment shell scripts passed syntax checks; git diff --check passed.
- Public-start preflight accepted a valid test hostname and stopped at the expected Docker prerequisite; it rejected first-owner bootstrap being enabled.
- Docker/Podman are not installed on the development laptop. Container image builds, Nginx/Caddy runtime validation, public certificate issuance and hosted deployment remain unverified.

AURORA is a private release candidate, not a production-certified or publicly hosted service. A successful local build does not replace the outstanding security, billing, infrastructure and legal checks.
