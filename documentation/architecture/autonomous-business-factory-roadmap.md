# Aurora Autonomous Business Factory — Implementation Roadmap

## Product goal
Aurora is a multi-workspace autonomous business factory. It should research real opportunities, validate evidence, plan businesses, create software products, create brand and marketing assets, launch approved campaigns, measure results and improve. Multiple ventures must remain isolated within a workspace.

## Current verified foundation (2026-11-25)
- Java 25/Spring Boot API, React/TypeScript dashboard, PostgreSQL cloud profile (Neon) and H2 local development.
- Authentication, workspace memberships/roles, public registration, invitations, audit log, approvals, tasks, business workspaces, shared memory and customer research.
- Seven specialist agents are registered: Research, Strategy, Content, Operations, Product Developer, QA and Security. Project workflows persist stage status/output and run asynchronously; pause/resume/cancel checkpoints avoid replaying completed stages.
- Research Agent is wired to a provider web-search tool, but source/citation reliability still needs an approved end-to-end test.
- Agent execution shares usage allowance, provider usage ledger and budget guard.
- A restricted React/TypeScript project builder and disposable network-isolated preview builder are implemented. The runner still uses the Docker socket and is not production-hardened. Image/video generation, social publishing and analytics integrations are not implemented.
- Current app is a private prototype. Public endpoint, hosting, credentials, safety and release gates are not complete.

## Target agent roles
1. Research & Market Intelligence: current sources, competitor evidence, customer/problem research, source quality and opportunity assessment.
2. Strategy & Venture Manager: business case, roadmap, experiments, KPIs, stop/pivot/build gates.
3. Product Manager: product brief, user stories, acceptance criteria, backlog and scope.
4. Brand & Design: naming, identity, visual tokens, logo/creative briefs, website/app UI, campaign designs.
5. Software Engineer(s): frontend, backend, database, integration and code generation/editing in isolated project workspaces.
6. QA & Security: test plans, test execution, bug reports, dependency/security checks, accessibility and acceptance gates.
7. Marketing & Creative: channel strategy, campaign copy, scripts, graphics, short-form videos, demos and content variants.
8. Growth & Analytics: campaign experiments, funnel analytics, performance reports and recommendations.
9. Finance: cost model, budgets, financial reports and alerts; no payment movement.
10. Operations & Customer Success: tasks, documentation, customer feedback synthesis and service processes.

## Milestone plan (deliver in order)
### M1 — Durable Venture Projects & Workflow Engine
- Venture/project records and strict workspace isolation.
- Project lifecycle and stages.
- Durable workflow/job records, steps, inputs, outputs, status, timestamps, retries, checkpoints, cancel/resume and execution logs.
- Multi-agent handoffs and dependency graph; avoid single long synchronous HTTP requests.
- Run cost/usage attribution per project, agent and step.
- Human approval records at policy gates.
- Dashboard for projects, workflow progress, logs, deliverables and approvals.

### M2 — Project Artifacts & Development Workspace
- Artifact registry for research reports, briefs, design files, source code, assets, test reports and releases.
- Each project gets an isolated, resource-limited workspace/container; no host filesystem access.
- Explicit allowlisted tools, time/resource limits, network restrictions, secret isolation, safe file export and audit trails.
- Engineer agent can create/edit project files, run commands/tests, inspect errors, iterate and produce a preview.
- Security agent runs checks and QA agent verifies acceptance criteria before preview.
- Build artifacts and previews are versioned; no production deploy without approval.

### M3 — Research & Product Planning Pipeline
- Multi-step research with source capture, URL, publisher, title, published/update date, retrieved date, claims, evidence type, confidence and freshness.
- Independent evidence audit and validation gate; primary research must not be fabricated.
- Product brief, target users, problem, MVP scope, user stories, acceptance tests and technical plan.

### M4 — Website/App Creation & Preview Deployment
- Frontend/backend/database generation inside isolated workspace.
- Automated build, test, lint, accessibility and security scans.
- Repair loop with maximum attempts/cost and stop conditions.
- Preview environment, review link, change history, rollback.
- Production deploy requires explicit owner approval and domain/provider permissions.

