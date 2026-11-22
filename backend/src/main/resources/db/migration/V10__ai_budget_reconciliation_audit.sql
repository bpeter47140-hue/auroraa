ALTER TABLE ai_budget_reservations
    ADD COLUMN settlement_source VARCHAR(16),
    ADD COLUMN reconciliation_reference VARCHAR(160),
    ADD COLUMN reconciliation_note VARCHAR(1000),
    ADD COLUMN reconciled_by_role VARCHAR(16);

UPDATE ai_budget_reservations
SET settlement_source = 'PROVIDER'
WHERE status = 'SETTLED';

ALTER TABLE ai_budget_reservations
    ADD CONSTRAINT ck_ai_budget_settlement_source
        CHECK (settlement_source IS NULL OR settlement_source IN ('PROVIDER', 'MANUAL')),
    ADD CONSTRAINT ck_ai_budget_reconciled_metadata
        CHECK (settlement_source <> 'MANUAL' OR
            (reconciliation_reference IS NOT NULL AND reconciliation_note IS NOT NULL AND reconciled_by_role IS NOT NULL));
CREATE INDEX idx_ai_budget_reservations_workspace_held
    ON ai_budget_reservations(workspace_id, status, created_at);
