package com.aurora;

import com.aurora.identity.AuthDtos;
import com.aurora.identity.IdentityAuthService;
import com.aurora.projects.VentureProject;
import com.aurora.projects.VentureProjectRepository;
import com.aurora.projects.ProjectWorkflowRun;
import com.aurora.projects.ProjectWorkflowRunRepository;
import com.aurora.projects.ProjectWorkflowStep;
import com.aurora.projects.ProjectWorkflowStepRepository;
import com.aurora.projects.ProjectWorkflowService;
import com.aurora.identity.WorkspaceRole;
import com.aurora.security.WorkspaceContextHolder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import java.io.IOException;
import java.util.Comparator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {"aurora.security.sessions.enabled=true", "aurora.projects.storage-path=./target/test-project-workspaces", "aurora.preview.signing-secret=integration-test-preview-signing-secret-with-at-least-32-bytes"})
class ProjectWorkspaceIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired IdentityAuthService auth;
    @Autowired com.aurora.identity.TenantWorkspaceRepository tenantWorkspaces;
    @Autowired VentureProjectRepository ventureProjects;
    @Autowired ProjectWorkflowRunRepository workflowRuns;
    @Autowired ProjectWorkflowStepRepository workflowSteps;
    @Autowired ProjectWorkflowService workflowService;

    @BeforeEach
    void cleanProjectWorkspaceFiles() throws IOException {
        Path root = Paths.get("./target/test-project-workspaces").toAbsolutePath().normalize();
        if (Files.exists(root)) {
            try (var paths = Files.walk(root)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
            }
        }
    }

    @Test
    void onlyOneWorkerCanClaimAQueuedWorkflow() {
        AuthDtos.AuthResponse owner = auth.register(new AuthDtos.RegisterRequest(
            "workflow.claim@example.com", "a-secure-password-123", "Workflow Claim", "Claim workspace"));
        var workspace = tenantWorkspaces.findById(owner.workspaces().getFirst().id()).orElseThrow();
        VentureProject project = new VentureProject("Claim test", "Verify atomic queue claiming", "GENERAL");
        project.assignWorkspace(workspace);
        project = ventureProjects.saveAndFlush(project);
        ProjectWorkflowRun run = workflowRuns.saveAndFlush(new ProjectWorkflowRun(workspace, project, "website-planning"));

        assertEquals(1, workflowRuns.claimQueued(run.getId()));
        assertEquals(0, workflowRuns.claimQueued(run.getId()));
        ProjectWorkflowRun claimed = workflowRuns.findById(run.getId()).orElseThrow();
        assertEquals("RUNNING", claimed.getStatus());
        assertNotNull(claimed.getStartedAt());
    }

    @Test
    void failedWorkflowCanBeRequeuedForOnlyOneRetryClaim() {
        AuthDtos.AuthResponse owner = auth.register(new AuthDtos.RegisterRequest(
            "workflow.retry@example.com", "a-secure-password-123", "Workflow Retry", "Retry workspace"));
        var workspace = tenantWorkspaces.findById(owner.workspaces().getFirst().id()).orElseThrow();
        VentureProject project = new VentureProject("Retry test", "Verify safe retry claim", "GENERAL");
        project.assignWorkspace(workspace);
        project = ventureProjects.saveAndFlush(project);
        ProjectWorkflowRun run = new ProjectWorkflowRun(workspace, project, "website-planning");
        run.fail("Test failure");
        run = workflowRuns.saveAndFlush(run);
        assertEquals(1, workflowRuns.requeueFailedForRetry(run.getId(), workspace.getId()));
        assertEquals(0, workflowRuns.requeueFailedForRetry(run.getId(), workspace.getId()));
        assertEquals("QUEUED", workflowRuns.findById(run.getId()).orElseThrow().getStatus());
    }

    @Test
    void retryIsBlockedWhenPreviousAiRequestOutcomeCannotBeProven() {
        AuthDtos.AuthResponse owner = auth.register(new AuthDtos.RegisterRequest(
            "workflow.retryguard@example.com", "a-secure-password-123", "Workflow Retry Guard", "Retry Guard workspace"));
        var workspace = tenantWorkspaces.findById(owner.workspaces().getFirst().id()).orElseThrow();
        VentureProject project = new VentureProject("Retry guard test", "Do not duplicate unknown provider spend", "GENERAL");
        project.assignWorkspace(workspace);
        project = ventureProjects.saveAndFlush(project);
        ProjectWorkflowRun run = new ProjectWorkflowRun(workspace, project, "website-planning");
        run.fail("Test failure");
        run = workflowRuns.saveAndFlush(run);
        ProjectWorkflowStep step = new ProjectWorkflowStep(run, 1, "research-agent", "Research", "Research");
        step.fail("Unknown provider outcome");
        step = workflowSteps.saveAndFlush(step);
        WorkspaceContextHolder.set(workspace, WorkspaceRole.OWNER);
        try {
            final Long workflowId = run.getId();
            final Long stepId = step.getId();
            assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> workflowService.retryFailedStep(workflowId, stepId));
        } finally { WorkspaceContextHolder.clear(); }
        assertEquals("FAILED", workflowRuns.findById(run.getId()).orElseThrow().getStatus());
        assertEquals("FAILED", workflowSteps.findById(step.getId()).orElseThrow().getStatus());
    }

    @Test
    void queuedWorkflowCanOnlyBeClaimedOnceAndClaimIncludesLease() {
        AuthDtos.AuthResponse owner = auth.register(new AuthDtos.RegisterRequest(
            "workflow.lease@example.com", "a-secure-password-123", "Lease Owner", "Lease workspace"));
        var workspace = tenantWorkspaces.findById(owner.workspaces().getFirst().id()).orElseThrow();
        VentureProject project = new VentureProject("Lease claim", "Test worker lease", "GENERAL");
        project.assignWorkspace(workspace); project = ventureProjects.saveAndFlush(project);
        ProjectWorkflowRun run = workflowRuns.saveAndFlush(new ProjectWorkflowRun(workspace, project, "website-planning"));
        String worker = java.util.UUID.randomUUID().toString();
        java.time.Instant expiry = java.time.Instant.now().plusSeconds(60);
        assertEquals(1, workflowRuns.claimQueuedWithLease(run.getId(), worker, expiry));
        assertEquals(0, workflowRuns.claimQueuedWithLease(run.getId(), java.util.UUID.randomUUID().toString(), expiry));
        var claimed = workflowRuns.findById(run.getId()).orElseThrow();
        assertEquals("RUNNING", claimed.getStatus());
        assertEquals(worker, claimed.getWorkerId());
        assertNotNull(claimed.getHeartbeatAt());
        assertNotNull(claimed.getLeaseExpiresAt());
    }

    @Test
    void onlyWorkspaceOwnerOrAdminCanApproveWebsiteSourceGeneration() {
        AuthDtos.AuthResponse owner = auth.register(new AuthDtos.RegisterRequest(
            "workflow.approval@example.com", "a-secure-password-123", "Workflow Approval", "Approval workspace"));
        var workspace = tenantWorkspaces.findById(owner.workspaces().getFirst().id()).orElseThrow();
        VentureProject project = new VentureProject("Approval test", "Build a reviewed website", "WEBSITE");
        project.assignWorkspace(workspace);
        project = ventureProjects.saveAndFlush(project);
        ProjectWorkflowRun run = workflowRuns.saveAndFlush(new ProjectWorkflowRun(workspace, project, "website-build"));
        ProjectWorkflowStep step = new ProjectWorkflowStep(run, 4, "developer-agent", "Generate website source", "Build the agreed MVP.");
        step.requireApproval();
        step.awaitApproval();
        workflowSteps.saveAndFlush(step);
        run.awaitApproval();
        workflowRuns.saveAndFlush(run);

        WorkspaceContextHolder.set(workspace, WorkspaceRole.MEMBER);
        try {
            assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> workflowService.approveStep(run.getId(), step.getId()));
        } finally { WorkspaceContextHolder.clear(); }

        WorkspaceContextHolder.set(workspace, WorkspaceRole.OWNER);
        try { workflowService.approveStep(run.getId(), step.getId()); }
        finally { WorkspaceContextHolder.clear(); }

        ProjectWorkflowRun approvedRun = workflowRuns.findById(run.getId()).orElseThrow();
        ProjectWorkflowStep approvedStep = workflowSteps.findById(step.getId()).orElseThrow();
        assertEquals("QUEUED", approvedRun.getStatus());
        assertEquals("PENDING", approvedStep.getStatus());
        assertEquals("APPROVED", approvedStep.getApprovalStatus());
        assertNotNull(approvedStep.getApprovalDecidedAt());
    }

    @Test
    void ownerCanCreateListAndUpdateWorkspaceProject() throws Exception {
        AuthDtos.AuthResponse owner = auth.register(new AuthDtos.RegisterRequest(
            "projects.owner@example.com", "a-secure-password-123", "Projects Owner", "Projects"));
        String token = owner.token();
        Long workspaceId = owner.workspaces().getFirst().id();

        org.springframework.test.web.servlet.MvcResult created = mvc.perform(post("/api/v1/projects")
            .header("Authorization", "Bearer " + token)
            .header("X-Aurora-Workspace", workspaceId)
            .contentType("application/json")
            .content("{\"name\":\"Coffee shop website\",\"goal\":\"Build a website to showcase the cafe and attract customers\",\"projectType\":\"WEBSITE\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name", is("Coffee shop website")))
            .andExpect(jsonPath("$.status", is("PLANNING")))
            .andExpect(jsonPath("$.projectType", is("WEBSITE")))
            .andReturn();
        Number projectIdValue = com.jayway.jsonpath.JsonPath.read(created.getResponse().getContentAsString(), "$.id");
        Long projectId = projectIdValue.longValue();

        mvc.perform(get("/api/v1/projects")
            .header("Authorization", "Bearer " + token)
            .header("X-Aurora-Workspace", workspaceId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name", is("Coffee shop website")));

        mvc.perform(get("/api/v1/projects/{projectId}/workflows/templates", projectId)
            .header("Authorization", "Bearer " + token)
            .header("X-Aurora-Workspace", workspaceId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0]", is("website-planning")))
            .andExpect(jsonPath("$", hasItem("website-build")))
            .andExpect(jsonPath("$", hasItem("marketing-plan")))
            .andExpect(jsonPath("$", hasItem("business-factory")));

        mvc.perform(patch("/api/v1/projects/{projectId}/status", projectId)
            .header("Authorization", "Bearer " + token)
            .header("X-Aurora-Workspace", workspaceId)
            .contentType("application/json")
            .content("{\"status\":\"ACTIVE\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status", is("ACTIVE")));

        org.springframework.test.web.servlet.MvcResult started = mvc.perform(post("/api/v1/projects/{projectId}/workflows", projectId)
            .header("Authorization", "Bearer " + token)
            .header("X-Aurora-Workspace", workspaceId)
            .contentType("application/json")
            .content("{\"template\":\"website-planning\"}"))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.run.workflowType", is("website-planning")))
            .andExpect(jsonPath("$.steps", hasSize(4)))
            .andExpect(jsonPath("$.steps[0].title", is("Market & user research")))
            .andReturn();
        Number workflowIdValue = com.jayway.jsonpath.JsonPath.read(started.getResponse().getContentAsString(), "$.run.id");
        Long workflowId = workflowIdValue.longValue();
        org.springframework.test.web.servlet.MvcResult latest = null;
        for (int attempt = 0; attempt < 30; attempt++) {
            latest = mvc.perform(get("/api/v1/projects/{projectId}/workflows/{workflowId}", projectId, workflowId)
                .header("Authorization", "Bearer " + token)
                .header("X-Aurora-Workspace", workspaceId)).andReturn();
            String workflowStatus = com.jayway.jsonpath.JsonPath.read(latest.getResponse().getContentAsString(), "$.run.status");
            if ("FAILED".equals(workflowStatus) || "SUCCEEDED".equals(workflowStatus)) break;
            Thread.sleep(100);
        }
        mvc.perform(get("/api/v1/projects/{projectId}/workflows/{workflowId}", projectId, workflowId)
            .header("Authorization", "Bearer " + token)
            .header("X-Aurora-Workspace", workspaceId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.run.status", is("FAILED")))
            .andExpect(jsonPath("$.steps[0].status", is("FAILED")));

        mvc.perform(post("/api/v1/projects/{projectId}/workflows", projectId)
            .header("Authorization", "Bearer " + token)
            .header("X-Aurora-Workspace", workspaceId)
            .contentType("application/json")
            .content("{\"template\":\"website-build\"}"))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.run.workflowType", is("website-build")))
            .andExpect(jsonPath("$.steps", hasSize(8)))
            .andExpect(jsonPath("$.steps[3].agentId", is("developer-agent")))
            .andExpect(jsonPath("$.steps[3].approvalRequired", is(true)))
            .andExpect(jsonPath("$.steps[3].approvalStatus", is("PENDING")))
            .andExpect(jsonPath("$.steps[4].agentId", is("preview-builder")))
            .andExpect(jsonPath("$.steps[5].agentId", is("qa-agent")))
            .andExpect(jsonPath("$.steps[5].title", is("Source-level website QA review")))
            .andExpect(jsonPath("$.steps[6].agentId", is("browser-qa")))
            .andExpect(jsonPath("$.steps[7].agentId", is("operations-agent")));
    }

    @Test
    void businessFactoryTemplateCreatesCompletePipelineAndRequiresOwnerApprovalBeforeCodeGeneration() {
        AuthDtos.AuthResponse owner = auth.register(new AuthDtos.RegisterRequest(
            "business.factory.workflow@example.com", "a-secure-password-123", "Factory Owner", "Factory workspace"));
        var workspace = tenantWorkspaces.findById(owner.workspaces().getFirst().id()).orElseThrow();
        VentureProject project = new VentureProject("Local services marketplace",
            "Validate and build a small marketplace for local service providers", "APPLICATION");
        project.assignWorkspace(workspace);
        project = ventureProjects.saveAndFlush(project);

        WorkspaceContextHolder.set(workspace, WorkspaceRole.OWNER);
        ProjectWorkflowRun run;
        try {
            run = workflowService.create(project.getId(), "business-factory");
        } finally {
            WorkspaceContextHolder.clear();
        }

        var stages = workflowSteps.findAllByWorkflowRun_IdOrderByStepOrderAsc(run.getId());
        assertEquals(9, stages.size());
        assertEquals(java.util.List.of(
            "research-agent", "strategy-agent", "content-agent", "developer-agent",
            "preview-builder", "qa-agent", "browser-qa", "content-agent", "operations-agent"),
            stages.stream().map(ProjectWorkflowStep::getAgentId).toList());
        assertTrue(stages.get(3).isApprovalRequired());
        assertEquals("PENDING", stages.get(3).getApprovalStatus());
        assertEquals(java.util.List.of(3), stages.get(3).getDependsOnStepOrders());
        assertEquals(java.util.List.of(8), stages.get(8).getDependsOnStepOrders());
        assertEquals("QUEUED", run.getStatus());
    }

    @Test
    void ownerCanGeneratePreviewFilesAndExportStarterZip() throws Exception {
        AuthDtos.AuthResponse owner = auth.register(new AuthDtos.RegisterRequest(
            "projects.scaffold@example.com", "a-secure-password-123", "Scaffold Owner", "Scaffold workspace"));
        Long workspaceId = owner.workspaces().getFirst().id();
        org.springframework.test.web.servlet.MvcResult created = mvc.perform(post("/api/v1/projects")
            .header("Authorization", "Bearer " + owner.token()).header("X-Aurora-Workspace", workspaceId)
            .contentType("application/json").content("{\"name\":\"Bakery site\",\"goal\":\"Show bakery products and opening hours\",\"projectType\":\"WEBSITE\"}"))
            .andExpect(status().isCreated()).andReturn();
        Number idValue = com.jayway.jsonpath.JsonPath.read(created.getResponse().getContentAsString(), "$.id");
        Long projectId = idValue.longValue();
        mvc.perform(post("/api/v1/projects/{projectId}/workspace/scaffold", projectId)
            .header("Authorization", "Bearer " + owner.token()).header("X-Aurora-Workspace", workspaceId))
            .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(8)));
        mvc.perform(get("/api/v1/projects/{projectId}/workspace/files", projectId)
            .header("Authorization", "Bearer " + owner.token()).header("X-Aurora-Workspace", workspaceId))
            .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(8)));
        mvc.perform(get("/api/v1/projects/{projectId}/workspace/export.zip", projectId)
            .header("Authorization", "Bearer " + owner.token()).header("X-Aurora-Workspace", workspaceId))
            .andExpect(status().isOk()).andExpect(header().string("Content-Type", "application/zip"));
    }

    @Test
    void signedPreviewUrlWorksWithoutSessionAuthenticationAndRejectsTampering() throws Exception {
        AuthDtos.AuthResponse owner = auth.register(new AuthDtos.RegisterRequest(
            "projects.preview@example.com", "a-secure-password-123", "Preview Owner", "Preview workspace"));
        Long workspaceId = owner.workspaces().getFirst().id();
        org.springframework.test.web.servlet.MvcResult created = mvc.perform(post("/api/v1/projects")
            .header("Authorization", "Bearer " + owner.token()).header("X-Aurora-Workspace", workspaceId)
            .contentType("application/json").content("{\"name\":\"Preview site\",\"goal\":\"Inspect a generated website\",\"projectType\":\"WEBSITE\"}"))
            .andExpect(status().isCreated()).andReturn();
        Long projectId = ((Number) com.jayway.jsonpath.JsonPath.read(created.getResponse().getContentAsString(), "$.id")).longValue();
        mvc.perform(post("/api/v1/projects/{projectId}/workspace/scaffold", projectId)
            .header("Authorization", "Bearer " + owner.token()).header("X-Aurora-Workspace", workspaceId))
            .andExpect(status().isOk());

        Path previewRoot = Paths.get("./target/test-project-workspaces").toAbsolutePath().normalize()
            .resolve("ws-" + workspaceId).resolve("project-" + projectId).resolve("preview");
        Files.createDirectories(previewRoot);
        Files.writeString(previewRoot.resolve("index.html"), "<!doctype html><title>Signed preview works</title>");

        org.springframework.test.web.servlet.MvcResult access = mvc.perform(post("/api/v1/projects/{projectId}/workspace/preview-access", projectId)
            .header("Authorization", "Bearer " + owner.token()).header("X-Aurora-Workspace", workspaceId))
            .andExpect(status().isOk()).andReturn();
        String previewUrl = com.jayway.jsonpath.JsonPath.read(access.getResponse().getContentAsString(), "$.url");
        mvc.perform(get(previewUrl)) // The short-lived HMAC URL is the only credential needed by the iframe.
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Signed preview works")))
            .andExpect(header().string("Content-Security-Policy", containsString("sandbox allow-scripts")))
            .andExpect(header().string("Referrer-Policy", "no-referrer"));

        String token = previewUrl.substring(previewUrl.indexOf("/preview/") + 9, previewUrl.lastIndexOf('/'));
        String tamperedUrl = previewUrl.replace(token, "a.b");
        mvc.perform(get(tamperedUrl)).andExpect(status().isForbidden());
    }

    @Test
    void ownerCanRequestReviewAndRecordAnAuditedDecision() throws Exception {
        AuthDtos.AuthResponse owner = auth.register(new AuthDtos.RegisterRequest(
            "projects.review@example.com", "a-secure-password-123", "Review Owner", "Review workspace"));
        Long workspaceId = owner.workspaces().getFirst().id();
        org.springframework.test.web.servlet.MvcResult created = mvc.perform(post("/api/v1/projects")
            .header("Authorization", "Bearer " + owner.token()).header("X-Aurora-Workspace", workspaceId)
            .contentType("application/json").content("{\"name\":\"Review website\",\"goal\":\"Build a site and inspect it before launch\",\"projectType\":\"WEBSITE\"}"))
            .andExpect(status().isCreated()).andReturn();
        Long projectId = ((Number) com.jayway.jsonpath.JsonPath.read(created.getResponse().getContentAsString(), "$.id")).longValue();
        org.springframework.test.web.servlet.MvcResult scaffold = mvc.perform(post("/api/v1/projects/{projectId}/workspace/scaffold", projectId)
            .header("Authorization", "Bearer " + owner.token()).header("X-Aurora-Workspace", workspaceId))
            .andExpect(status().isOk()).andReturn();
        Long artifactId = ((Number) com.jayway.jsonpath.JsonPath.read(scaffold.getResponse().getContentAsString(), "$[0].id")).longValue();

        org.springframework.test.web.servlet.MvcResult requested = mvc.perform(post("/api/v1/projects/{projectId}/reviews", projectId)
            .header("Authorization", "Bearer " + owner.token()).header("X-Aurora-Workspace", workspaceId)
            .contentType("application/json").content("{\"artifactId\":" + artifactId + "}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.status", is("PENDING_REVIEW"))).andReturn();
        Long reviewId = ((Number) com.jayway.jsonpath.JsonPath.read(requested.getResponse().getContentAsString(), "$.id")).longValue();

        mvc.perform(post("/api/v1/projects/{projectId}/reviews/{reviewId}/decision", projectId, reviewId)
            .header("Authorization", "Bearer " + owner.token()).header("X-Aurora-Workspace", workspaceId)
            .contentType("application/json").content("{\"decision\":\"CHANGES_REQUESTED\"}"))
            .andExpect(status().isBadRequest());

        mvc.perform(post("/api/v1/projects/{projectId}/reviews/{reviewId}/decision", projectId, reviewId)
            .header("Authorization", "Bearer " + owner.token()).header("X-Aurora-Workspace", workspaceId)
            .contentType("application/json").content("{\"decision\":\"CHANGES_REQUESTED\",\"feedback\":\"Please improve the mobile layout before approval.\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status", is("CHANGES_REQUESTED")))
            .andExpect(jsonPath("$.reviewedBy", is("projects.review@example.com")));

        mvc.perform(get("/api/v1/projects/{projectId}/reviews", projectId)
            .header("Authorization", "Bearer " + owner.token()).header("X-Aurora-Workspace", workspaceId))
            .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].artifactVersionId").isNotEmpty())
            .andExpect(jsonPath("$[0].artifactVersionNumber", is(1)))
            .andExpect(jsonPath("$[0].feedback", is("Please improve the mobile layout before approval.")));
    }

    @Test
    void manualSourceEditsCreateImmutableVersionsAndReviewPinsTheExactVersion() throws Exception {
        AuthDtos.AuthResponse owner = auth.register(new AuthDtos.RegisterRequest(
            "projects.versions@example.com", "a-secure-password-123", "Version Owner", "Version workspace"));
        Long workspaceId = owner.workspaces().getFirst().id();
        org.springframework.test.web.servlet.MvcResult created = mvc.perform(post("/api/v1/projects")
            .header("Authorization", "Bearer " + owner.token()).header("X-Aurora-Workspace", workspaceId)
            .contentType("application/json").content("{\"name\":\"Version website\",\"goal\":\"Track immutable source versions\",\"projectType\":\"WEBSITE\"}"))
            .andExpect(status().isCreated()).andReturn();
        Long projectId = ((Number) com.jayway.jsonpath.JsonPath.read(created.getResponse().getContentAsString(), "$.id")).longValue();
        org.springframework.test.web.servlet.MvcResult scaffold = mvc.perform(post("/api/v1/projects/{projectId}/workspace/scaffold", projectId)
            .header("Authorization", "Bearer " + owner.token()).header("X-Aurora-Workspace", workspaceId))
            .andExpect(status().isOk()).andReturn();
        java.util.List<java.util.Map<String,Object>> artifactRows = com.jayway.jsonpath.JsonPath.read(scaffold.getResponse().getContentAsString(), "$[*]");
        Long artifactId = ((Number) artifactRows.stream().filter(row -> "src/App.tsx".equals(row.get("relativePath"))).findFirst().orElseThrow().get("id")).longValue();
        mvc.perform(put("/api/v1/projects/{projectId}/workspace/file", projectId)
            .header("Authorization", "Bearer " + owner.token()).header("X-Aurora-Workspace", workspaceId)
            .contentType("application/json").content("{\"path\":\"src/App.tsx\",\"content\":\"export default function App(){return <main>Edited</main>}\"}"))
            .andExpect(status().isOk());
        mvc.perform(get("/api/v1/projects/{projectId}/workspace/artifacts/{artifactId}/versions", projectId, artifactId)
            .header("Authorization", "Bearer " + owner.token()).header("X-Aurora-Workspace", workspaceId))
            .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(3)))
            .andExpect(jsonPath("$[0].versionNumber", is(3)))
            .andExpect(jsonPath("$[1].versionNumber", is(2)))
            .andExpect(jsonPath("$[2].versionNumber", is(1)));
        mvc.perform(post("/api/v1/projects/{projectId}/reviews", projectId)
            .header("Authorization", "Bearer " + owner.token()).header("X-Aurora-Workspace", workspaceId)
            .contentType("application/json").content("{\"artifactId\":" + artifactId + "}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.artifactVersionNumber", is(3)))
            .andExpect(jsonPath("$.artifactVersionId").isNotEmpty());
    }

    @Test
    void projectAccessIsScopedToSelectedWorkspace() throws Exception {
        AuthDtos.AuthResponse owner = auth.register(new AuthDtos.RegisterRequest(
            "projects.isolation@example.com", "a-secure-password-123", "Projects Isolation", "Private projects"));
        mvc.perform(get("/api/v1/projects")
            .header("Authorization", "Bearer " + owner.token())
            .header("X-Aurora-Workspace", owner.workspaces().getFirst().id()))
            .andExpect(status().isOk());

        mvc.perform(get("/api/v1/projects")
            .header("Authorization", "Bearer " + owner.token())
            .header("X-Aurora-Workspace", 999999))
            .andExpect(status().isForbidden());
    }
}