### M5 — Brand & Creative Production
- Integrate image/design generation provider(s) and store outputs as artifacts.
- Brand brief, logo explorations, color/type systems, UI assets, posters, social cards, thumbnails and ad variants.
- Video workflow: script/storyboard -> scene/asset generation or stock selection -> voice/audio -> edit/assemble/captions -> render -> quality review.
- Track licensing/rights, source assets, brand consistency, output dimensions, formats and generation costs.
- Video provider capability may be asynchronous; job polling and failure/retry handling required.

### M6 — Marketing Distribution & Approval
- Channel-specific plans, content calendar, copy, creatives and UTM links.
- OAuth/connectors for selected social channels; credentials encrypted and scope-limited.
- Draft/scheduled queue, preview, approval, publish, retry, post URL and audit history.
- Initial policy: all public publishing requires owner approval; later allow narrowly configured campaigns/limits after explicit opt-in.
- No unsolicited spam, fabricated endorsements, fake reviews or misleading claims.

### M7 — Growth Analytics & Continuous Improvement
- Integrate permitted analytics/metrics from site and authorized channels.
- Measure visits, signups, conversion, retention, campaign performance and cost.
- Agents generate hypotheses and proposed experiments; tests require policy approval.
- Track outcomes against goals; recommend iterate/pivot/stop with evidence.

## Autonomy & approval policy
Autonomous by default for internal research, planning, drafting, code in sandbox, tests, local previews and reversible internal changes.
Approval required for public launch/publishing, paid ads, spend/subscriptions, domain purchase/changes, customer contact, collecting personal data, financial/legal commitments, destructive actions, production data changes and sharing private information.
Approval is scoped, auditable, expiring where appropriate, and should not imply a tool can bypass its own safeguards.

## Architecture principles
- Spring Boot remains the trusted control plane and owns auth, workspace scoping, approvals, audit, budgets, workflow state and integrations.
- Agents cannot access raw credentials, production database write access or unrestricted host shell.
- Use isolated worker/sandbox execution for coding and asset jobs; workers authenticate to internal APIs and receive scoped capabilities only.
- Use durable queue/orchestrator with resumable state, idempotent steps and bounded retries.
- Model-provider, search, design/image, video/audio, hosting, social publishing and analytics are pluggable providers; credentials are server-side.
- Every action records project/workspace, agent, run/step, requested capability, policy result, cost and artifact references.
- Multi-tenant tests and adversarial prompt-injection tests are mandatory before external actions.

## Remaining M1 work
Finish explicit retries with spend-safe idempotency, workflow approval gates, dependency-aware execution and lease-based multi-instance recovery before describing the workflow engine as fully durable or multi-instance-ready. Atomic queued-run claiming and step-level usage attribution are implemented. Continue to add functional agent capabilities, not decorative role cards.

## M1 implementation progress (2026-11-25)
- Added workspace-scoped VentureProject CRUD/status API and audit events.
- Added durable project workflow run and step tables; outputs/errors and timestamps are persisted per stage.
- Added asynchronous, bounded worker pool and two initial planning templates: website/app planning and marketing planning. Both currently use existing Research, Strategy, Content and Operations agents only.
- Added project dashboard page to create projects, launch a workflow, poll progress, read agent outputs and request cancellation.
- Added cancellation between agent stages, queued-work recovery on application startup and safe handling of interrupted RUNNING stages (not automatically retried, to avoid duplicate provider charges).
- Additive PostgreSQL migrations V13–V15 were applied successfully to an isolated temporary database using all migrations V1–V15; temporary database was dropped.
- Frontend production build and lint passed. The new workspace-scoped project/workflow integration tests pass, including an asynchronous failure-path test with AI disabled (no provider request or spend).
- M1 remains incomplete: explicit retry with spend-safe idempotency, dependency-aware execution, lease-based multi-instance recovery and workflow-specific approval gates remain. Per-step usage attribution and atomic queued-run claiming are now implemented. Planning templates still do not build or deploy apps.

## Current next steps
1. Add browser-driven functional tests and accessibility automation for generated websites.
2. Add an artifact diff UI and link QA/security findings to exact immutable source lines.
3. Replace preview-runner Docker socket access with a dedicated rootless build daemon or restricted Docker API broker.
4. Add explicit workflow approval gates, spend-safe retries, dependency-aware execution and lease-based multi-instance recovery.
5. Add staging deployment and a separate explicit launch approval; never publish automatically after a preview build.
6. Add design/image and video generation providers with stored assets and review.
7. Add publishing connectors, scheduling, approvals and analytics only after the safety gates are in place.

