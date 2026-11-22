package com.aurora.identity;

import com.aurora.tasks.*;
import com.aurora.approvals.*;
import com.aurora.businesses.*;
import com.aurora.memory.*;
import com.aurora.research.*;
import com.aurora.agents.*;
import com.aurora.audit.*;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class WorkspaceDataBackfill implements ApplicationRunner {
    private final TenantWorkspaceRepository workspaces;
    private final TaskRepository tasks;
    private final ApprovalRepository approvals;
    private final BusinessRepository businesses;
    private final MemoryRepository memories;
    private final CustomerInsightRepository insights;
    private final AgentRunRepository runs;
    private final AuditRepository audits;

    public WorkspaceDataBackfill(TenantWorkspaceRepository workspaces, TaskRepository tasks,
        ApprovalRepository approvals, BusinessRepository businesses, MemoryRepository memories,
        CustomerInsightRepository insights, AgentRunRepository runs, AuditRepository audits) {
        this.workspaces=workspaces; this.tasks=tasks; this.approvals=approvals; this.businesses=businesses;
        this.memories=memories; this.insights=insights; this.runs=runs; this.audits=audits;
    }

    @Override @Transactional
    public void run(ApplicationArguments args) {
        TenantWorkspace defaultWorkspace = workspaces.findAll().stream()
            .min(java.util.Comparator.comparing(TenantWorkspace::getId)).orElse(null);
        if (defaultWorkspace == null) return; // Fresh test databases remain empty until first-owner registration.
        tasks.findAll().stream().filter(x -> x.getWorkspace()==null).forEach(x -> x.assignWorkspace(defaultWorkspace));
        approvals.findAll().stream().filter(x -> x.getWorkspace()==null).forEach(x -> x.assignWorkspace(defaultWorkspace));
        businesses.findAll().stream().filter(x -> x.getWorkspace()==null).forEach(x -> x.assignWorkspace(defaultWorkspace));
        memories.findAll().stream().filter(x -> x.getWorkspace()==null).forEach(x -> x.assignWorkspace(defaultWorkspace));
        insights.findAll().stream().filter(x -> x.getWorkspace()==null).forEach(x -> x.assignWorkspace(defaultWorkspace));
        runs.findAll().stream().filter(x -> x.getWorkspace()==null).forEach(x -> x.assignWorkspace(defaultWorkspace));
        audits.findAll().stream().filter(x -> x.getWorkspace()==null).forEach(x -> x.assignWorkspace(defaultWorkspace));
        tasks.flush(); approvals.flush(); businesses.flush(); memories.flush(); insights.flush(); runs.flush(); audits.flush();
    }
}
