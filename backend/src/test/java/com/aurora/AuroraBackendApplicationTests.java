package com.aurora;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import com.aurora.identity.TenantWorkspace;
import com.aurora.identity.TenantWorkspaceRepository;
import com.aurora.security.WorkspaceContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuroraBackendApplicationTests {
    @Autowired private MockMvc mvc;
    @Autowired private TenantWorkspaceRepository workspaces;

    @BeforeEach void setWorkspace() {
        WorkspaceContextHolder.set(workspaces.saveAndFlush(new TenantWorkspace("Integration test workspace")), com.aurora.identity.WorkspaceRole.OWNER);
    }
    @AfterEach void clearWorkspace() { WorkspaceContextHolder.clear(); }

    @Test
    void taskApprovalAndAuditWorkflowWorks() throws Exception {
        mvc.perform(post("/api/v1/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"Build the first research agent","description":"Implement and validate the first AI employee.","priority":"HIGH"}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.title").value("Build the first research agent"))
            .andExpect(jsonPath("$.status").value("PENDING"));

        mvc.perform(patch("/api/v1/tasks/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"IN_PROGRESS\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        mvc.perform(post("/api/v1/approvals")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"Enable a paid AI provider","description":"Authorize a model provider and its usage budget.","actionType":"PAID_SERVICE"}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("PENDING"));

        mvc.perform(post("/api/v1/approvals/1/approve")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"note\":\"Test owner decision\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("APPROVED"));

        mvc.perform(post("/api/v1/businesses")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"AURORA Test Workspace","description":"Isolated integration test.","goal":"Verify workspace persistence.","status":"RESEARCH"}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("RESEARCH"));

        mvc.perform(get("/api/v1/dashboard/stats"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalTasks").value(1))
            .andExpect(jsonPath("$.inProgressTasks").value(1))
            .andExpect(jsonPath("$.pendingApprovals").value(0))
            .andExpect(jsonPath("$.totalBusinesses").value(1));

        mvc.perform(post("/api/v1/agents/research/run")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"question\":\"Research a business idea.\"}"))
            .andExpect(status().isServiceUnavailable());

        mvc.perform(get("/api/v1/audit"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(5));
    }
}