## M2 implementation progress (2026-11-25)
- Added V15 project artifact registry migration and workspace-scoped artifact metadata.
- Added persistent generated workspace files in a workspace/project-specific storage directory; Docker Compose mounts a dedicated named volume and Dockerfile sets the app user permissions.
- Added React + TypeScript/Vite starter scaffolding, source-file preview in dashboard, artifact list, and ZIP export.
- Added developer-agent catalog profile and restricted source-generation endpoint. It accepts only source files in the frontend allowlist, validates JSON/path/size limits, records artifacts and AI run usage. Generated code is never executed on the Aurora backend or host; it is built only in the isolated preview container and is not deployed.
- Existing agent workflow stage outputs are saved as Markdown project artifacts in the workspace.
- The generated starter was tested with npm install and production build: PASS (Vite build).
- Integration tests verify project isolation, workflow failure behavior with no AI spend, starter scaffold creation, file listing and ZIP export.
- At this earlier M2 checkpoint, file editing UI, build/test execution sandbox and live preview hosting were still outstanding. Later M2 sections below record the review system and isolated preview implementation. Do not run AI-generated source on the backend/host.

- Final backend regression run: 65 tests passed, zero failures/errors. Frontend production build and lint pass; scaffold project also ran npm install and `npm run build` successfully. The code-generation endpoint itself is not live-tested against a provider yet (no provider request was made during validation).


## M2 implementation progress — review & approval (2026-11-25)
- Added additive V16 `project_reviews` migration and workspace-scoped review API for artifact versions.
- Review requests record artifact path, name, requester, SHA-256 content hash, status and timestamps. A duplicate pending request for the exact same artifact version is rejected.
- Only workspace OWNER/ADMIN may decide reviews. APPROVED, CHANGES_REQUESTED and REJECTED are supported; feedback is mandatory for changes/rejection.
- Decisions re-hash the current file and reject stale review requests if the artifact changed after submission. Review actions are written to the audit log.
- Project dashboard now exposes source/document contents for inspection, review requests, pending decisions, feedback and review history. Review approval does not deploy or publish.
- Frontend production build and lint pass; backend compile passes. Review API integration test verifies pending request creation, mandatory feedback for change requests, owner decision identity and persisted review history. Full backend regression suite: 66 tests passed, zero failures/errors. Validated the full PostgreSQL migration chain V1–V16 against an isolated temporary database on the local PostgreSQL 17 container; confirmed `project_reviews` was created and dropped the temporary database. No existing Aurora database was migrated or modified during this check.
- At the time of this review-system checkpoint, immutable artifact snapshots/content retrieval for older versions, visual live preview, editor, isolated build/test sandbox, QA/security gates, and deployment workflow were still outstanding. The live preview sandbox is now implemented in the later M2 section; immutable snapshots/editor/QA/deployment remain open.


## M2 implementation progress — isolated live preview (2026-11-25)
- Added a separate preview-runner service and prebuilt Node builder image. The backend sends only workspace/project identifiers to the runner; source is copied into a fresh, disposable build container.
- Build container has no network, no project-volume mount and no secrets. The build process runs as UID 10001 with dropped Linux capabilities and `no-new-privileges`; its disposable writable layer is capped at 128 MiB and the container is removed after each run. It has 512 MiB memory, 1 CPU, 128 process limit, a five-minute execution limit and the runner allows one build at a time. Only the fixed Vite/TypeScript build scripts and trusted builder Vite config are accepted. Build input/output have file-count and size caps; build time is bounded.
- Preview output is stored in the existing project workspace volume. The backend serves it only through short-lived HMAC-signed preview paths (30 minutes), after checking the workspace/project claims and preventing traversal/symlink access.
- Preview responses use restrictive CSP, `connect-src 'none'`, no-referrer and a sandboxed iframe without `allow-same-origin`. Dashboard now has Build & Preview, build logs, desktop/mobile widths and preview-link refresh.
- Builder uses a preinstalled, fixed React/Vite/TypeScript dependency set; it does not run `npm install` on generated project files and the build container has no network. Additional dependencies are intentionally not yet allowed.
- Operational security note: the trusted preview-runner service requires access to the Docker Engine socket, which is effectively host-admin access. It is not exposed publicly and requires a random internal bearer token, but production hardening should move it to a dedicated rootless/isolated Docker daemon or restricted socket proxy before hosting untrusted users at scale.
- Frontend build/lint and backend container image build pass. The preview runner container is running locally; the end-to-end smoke test exposed Docker archive/temporary-filesystem behavior, so the runner implementation was being adjusted and retested at that point. The final verification below confirms the corrected build path. No deployment or public launch was performed.

