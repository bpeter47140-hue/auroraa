package com.aurora;

import com.aurora.identity.AuthDtos;
import com.aurora.identity.IdentityAuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.time.Instant;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@org.springframework.test.context.TestPropertySource(properties = {"aurora.security.sessions.enabled=true"})
class MarketingCampaignIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired IdentityAuthService auth;

    @Test
    void ownerCanDraftReviewApproveAndQueueCampaignWithoutPublishing() throws Exception {
        AuthDtos.AuthResponse owner = auth.register(new AuthDtos.RegisterRequest(
            "campaign.owner@example.com", "a-secure-password-123", "Campaign Owner", "Campaign workspace"));
        String token = owner.token();
        Long workspaceId = owner.workspaces().getFirst().id();
        var createdProject = mvc.perform(post("/api/v1/projects")
            .header("Authorization", "Bearer " + token).header("X-Aurora-Workspace", workspaceId)
            .contentType("application/json")
            .content("{\"name\":\"Campaign test\",\"goal\":\"Test safe campaign lifecycle\",\"projectType\":\"MARKETING\"}"))
            .andExpect(status().isCreated()).andReturn();
        Long projectId = ((Number) com.jayway.jsonpath.JsonPath.read(createdProject.getResponse().getContentAsString(), "$.id")).longValue();
        String draft = "{\"title\":\"Launch announcement\",\"channel\":\"INSTAGRAM\",\"format\":\"POST\",\"content\":\"A new chapter starts today.\",\"callToAction\":\"Learn more\"}";
        var created = mvc.perform(post("/api/v1/projects/{projectId}/campaigns", projectId)
            .header("Authorization", "Bearer " + token).header("X-Aurora-Workspace", workspaceId)
            .contentType("application/json").content(draft))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.status", is("DRAFT"))).andReturn();
        Long campaignId = ((Number) com.jayway.jsonpath.JsonPath.read(created.getResponse().getContentAsString(), "$.id")).longValue();
        mvc.perform(post("/api/v1/projects/{projectId}/campaigns/{campaignId}/submit", projectId, campaignId)
            .header("Authorization", "Bearer " + token).header("X-Aurora-Workspace", workspaceId))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status", is("PENDING_REVIEW")));
        mvc.perform(post("/api/v1/projects/{projectId}/campaigns/{campaignId}/schedule", projectId, campaignId)
            .header("Authorization", "Bearer " + token).header("X-Aurora-Workspace", workspaceId)
            .contentType("application/json").content("{\"scheduledAt\":\"" + Instant.now().plusSeconds(3600) + "\"}"))
            .andExpect(status().isConflict());
        mvc.perform(post("/api/v1/projects/{projectId}/campaigns/{campaignId}/decision", projectId, campaignId)
            .header("Authorization", "Bearer " + token).header("X-Aurora-Workspace", workspaceId)
            .contentType("application/json").content("{\"decision\":\"APPROVED\",\"feedback\":\"\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status", is("APPROVED")));
        mvc.perform(post("/api/v1/projects/{projectId}/campaigns/{campaignId}/schedule", projectId, campaignId)
            .header("Authorization", "Bearer " + token).header("X-Aurora-Workspace", workspaceId)
            .contentType("application/json").content("{\"scheduledAt\":\"" + Instant.now().plusSeconds(3600) + "\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status", is("SCHEDULED")));
        mvc.perform(get("/api/v1/projects/{projectId}/campaigns", projectId)
            .header("Authorization", "Bearer " + token).header("X-Aurora-Workspace", workspaceId))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].status", is("SCHEDULED")));
    }
}
