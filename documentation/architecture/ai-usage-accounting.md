# AURORA AI usage accounting

## Two separate measurements

AURORA tracks two different things:

1. **Workspace request allowance** — the existing application-level count used to limit member requests. Owner/admin exemption applies only to this counter. It does not make AI calls free.
2. **Provider usage ledger** — one record per attempted provider call, linked to the workspace and agent run. It stores provider/model, prompt/completion/total tokens, provider request ID, request status and provider-reported USD cost.

## How to interpret the dashboard

- `providerRequests` counts ledger entries, including calls with unknown outcomes.
- `reportedRequests` counts entries where the provider returned a usage object.
- `requestsWithUnknownUsage` includes attempts with no confirmed usage object or an unknown outcome.
- Token totals include only non-negative token counts returned by the provider.
- `reportedCostUsd` sums only costs explicitly returned by the provider. A missing cost is not interpreted as zero; `requestsWithoutReportedCost` exposes those entries.
- Values are scoped to the current UTC calendar month and workspace.

## Limits and known gaps

The ledger is not a provider invoice and does not yet reconcile credits, discounts, taxes, account-level adjustments, or delayed billing corrections. Use the provider account's billing page/export for financial reconciliation.

Provider-reported cost is only known after a request completes, so a post-response check alone cannot prevent concurrent overspend. Flyway V9 adds a database-serialized pre-request reservation guard for the deployment-wide platform ceiling and optional per-business ceilings. It is an application-side control and must be paired with an independent provider-side billing cap; a single request can still overrun if its configured reservation is too low.

Business-level attribution is also pending: current agent runs are workspace-scoped and do not yet carry a `business_workspace_id`.

## Business attribution (V8)

Agent runs may optionally reference a business workspace. The API verifies the business belongs to the active tenant workspace before saving the run. Provider ledger summaries group usage by business and keep unassigned platform/workspace work in a separate bucket. Existing runs remain unassigned; V8 does not guess their business from prompt text or historical context.

Business attribution is the basis for per-business budgets. V9 implements pre-dispatch reservations and leaves missing-cost reservations held rather than releasing them. Monetary ceilings are disabled by default until an operator supplies a defensible per-request reservation maximum and explicitly configures the desired limits.

## Monetary budget guard (initial implementation)

A database-backed reservation is created and committed before an agent request is dispatched whenever a platform or business monetary ceiling is enabled. A singleton database lock serializes reservations across application instances, preventing concurrent calls from independently spending the same remaining balance. The configured maximum per-request reservation is held against the platform ceiling and, where selected, the business ceiling. Provider-reported costs settle a reservation against actual cost; missing or malformed cost data leaves the reservation held and unavailable for reuse. A provider-reported overrun is recorded as actual spend and therefore blocks subsequent requests, but cannot undo the individual provider charge that already occurred.

Configuration:

- `AURORA_AI_PLATFORM_MONTHLY_BUDGET_USD=0` disables the platform-wide ceiling. A positive value enables it.
- `AURORA_AI_MAX_REQUEST_RESERVATION_USD=0` disables monetary reservations. It must be a defensible positive upper bound whenever a platform ceiling is enabled, and must be configured before a business ceiling is enabled.
- Workspace owners and administrators can set a monthly USD ceiling per business through `PUT /api/v1/businesses/{businessId}/ai-budget`.
- Unattributed workspace-level work is covered by the platform ceiling, but not a business-specific ceiling.
- Unknown-cost reservations remain held, including across month boundaries. They require verification against provider billing records before the reserved amount can be reused. No self-service release endpoint is provided yet.

This is an application-side guard, not a substitute for provider-side billing limits. Configure an independent provider account hard cap as well. The platform ceiling is deployment-controlled, not editable by workspace owners; this intentionally avoids granting tenant administrators global platform authority. Dashboard threshold warnings and owner/admin business-budget controls are implemented. Email/push alert delivery, a privileged reconciliation workflow for unknown-cost holds, and platform-owner UI remain follow-up work.


### Unknown-cost reconciliation

Owner/admin users can review the oldest 100 unresolved reservations in their active workspace. Manual reconciliation requires an actual provider-billing amount, a billing reference and an evidence note. It is workspace-scoped, serialized with reservation creation, and allowed once per reservation. The reservation and matching usage-ledger row are updated together; the ledger labels the cost `MANUALLY_RECONCILED`. This is an operator assertion backed by the supplied provider billing reference, not an automated provider verification. Active runs are excluded; only completed or one-hour-stale runs can be reconciled. If a late provider response subsequently arrives, its provider-reported cost supersedes the manual value while the manual evidence remains available for audit. The application does not release an unknown-cost hold without this explicit review.