- End-to-end runner smoke tests now pass for both a minimal Vite site and the exact React/TypeScript scaffold (`tsc -b && vite build`). Build logs are returned and preview files are stored in the shared project volume; temporary smoke-test workspace was removed.
- Added HMAC preview access (30-minute expiry) and a dedicated unauthenticated GET path that validates the signed workspace/project claims. Session/legacy auth filters bypass only matching GET preview URLs; a MockMvc integration test verifies token access and tamper rejection.
- Nginx has a dedicated signed-preview location so its dashboard-wide `X-Frame-Options: DENY` and `frame-ancestors 'none'` policy do not override the backend's sandboxed preview policy. Normal dashboard pages retain their strict framing policy.
- Full backend regression suite passed (66 tests); the expanded project workspace integration suite passed (5 tests). Go runner tests pass, frontend production build/lint pass, and PostgreSQL migration chain V1–V17 passed on an isolated temporary database. Preview runner is healthy locally. Dashboard/backend image refresh remains local-only; no public deployment.


### Final local preview verification (2026-11-25)
- The preview runner now runs as UID 10001 with supplementary Docker-socket group only, read-only runner filesystem, all runner capabilities dropped, 256 MiB runner memory cap and 0.5 CPU cap. It is only on the internal preview network; no host port is published.
- Each disposable build container has no network, no host/workspace bind mounts, UID 10001, all capabilities dropped, `no-new-privileges`, 512 MiB RAM, 1 CPU, 128 PIDs, a 128 MiB writable-layer cap and a 192 MiB `/tmp` tmpfs. It is deleted after each build.
- Verified a real React/TypeScript Vite project builds successfully through the backend-to-runner private network after the final storage cap. Build logs returned successfully; the temporary smoke-test workspace was deleted.
- Verified runner Go tests with `-race`, `go vet`, frontend production build/lint, Nginx config syntax, and dashboard health. The refreshed local dashboard bundle contains the Build & preview controls.
- Backend regression suite: 67 tests, zero failures/errors. V1–V17 PostgreSQL migration chain tested on a temporary PostgreSQL 17 database; the local dev database's V16 CHAR/VARCHAR mismatch was corrected with the V17 forward migration also checked.
- No public deployment, public preview URL, publishing, or launch was performed.


## M2 implementation progress — source editing, immutable history and QA gates (2026-11-25)
- Added V18 immutable project artifact versions with full text content, SHA-256 hashes, actor and timestamps. Starter files, recovered workspace files, AI-generated source, manual edits and QA reports are snapshotted.
- Added workspace-scoped version history and read-only version retrieval APIs. Manual source edits are restricted to allowlisted frontend folders and 50,000 characters per file; edits invalidate the current preview until rebuilt.
- Added V19 to pin every review request to an exact immutable artifact version ID, version number and SHA-256 hash. Existing artifacts without history are snapshotted at review request. Review decisions still verify that the current file has not changed since the review was submitted.
- Added on-demand QA and security review agents. They receive bounded frontend source context, treat source as untrusted data, report severity, evidence, limitations and remediation, and save results as immutable project report artifacts. They do not edit source, execute project code or publish.
- Dashboard exposes QA and security review actions alongside Build & Preview and displays the report. The agent catalogue now contains six specialist agents.
- Integration coverage verifies manual edits create immutable snapshots and reviews pin the exact version. The full backend regression suite passed (70 tests, 0 failures); focused workflow/workspace integration tests passed again after the latest checkpoint changes. The local PostgreSQL database has Flyway V19 applied and the Compose stack is healthy.

