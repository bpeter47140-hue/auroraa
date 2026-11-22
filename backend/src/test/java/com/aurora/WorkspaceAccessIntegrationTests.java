package com.aurora;

import com.aurora.identity.AuthDtos;
import com.aurora.identity.IdentityAuthService;
import com.aurora.identity.TenantWorkspace;
import com.aurora.identity.TenantWorkspaceRepository;
import com.aurora.tasks.TaskItem;
import com.aurora.tasks.TaskRepository;
import com.aurora.businesses.BusinessRepository;
import com.aurora.businesses.BusinessWorkspace;
import com.aurora.memory.MemoryRepository;
import com.aurora.memory.MemoryEntry;
import com.aurora.research.CustomerInsightRepository;
import com.aurora.research.CustomerInsight;
import com.aurora.approvals.ApprovalRepository;
import com.aurora.approvals.ApprovalRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "aurora.security.sessions.enabled=true")
@Transactional
class WorkspaceAccessIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired IdentityAuthService auth;
    @Autowired TenantWorkspaceRepository workspaces;
    @Autowired TaskRepository tasks;
    @Autowired BusinessRepository businesses;
    @Autowired MemoryRepository memories;
    @Autowired CustomerInsightRepository insights;
    @Autowired ApprovalRepository approvals;

    @Test
    void authenticatedUserCanOnlySelectAndReadTheirOwnWorkspace() throws Exception {
        AuthDtos.AuthResponse owner = auth.registerFirstOwner(new AuthDtos.RegisterRequest(
            "workspace.owner@example.com", "a-secure-password-123", "Workspace Owner", "Alpha"
        ));
        Long alphaId = owner.workspaces().getFirst().id();

        TenantWorkspace beta = workspaces.saveAndFlush(new TenantWorkspace("Beta"));
        TaskItem alphaTask = new TaskItem("Alpha private task", null, "NORMAL");
        alphaTask.assignWorkspace(workspaces.findById(alphaId).orElseThrow());
        tasks.saveAndFlush(alphaTask);
        TaskItem betaTask = new TaskItem("Beta private task", null, "NORMAL");
        betaTask.assignWorkspace(beta);
        tasks.saveAndFlush(betaTask);

        mvc.perform(get("/api/v1/tasks")
                .header("Authorization", "Bearer " + owner.token())
                .header("X-Aurora-Workspace", alphaId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].title", is("Alpha private task")));

        mvc.perform(get("/api/v1/tasks")
                .header("Authorization", "Bearer " + owner.token())
                .header("X-Aurora-Workspace", beta.getId()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error", is("No access to the selected workspace")));
    }

    @Test
    void crossWorkspaceUpdatesAndDeletesAreHiddenAsNotFound() throws Exception {
        AuthDtos.AuthResponse owner = auth.registerFirstOwner(new AuthDtos.RegisterRequest(
            "isolation.owner@example.com", "a-secure-password-123", "Isolation Owner", "Alpha"
        ));
        Long alphaId = owner.workspaces().getFirst().id();
        TenantWorkspace beta = workspaces.saveAndFlush(new TenantWorkspace("Beta"));

        TaskItem task = new TaskItem("Beta task", null, "NORMAL");
        task.assignWorkspace(beta);
        task = tasks.saveAndFlush(task);
        BusinessWorkspace business = new BusinessWorkspace("Beta business", null, null);
        business.assignWorkspace(beta);
        business = businesses.saveAndFlush(business);
        MemoryEntry memory = new MemoryEntry("FINDING", "Beta memory", "Private", null);
        memory.assignWorkspace(beta);
        memory = memories.saveAndFlush(memory);
        CustomerInsight insight = new CustomerInsight("PROBLEM", "Participant", "Private problem", null, null, null, null, "REPORTED");
        insight.assignWorkspace(beta);
        insight = insights.saveAndFlush(insight);
        ApprovalRequest approval = new ApprovalRequest("Beta approval", "Private action", "TEST");
        approval.assignWorkspace(beta);
        approval = approvals.saveAndFlush(approval);

        String token = owner.token();
        String workspace = String.valueOf(alphaId);
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/v1/tasks/{id}/status", task.getId())
                .header("Authorization", "Bearer " + token).header("X-Aurora-Workspace", workspace)
                .contentType("application/json").content("{\"status\":\"COMPLETED\"}"))
            .andExpect(status().isNotFound());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/v1/tasks/{id}", task.getId())
                .header("Authorization", "Bearer " + token).header("X-Aurora-Workspace", workspace))
            .andExpect(status().isNotFound());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/v1/businesses/{id}", business.getId())
                .header("Authorization", "Bearer " + token).header("X-Aurora-Workspace", workspace)
                .contentType("application/json").content("{\"name\":\"Hijack\",\"status\":\"ACTIVE\"}"))
            .andExpect(status().isNotFound());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/v1/memory/{id}", memory.getId())
                .header("Authorization", "Bearer " + token).header("X-Aurora-Workspace", workspace))
            .andExpect(status().isNotFound());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/v1/customer-insights/{id}", insight.getId())
                .header("Authorization", "Bearer " + token).header("X-Aurora-Workspace", workspace))
            .andExpect(status().isNotFound());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/approvals/{id}/approve", approval.getId())
                .header("Authorization", "Bearer " + token).header("X-Aurora-Workspace", workspace))
            .andExpect(status().isNotFound());
    }

    @Test
    void protectedEndpointsRejectMissingAndRevokedSessions() throws Exception {
        mvc.perform(get("/api/v1/tasks"))
            .andExpect(status().isUnauthorized());

        AuthDtos.AuthResponse owner = auth.registerFirstOwner(new AuthDtos.RegisterRequest(
            "revocation.owner@example.com", "a-secure-password-123", "Revocation Owner", "Revocation"
        ));
        auth.logout(owner.token());

        mvc.perform(get("/api/v1/tasks")
                .header("Authorization", "Bearer " + owner.token()))
            .andExpect(status().isUnauthorized());
    }
}
