DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM tenant_workspaces)
       AND (
          EXISTS (SELECT 1 FROM work_tasks WHERE workspace_id IS NULL)
          OR EXISTS (SELECT 1 FROM approval_requests WHERE workspace_id IS NULL)
          OR EXISTS (SELECT 1 FROM business_workspaces WHERE workspace_id IS NULL)
          OR EXISTS (SELECT 1 FROM memory_entries WHERE workspace_id IS NULL)
          OR EXISTS (SELECT 1 FROM customer_insights WHERE workspace_id IS NULL)
          OR EXISTS (SELECT 1 FROM agent_runs WHERE workspace_id IS NULL)
          OR EXISTS (SELECT 1 FROM audit_events WHERE workspace_id IS NULL)
       ) THEN
        INSERT INTO tenant_workspaces(name,status,created_at,updated_at)
        VALUES ('Migrated legacy workspace','ACTIVE',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP);
    END IF;
END $$;

UPDATE work_tasks SET workspace_id=(SELECT MIN(id) FROM tenant_workspaces) WHERE workspace_id IS NULL AND EXISTS (SELECT 1 FROM tenant_workspaces);
UPDATE approval_requests SET workspace_id=(SELECT MIN(id) FROM tenant_workspaces) WHERE workspace_id IS NULL AND EXISTS (SELECT 1 FROM tenant_workspaces);
UPDATE business_workspaces SET workspace_id=(SELECT MIN(id) FROM tenant_workspaces) WHERE workspace_id IS NULL AND EXISTS (SELECT 1 FROM tenant_workspaces);
UPDATE memory_entries SET workspace_id=(SELECT MIN(id) FROM tenant_workspaces) WHERE workspace_id IS NULL AND EXISTS (SELECT 1 FROM tenant_workspaces);
UPDATE customer_insights SET workspace_id=(SELECT MIN(id) FROM tenant_workspaces) WHERE workspace_id IS NULL AND EXISTS (SELECT 1 FROM tenant_workspaces);
UPDATE agent_runs SET workspace_id=(SELECT MIN(id) FROM tenant_workspaces) WHERE workspace_id IS NULL AND EXISTS (SELECT 1 FROM tenant_workspaces);
UPDATE audit_events SET workspace_id=(SELECT MIN(id) FROM tenant_workspaces) WHERE workspace_id IS NULL AND EXISTS (SELECT 1 FROM tenant_workspaces);

ALTER TABLE work_tasks ALTER COLUMN workspace_id SET NOT NULL;
ALTER TABLE approval_requests ALTER COLUMN workspace_id SET NOT NULL;
ALTER TABLE business_workspaces ALTER COLUMN workspace_id SET NOT NULL;
ALTER TABLE memory_entries ALTER COLUMN workspace_id SET NOT NULL;
ALTER TABLE customer_insights ALTER COLUMN workspace_id SET NOT NULL;
ALTER TABLE agent_runs ALTER COLUMN workspace_id SET NOT NULL;
ALTER TABLE audit_events ALTER COLUMN workspace_id SET NOT NULL;