## Still required before production autonomy
- M1: explicit retry with spend-safe idempotency, dependency graph and lease-based multi-instance recovery. Website source-generation approval gates, per-step usage attribution and atomic queued-run claiming are implemented. Pause/resume/cancel and conservative restart recovery are implemented for a single local instance; multi-instance recovery is not yet safe.
- M2: dedicated isolated/rootless build daemon or restricted Docker API broker; browser-driven functional tests, accessibility automation, dependency/SCA scanning, artifact diff UI and QA findings linked to source lines. Current QA/security agents are advisory source reviews, not proof of safety.
- M3: durable research source registry, verified citations and customer-validation evidence.
- M4: bounded repair loop with owner review, staging deployment and separate explicit production-launch approval.
- M5–M7: design/image/video creation, approved publishing connectors, analytics and outcome-based improvement.
- Production operations: backups/restore drills, monitoring/alerts, rate limits, abuse controls, secret rotation, incident response, privacy/data retention policy, tenant-isolation penetration tests and independent security review.


## Website-build workflow implementation (2026-11-25)
- Added a distinct `website-build` workflow option alongside planning-only and marketing-plan workflows.
- The new pipeline runs research, product requirements/MVP definition, brand/content brief, restricted developer-agent source generation, isolated preview build, and launch-readiness planning.
- The developer stage passes bounded prior-stage context into the existing JSON/path/size-validated source-generation service. Generated source is snapshotted and is never run on the backend or host.
- The preview stage uses the existing disposable network-disabled builder. A successful build creates a 30-minute signed preview link for human inspection; it does not deploy or publish.
- The dashboard workflow selector now exposes the new pipeline. Production deployment and public publishing remain separate and require explicit owner approval.
- Integration coverage checks the new template and expected developer/preview stages. Provider-backed end-to-end generation is still not run in tests because it would incur external AI usage; run it manually after confirming provider credentials and budget limits.

## Marketing production foundation (2026-11-25)
- Added tenant-scoped marketing campaign records tied to venture projects, with channel/format validation, draft editing, submit-for-review, owner/admin decisions, feedback, and future queue times.
- Added a Campaign Studio to the project dashboard. The existing budget-tracked Content Agent can generate channel-specific copy, carousel slide copy, email drafts, and short-form video scripts/shot directions. Generated text is editable and must be saved and reviewed.
- Campaign state transitions are enforced server-side: DRAFT/CHANGES_REQUESTED -> PENDING_REVIEW -> APPROVED -> SCHEDULED. Rejected work cannot be scheduled. Only workspace owners/admins can approve or queue campaigns.
- Added PostgreSQL migration V20 and integration coverage for the campaign review and scheduling lifecycle.
- IMPORTANT: SCHEDULED currently means queued in Aurora only. There is no Meta/Instagram, TikTok, YouTube, LinkedIn, or email delivery adapter connected; no public post is sent. OAuth credentials, platform permission review, media upload adapters, delivery retries/idempotency, and delivery receipts remain future work.
- Image/video file generation is not yet wired into Aurora's backend. The current Content Agent produces copy and production instructions only; it does not claim to render media.

## Social publishing integration research (official docs checked 2026-11-25)
- Meta/Instagram is the best first connector: Instagram publishing targets professional accounts, requires a Meta login flow and app permissions, media must be publicly reachable, and Meta documents media-container creation followed by `media_publish`. Enforce the documented 100-post rolling 24-hour account limit. Facebook Page publishing uses Page access tokens and Page task/permission checks. References: https://developers.facebook.com/documentation/instagram-platform/content-publishing and https://developers.facebook.com/docs/graph-api/reference/page/feed/
- TikTok Content Posting API requires creator-info lookup, explicit user consent, the `video.publish` scope and asynchronous upload/status handling. Unaudited apps are restricted to private viewing until audit. Reference: https://developers.tiktok.com/docs/en/content-posting-api-reference-direct-post
- YouTube uploads use OAuth 2.0 and the `youtube.upload` scope with resumable uploads; test uploads should default to private or unlisted. Reference: https://developers.google.com/youtube/data/guides/uploading_a_video
- LinkedIn's current Posts API supports organic text/image/video/document posts, requires a versioned `Linkedin-Version` header, and uses member or organization social scopes. Image/video posts require uploaded asset URNs first. Reference: https://learn.microsoft.com/en-us/linkedin/marketing/community-management/shares/posts-api
- Before any connector is enabled: implement per-workspace OAuth connection records with encrypted tokens, refresh/expiry handling, minimal scopes, disconnect/revoke, platform account ownership checks, consent capture, idempotent publish jobs, retry/backoff, provider receipts, webhook/status reconciliation, and an explicit final publish confirmation.
- Analytics should be a separate ingestion layer: store per-platform post IDs and timestamped metric snapshots, preserve source metric names/periods, and never blend reach, impressions, views or engagement into a single ambiguous number. Access/retention rules must be checked per platform before enabling analytics.


