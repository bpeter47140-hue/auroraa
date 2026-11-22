package com.aurora;

import com.aurora.identity.TenantWorkspace;
import com.aurora.identity.TenantWorkspaceRepository;
import com.aurora.tasks.TaskItem;
import com.aurora.tasks.TaskRepository;
import com.aurora.approvals.ApprovalRequest;
import com.aurora.approvals.ApprovalRepository;
import com.aurora.businesses.BusinessWorkspace;
import com.aurora.businesses.BusinessRepository;
import com.aurora.memory.MemoryEntry;
import com.aurora.memory.MemoryRepository;
import com.aurora.research.CustomerInsight;
import com.aurora.research.CustomerInsightRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class WorkspaceIsolationTests {
    @Autowired TenantWorkspaceRepository workspaces;
    @Autowired TaskRepository tasks;
    @Autowired ApprovalRepository approvals;
    @Autowired BusinessRepository businesses;
    @Autowired MemoryRepository memories;
    @Autowired CustomerInsightRepository insights;

    @Test
    void tenantOwnedRecordsAreOnlyReturnedForTheirWorkspace() {
        TenantWorkspace alpha = workspaces.saveAndFlush(new TenantWorkspace("Alpha"));
        TenantWorkspace beta = workspaces.saveAndFlush(new TenantWorkspace("Beta"));

        TaskItem alphaTask = new TaskItem("Alpha task", null, "NORMAL"); alphaTask.assignWorkspace(alpha); tasks.saveAndFlush(alphaTask);
        TaskItem betaTask = new TaskItem("Beta task", null, "NORMAL"); betaTask.assignWorkspace(beta); tasks.saveAndFlush(betaTask);
        assertThat(tasks.findAllByWorkspace_IdOrderByCreatedAtDesc(alpha.getId())).extracting(TaskItem::getTitle).containsExactly("Alpha task");
        assertThat(tasks.findByIdAndWorkspace_Id(betaTask.getId(), alpha.getId())).isEmpty();

        ApprovalRequest approval = new ApprovalRequest("Alpha approval", "Details", "CONTENT"); approval.assignWorkspace(alpha); approvals.saveAndFlush(approval);
        assertThat(approvals.findAllByWorkspace_IdOrderByCreatedAtDesc(beta.getId())).isEmpty();

        BusinessWorkspace business = new BusinessWorkspace("Alpha business", null, null); business.assignWorkspace(alpha); businesses.saveAndFlush(business);
        assertThat(businesses.findByIdAndWorkspace_Id(business.getId(), beta.getId())).isEmpty();

        MemoryEntry memory = new MemoryEntry("GENERAL", "Alpha memory", "Private note", null); memory.assignWorkspace(alpha); memories.saveAndFlush(memory);
        assertThat(memories.findTop100ByWorkspace_IdOrderByCreatedAtDesc(beta.getId())).isEmpty();

        CustomerInsight insight = new CustomerInsight("PROBLEM", "P1", "Alpha-only problem", null, null, null, null, "UNVERIFIED"); insight.assignWorkspace(alpha); insights.saveAndFlush(insight);
        assertThat(insights.findTop200ByWorkspace_IdOrderByCreatedAtDesc(beta.getId())).isEmpty();
    }
}