## M1 implementation progress — workflow-level AI usage attribution (2026-11-25)
- Added additive PostgreSQL migration V21 to attach each AI-backed workflow step to its `agent_run_id` and snapshot the provider usage status, prompt/completion/total tokens and provider-reported USD cost.
- Workflow steps now capture usage for standard agent stages and the restricted developer-agent website generation stage. Preview-builder stages correctly have no AI provider usage record.
- The API exposes the new step usage fields, and the project workflow UI displays the linked agent run, token totals and cost only when the provider actually reports it. Missing or unknown cost is labelled as unavailable/not reported; it is never presented as zero.
- Added focused tests for reported usage and missing ledger data. Full backend regression suite passes (73 tests, zero failures/errors); frontend lint and production build pass.
- Rebuilt and restarted the local backend/dashboard containers. Flyway validated all 21 migrations and successfully applied V21 to the existing local PostgreSQL database. Verified the six new columns and the successful V21 migration record directly in PostgreSQL. Backend and dashboard are healthy; dashboard returns HTTP 200. This remains local-only; no Netlify or public deployment was performed.


## M1 implementation progress — atomic workflow claiming (2026-11-25)
- Added a database compare-and-set claim for queued workflow runs. The worker must atomically change a run from QUEUED to RUNNING before loading and executing its steps; a duplicate dispatch receives zero updated rows and exits without invoking an agent.
- Added integration coverage proving that the first worker claim succeeds, a second claim for the same run is rejected, and the persisted run has a start timestamp.
- Full backend regression suite passes (74 tests, zero failures/errors). Rebuilt and restarted the local backend image; backend is healthy, PostgreSQL remains at schema V21, and the public-registration bootstrap endpoint returns HTTP 200. Dashboard remains healthy. Multi-instance recovery is still intentionally not marked safe because running-job recovery needs worker leases/heartbeats before multiple instances can share a queue. No public deployment was performed.

## M1 implementation progress — website source-generation approval gate (2026-11-25)
- Added V22 additive migration for workflow-step approval metadata.
- Website-build workflows now pause after research, product requirements and brand/content planning, immediately before the developer-agent source-generation request. The step and workflow show `AWAITING_APPROVAL`; no code-generation AI request is sent while awaiting approval.
- Only workspace OWNER/ADMIN can approve the protected step. Approval is recorded with a timestamp and audit event, then the worker resumes from that exact pending step; completed planning stages are not repeated.
- Dashboard shows the approval checkpoint and exposes an approval action only to owners/admins. Other workspace members can see that the workflow is waiting. An owner/admin can cancel a workflow while it is waiting; cancellation also closes the waiting step.
- This gate covers source generation only. It does not authorize production deployment, public publishing, spending outside existing AI budget controls, or multi-instance queue recovery.
- Verification after V22: backend regression suite passes 78 tests with zero failures/errors; frontend lint reports zero warnings/errors and the production build succeeds. Local Docker backend, dashboard, PostgreSQL and preview-runner are healthy; PostgreSQL validated and applied V22. Dashboard and registration bootstrap return HTTP 200. Changes remain local; no Netlify/public deployment was performed.


## M1 implementation progress — conservative workflow retry (2026-11-25)
- Added V23 retry metadata with a strict one-retry-per-step limit and a separate previous-agent-run reference; existing Agent Runs and provider-usage ledger entries remain immutable.
- Owner/admin retry is allowed only for a failed step in a failed workflow. AI-backed stages additionally require the linked Agent Run to be FAILED and its provider usage to be REPORTED or MANUALLY_RECONCILED. UNKNOWN, PENDING, missing usage, missing run IDs and ambiguous generation failures are blocked pending reconciliation. The isolated preview-builder stage may be retried because it does not call an AI provider.
- Retry uses a database compare-and-set from FAILED to QUEUED, then resets only the failed step. Duplicate clicks cannot queue duplicate retry dispatches. The next AI attempt creates a new Agent Run and passes through existing request allowance, budget reservation and provider usage accounting.
- One retry maximum per step; no automatic retries. Existing completed steps are skipped. This does not provide provider-side exactly-once semantics for an external request whose outcome is unknown.
- Verification after V23: full backend regression suite passes 81 tests across 24 suites with zero failures/errors; frontend lint reports zero warnings/errors and production build succeeds. PostgreSQL successfully applied V23; backend, dashboard, database and preview-runner are healthy; dashboard and registration bootstrap return HTTP 200. No public deployment was performed.


## M1 implementation progress — dependency graph and worker leases (2026-11-25)
- V24 adds explicit stage dependency orders and worker ownership, heartbeat, and lease-expiry fields with an index. Current templates are deliberately linear; dependency edges are saved and checked before each stage can execute.
- Workflow claims now atomically attach a unique worker token and 90-second lease. A local scheduler renews active worker leases every 20 seconds. Lease ownership is required for renewal, and a worker cannot renew an expired lease.
- Startup and periodic recovery only fail genuinely expired RUNNING leases; they do not mark another instance's live RUNNING lease as interrupted. Expired active AI stages are failed conservatively and never automatically replayed, since provider completion/billing may be unknown. Queued jobs are periodically dispatched and atomic claiming avoids duplicate workers.
- This provides crash detection and lease-aware coordination, not full distributed exactly-once execution: an unknown external provider request still requires manual reconciliation, and active steps are not resumed automatically after lease expiry.

- Verification: backend suite 83 tests across 24 suites (zero failures/errors/skips); frontend lint/build passed. V24 applied in local PostgreSQL; backend/dashboard/database/preview-runner healthy; dashboard and registration bootstrap HTTP 200. Docker backend build now packages the host-built JAR, avoiding redundant dependency downloads inside Docker. No public deployment.


## Website QA workflow integration (2026-11-25)
- Website-build workflow now includes a QA Agent stage after a successful isolated build and before launch-readiness planning.
- QA stage receives a bounded selection of project source files plus explicit build evidence and reviews UX, user journeys, controls, forms, responsiveness, semantics/accessibility, state handling, content and likely runtime risks.
- Output is persisted as a workflow step and project agent-output artifact, with prioritised severity, source references, remediation, verification steps and a release recommendation.
- It is a **source-level advisory review**, not browser automation: the QA agent has not clicked controls, rendered mobile layouts, checked live links or run browser tests. Those checks are explicitly marked NOT TESTED. A dedicated browser QA container with screenshots, functional assertions and accessibility automation remains a next milestone.
- Workflow integration tests assert the new QA stage position and preserve the source-generation approval and preview-build gates.

## Browser QA runner (2026-11-25)
- Added isolated Playwright Chromium service in `infrastructure/browser-qa`; internal authenticated endpoint only, no host port published.
- Backend `POST /api/v1/projects/{projectId}/quality/browser` requires workspace membership, rebuilds the project in the restricted preview builder, then tests the signed preview through the internal backend URL.
- Browser checks initial HTTP load, page title, console/page errors, failed requests, HTTP errors, internal links, interactive-element inventory, unlabeled buttons, image alt attributes, mobile horizontal overflow; captures desktop and mobile screenshots.
- Browser outbound page requests are restricted to preview same-origin. QA service uses a separate internal Docker network (backend joins it; QA cannot reach database/preview-runner directly), resource caps, read-only filesystem, tmpfs, dropped capabilities, no host port.
- Reports are saved as project QA artifacts; screenshots are returned to the project panel for viewing but excluded from persistent report JSON to keep storage smaller. The website-build workflow now runs this browser stage after the source QA review and before operations/readiness planning.
- Findings are advisory. Buttons/forms are inventoried, not indiscriminately clicked/submitted to avoid triggering destructive or external side effects. Mobile uses a viewport, not a mobile device emulator. External assets/links are blocked and are not fully verified. Browser testing is not a penetration test.


## Verification checkpoint (2026-11-27)
- Backend regression suite: 83 tests, zero failures/errors/skips.
- Frontend TypeScript/Vite production build: passed; Oxlint: 0 warnings and 0 errors.
- `git diff --check` and Docker Compose configuration validation: passed.
- Local Compose services were healthy at the check; dashboard HTTP 200 and `/api/v1/system/status` returned `OPERATIONAL`. Local PostgreSQL schema is at V24.
- Existing Netlify frontend responds HTTP 200 and its bundle points to the Render API. The Render API status endpoint timed out (70-second check); cloud backend availability/configuration is not confirmed. No cloud migrations, deploy, publishing or user-account actions were attempted.
- Setup bootstrap was improved to generate unique 256-bit database, preview-runner, preview-signing and browser-QA secrets, set `.env` permissions to 600, and detect the Docker socket GID where possible. A temporary-directory smoke test confirmed secret generation and permissions without touching the real `.env`.

## Release readiness snapshot
- **Core local Business Factory workflow:** implemented through planning, code generation approval, isolated build/preview, QA/security/browser reports, project artifacts and campaign draft/review queue.
- **Still prototype-level:** AI provider research citations need live validation; workflows need multi-instance-safe active-step recovery and provider idempotency; browser QA has limitations; preview runner uses Docker socket privileges; user-facing image/video generation and actual social publishing are not connected; analytics and production deploy approval pipeline are incomplete.
- **Release blocker:** verify the cloud backend and cloud migration/config state; complete secrets/credentials setup, independent budget caps, backup restore drill, tenant/security review and abuse/privacy controls before public onboarding.


## Campaign analytics foundation (2026-11-27)
- Added V25 `campaign_performance_metrics`, scoped to workspace and campaign, with one upsertable daily report per campaign.
- Owner/admin can record platform insights; workspace members/viewers can read them. Negative counts, impossible reach/click/conversion relationships, future reporting dates, and invalid monetary values are rejected.
- Campaign Studio now displays daily impressions, clicks, conversions, spend, revenue, click-through rate and return on ad spend, with manual reporting entry clearly labelled. This is not yet an automated social analytics integration.
- Next integrations: provider-specific account authorization and publishing APIs, asset generation/storage (image/video), and automated analytics sync. Those require the user's provider credentials, platform app approvals and account permissions; no credentials are embedded in AURORA.

## Creative Studio assets (2026-11-27)
- Added workspace/project-scoped creative asset storage and metadata for images and short videos. Uploads are limited to 5 MB and accepted only for PNG, JPEG, WebP, GIF, MP4 and WebM with basic file-signature checks; SVG and arbitrary files are rejected.
- Assets are stored outside generated source workspaces, exposed only through authenticated project-scoped endpoints, and can be previewed or deleted by workspace members with edit access.
- Campaign Studio now includes a private asset library, notes, upload/delete controls, image/video previews and a reusable project-aware creative prompt builder.
- Actual AI image/video generation and social account publishing remain disconnected; this release provides asset management and a prompt handoff, not a claim that media is generated or published automatically.

## Creative Studio verification (2026-11-27)
- Full backend regression suite: 83 tests passed; two additional focused Creative Studio media-signature tests passed.
- Frontend production build passed; Oxlint reports zero warnings/errors. git diff --check and Docker Compose configuration validation passed.
- Local Docker backend and dashboard were rebuilt and restarted. Flyway validated 26 migrations and applied V25/V26 to the existing local PostgreSQL database; schema is now V26. Backend health is healthy, dashboard HTTP 200, API system status OPERATIONAL, and the unauthenticated creative-asset endpoint correctly returns HTTP 401.
- Creative assets are stored in the existing persistent project-workspace volume, in a separate creative-assets directory so media does not enter generated website source/builds. No cloud deploy, social publishing, or external media-provider request was made.

## Campaign handoff/export pack (2026-11-27)
- Added an authenticated, workspace/project-scoped campaign export endpoint. Members can download a ZIP containing campaign copy, CTA, current review/scheduling metadata, an asset manifest, and up to 10 project creative assets within a 20 MB asset budget.
- Campaign Studio now exposes a one-click campaign pack download for review and handoff to a human publisher.
- Exporting does not publish or schedule content externally. The local queue remains a planning state until platform-specific OAuth, account ownership verification, permissions, media upload, delivery receipts and retry handling are configured.
- Current deployment configuration uses the OpenRouter-compatible chat endpoint and openrouter/free; no dedicated image model is configured. No social OAuth app credentials are configured. AI image/video generation and automatic social posting therefore remain disabled rather than silently falling back to paid or unverified providers.
- Verification: frontend production build and lint pass; backend regression suite 85 tests, zero failures/errors/skips; git diff --check passes.
