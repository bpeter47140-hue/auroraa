package com.aurora.projects;

import com.aurora.agents.AgentExecutionService;
import com.aurora.agents.AgentRun;
import com.aurora.agents.AgentRunRepository;
import com.aurora.agents.AiProviderUsageRepository;
import com.aurora.audit.AuditService;
import com.aurora.identity.WorkspaceRole;
import com.aurora.security.WorkspaceContextHolder;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;

@Service
public class ProjectWorkflowService {
    private final VentureProjectRepository projects;
    private final ProjectWorkflowRunRepository workflows;
    private final ProjectWorkflowStepRepository steps;
    private final AgentExecutionService agents;
    private final AuditService audit;
    private final ProjectWorkspaceService workspaceService;
    private final ProjectPreviewService previews;
    private final ProjectBrowserQAService browserQA;
    private final AiProviderUsageRepository providerUsages;
    private final AgentRunRepository agentRuns;
    private final java.util.concurrent.ConcurrentMap<Long, String> activeWorkerLeases = new java.util.concurrent.ConcurrentHashMap<>();

    private record Stage(String agent, String title, String instructions) {}
    public ProjectWorkflowService(VentureProjectRepository projects, ProjectWorkflowRunRepository workflows,
        ProjectWorkflowStepRepository steps,
        AgentExecutionService agents, AuditService audit, ProjectWorkspaceService workspaceService,
        ProjectPreviewService previews, ProjectBrowserQAService browserQA, AiProviderUsageRepository providerUsages, AgentRunRepository agentRuns) {
        this.projects = projects; this.workflows = workflows; this.steps = steps;
        this.agents = agents; this.audit = audit; this.workspaceService = workspaceService; this.previews = previews; this.browserQA=browserQA;
        this.providerUsages = providerUsages; this.agentRuns = agentRuns;
    }

    public List<String> availableTemplates() {
        return List.of("website-planning", "website-build", "marketing-plan", "business-factory");
    }

    @Transactional
    public ProjectWorkflowRun create(Long projectId, String template) {
        Long workspaceId = WorkspaceContextHolder.id();
        VentureProject project = projects.findByIdAndWorkspace_Id(projectId, workspaceId)
            .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND, "Project not found."));
        if (java.util.Set.of("COMPLETED", "ARCHIVED", "PAUSED").contains(project.getStatus()))
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.CONFLICT, "Resume or reactivate this project before starting a workflow.");
        List<Stage> stages = stagesFor(template);
        ProjectWorkflowRun run = workflows.save(new ProjectWorkflowRun(WorkspaceContextHolder.require(), project, template));
        int order = 1;
        for (Stage stage : stages) {
            ProjectWorkflowStep step = new ProjectWorkflowStep(run, order, stage.agent(), stage.title(), stage.instructions());
            if (order > 1) step.setDependencies(List.of(order - 1));
            order++;
            if (java.util.Set.of("website-build", "business-factory").contains(template) && "developer-agent".equals(stage.agent())) step.requireApproval();
            steps.save(step);
        }
        audit.record("PROJECT_WORKFLOW_CREATED", "PROJECT_WORKFLOW", run.getId().toString(),
            "Queued " + template + " workflow for project " + project.getName());
        return run;
    }

    private List<Stage> stagesFor(String template) {
        if ("business-factory".equals(template)) return List.of(
            new Stage("research-agent", "Market, customer & competitor research",
                "Investigate the target problem, customer segments, current alternatives, competitors, market signals and relevant constraints using live sources. Cite direct URLs, publisher and dates. Separate verified evidence, third-party claims and assumptions. Do not invent interviews, surveys, demand, statistics or customer commitments. Finish with the most important unknowns and low-cost validation experiments."),
            new Stage("strategy-agent", "Business model & opportunity assessment",
                "Evaluate the opportunity using the research handoff. Define target customer, problem, value proposition, differentiation, business model, plausible revenue streams, cost drivers, pricing hypotheses, risks and measurable success criteria. Make assumptions explicit; do not present speculative financial projections as facts. Recommend BUILD, VALIDATE FURTHER, PIVOT or STOP and explain the evidence behind that recommendation. Include a lean validation plan before meaningful spending."),
            new Stage("content-agent", "Brand identity & product experience brief",
                "Create a practical brand and product brief: positioning, audience, naming directions, voice, visual direction, navigation, page-by-page copy, calls to action and trust signals. Flag claims that require proof. The output is a brief and draft copy, not a generated logo, image or video."),
            new Stage("developer-agent", "Build the approved MVP website",
                "Build a focused, usable MVP website for the proposed business using Aurora's restricted React/TypeScript scaffold. Follow the product and brand briefs. Prioritise the core customer journey, responsive layout, semantic accessible UI, honest copy and functional local interactions. Do not add dependencies, analytics, tracking, external requests, credential collection, payment processing or fake testimonials. The source will be built in an isolated preview container. It must not be deployed or published."),
            new Stage("preview-builder", "Build isolated product preview",
                "Build the generated frontend with Aurora's isolated, network-disabled preview builder. Return build evidence, logs and the short-lived private preview link. A successful build is not production deployment or launch approval."),
            new Stage("qa-agent", "Product quality & security source review",
                "Review the generated source against the research, product and brand briefs. Inspect user journeys, functional controls, navigation, forms, responsive styles, accessibility, error/empty states, content accuracy, privacy risks and obvious security issues. Give prioritised findings with severity, file and line evidence, user impact, remediation and verification steps. Distinguish verified evidence from assumptions. Do not edit source or claim to have run browser tests."),
            new Stage("browser-qa", "Automated browser & responsive QA",
                "Run Aurora's isolated Chromium QA runner against the successfully built preview. Report actual desktop/mobile page-load results, screenshots, console/runtime errors, failed requests, internal-link status, responsive overflow and accessibility/interaction inventory. Do not submit forms or click controls that could cause side effects. Clearly state runner limitations; this is not a penetration test or a complete accessibility audit."),
            new Stage("content-agent", "Launch marketing & campaign handoff pack",
                "Create a practical launch marketing pack based on the validated business and product context: audience segments, channel strategy, campaign concepts, a 2-week content calendar, channel-specific post copy, email draft, calls to action, short-form video scripts and creative briefs. Include suggested tracking parameters and measurable goals. Do not claim image/video files were rendered, do not fabricate endorsements or performance results, and do not publish or contact anyone. Mark all items as drafts for human review."),
            new Stage("operations-agent", "Business operations & launch-readiness runbook",
                "Prepare the operating plan for a small first launch: customer support process, fulfilment/service delivery checklist, privacy and data-handling questions, analytics and KPI definitions, campaign handoff steps, costs to verify, incident/rollback plan, customer feedback loop and a prioritised 30-day task plan. Review all prior outputs and QA findings. End with a clear launch-readiness verdict and a checklist of decisions requiring owner approval. Do not deploy, spend, publish, contact customers or make legal/financial commitments.")
        );
        if ("website-planning".equals(template)) return List.of(
            new Stage("research-agent", "Market & user research",
                "Research this project idea, customer needs, competitors and evidence. Use live search for current claims. Clearly cite sources, dates and uncertainties. Do not invent user interviews or market statistics."),
            new Stage("strategy-agent", "Product strategy & requirements",
                "Based on research, produce a critical project recommendation, target users, value proposition, product requirements, user stories, MVP scope, success metrics, risks and validation criteria. Challenge unsupported assumptions."),
            new Stage("content-agent", "Brand messaging & website copy",
                "Draft brand positioning, tone, page structure, homepage copy, calls to action and an initial content outline. Avoid unsupported promises; label claims requiring verification."),
            new Stage("operations-agent", "Build & launch plan",
                "Create a practical, phased plan for designing, developing, testing, previewing and launching this project. Include deliverables, dependencies, tests, risks, approval gates and next tasks. Do not claim code has been built or deployed")
        );
        if ("website-build".equals(template)) return List.of(
            new Stage("research-agent", "Market & user research",
                "Research the target customers, their problem, competitors and current alternatives. Use live sources and cite direct URLs, publishers and dates. Separate evidence from assumptions; never invent interviews, demand or statistics."),
            new Stage("strategy-agent", "Product requirements & MVP",
                "Turn the research into a focused product brief. Define target users, core problem, value proposition, essential pages and features, user journeys, acceptance criteria, accessibility requirements, success metrics, risks and what must be validated before launch. Challenge unsupported assumptions."),
            new Stage("content-agent", "Brand & website content",
                "Create the brand positioning, audience-specific messaging, navigation, page-by-page copy, calls to action and visual direction. Flag every factual claim that needs verification. This is a content brief, not a rendered design."),
            new Stage("developer-agent", "Generate website source",
                "Build the website from the preceding research, product requirements and brand/content brief. Implement the agreed MVP, not unrelated features. Use the existing restricted React/TypeScript scaffold. Do not add dependencies, external requests, tracking, credential collection or fake claims. The source will be built in an isolated preview container and will not be deployed."),
            new Stage("preview-builder", "Build isolated website preview",
                "Build the generated frontend using Aurora's isolated, network-disabled preview builder. Return the build result and a short-lived preview link. A successful preview is not a production deployment or launch approval."),
            new Stage("qa-agent", "Source-level website QA review",
                "Review the generated website source against the validated brief and successful build evidence. Inspect actual files for user journeys, functional controls, navigation, forms, responsive CSS, accessibility, loading/empty/error states, content accuracy, and obvious runtime risks. Report concrete findings with severity, file/line/snippet, user impact, proposed correction, and test steps. Separate verified source/build evidence from assumptions. You have NOT run a browser, clicked controls, tested mobile rendering, or verified deployed URLs; explicitly mark those as NOT TESTED and list manual/runtime checks. Produce a prioritised defect list and verdict BLOCK, FIX BEFORE RELEASE, or READY FOR HUMAN REVIEW. Do not edit source."),
            new Stage("browser-qa", "Automated Chromium browser QA",
                "Run the isolated Chromium browser QA runner against the successfully built preview. This stage opens the real preview at desktop and mobile viewport sizes, records screenshots, checks page load, internal links, console/runtime and HTTP errors, responsive overflow, and inventories accessibility/interaction concerns. Report exact observed results, screenshots, issues and severity. The runner does not submit forms or click potentially destructive controls; disclose those limitations. Do not modify source."),
            new Stage("operations-agent", "Launch readiness & validation plan",
                "Review the research, product brief, generated website and build result. Create a prioritised human validation and launch-readiness checklist, including remaining content, legal/privacy checks, QA/security findings, analytics consent, domain/hosting and owner approval. Do not publish or deploy." )
        );
        if ("marketing-plan".equals(template)) return List.of(
            new Stage("research-agent", "Audience & channel research",
                "Research target audience, alternatives, channel fit and examples using live sources. Cite dates and sources. Do not invent performance statistics"),
            new Stage("strategy-agent", "Marketing strategy",
                "Build a marketing plan with audience segments, positioning, objectives, channel selection, budget assumptions, KPIs, experiment plan and risks. Separate evidence from assumptions"),
            new Stage("content-agent", "Campaign creative drafts",
                "Produce campaign concepts, a content calendar, copy drafts, short-video scripts, creative briefs, CTAs and channel-specific variations. Do not publish or imply assets/videos are rendered"),
            new Stage("operations-agent", "Campaign execution checklist",
                "Create a checklist covering asset production, review, approvals, scheduling, publishing, tracking and post-campaign analysis. Public publishing and spend require owner approval")
        );
        throw new IllegalArgumentException("Unknown workflow template. Available templates: website-planning, website-build, marketing-plan");
    }

    @Async("auroraWorkflowExecutor")
    public void executeAsync(Long workflowId) {
        // Atomic database compare-and-set prevents duplicate dispatches from claiming the same run.
        String workerToken = java.util.UUID.randomUUID().toString();
        if (workflows.claimQueuedWithLease(workflowId, workerToken, java.time.Instant.now().plusSeconds(90)) != 1) return;
        activeWorkerLeases.put(workflowId, workerToken);
        ProjectWorkflowRun run = workflows.findWithWorkspaceAndProjectById(workflowId).orElse(null);
        if (run == null || !"RUNNING".equals(run.getStatus())) return;
        WorkspaceContextHolder.set(run.getWorkspace(), WorkspaceRole.MEMBER);
        ProjectWorkflowStep activeStep = null;
        try {
            List<ProjectWorkflowStep> orderedSteps = steps.findAllByWorkflowRun_IdOrderByStepOrderAsc(workflowId);
            StringBuilder priorOutputs = new StringBuilder();
            for (ProjectWorkflowStep completed : orderedSteps) {
                if ("SUCCEEDED".equals(completed.getStatus()) && completed.getOutput() != null) {
                    priorOutputs.append("\n\n### ").append(completed.getTitle()).append("\n").append(completed.getOutput());
                }
            }
            String handoff = priorOutputs.length() > 24000 ? priorOutputs.substring(priorOutputs.length() - 24000) : priorOutputs.toString();
            for (ProjectWorkflowStep step : orderedSteps) {
                // Dependency graph: a stage is runnable only after all declared prerequisites succeed.
                for (Integer prerequisiteOrder : step.getDependsOnStepOrders()) {
                    ProjectWorkflowStep prerequisite = orderedSteps.stream().filter(candidate -> candidate.getStepOrder().equals(prerequisiteOrder)).findFirst().orElseThrow(() -> new IllegalStateException("Workflow dependency is missing."));
                    if (!"SUCCEEDED".equals(prerequisite.getStatus())) throw new IllegalStateException("Stage dependency "+prerequisite.getTitle()+" is not completed; refusing to run "+step.getTitle()+".");
                }
                if (workflows.renewLease(workflowId, workerToken, java.time.Instant.now().plusSeconds(90)) != 1) throw new IllegalStateException("Workflow worker lease expired or ownership changed.");
                // Resume only pending stages. Completed AI calls are never repeated.
                if (!"PENDING".equals(step.getStatus())) continue;
                // Reload status to honour a cancellation or pause request between stages.
                run = workflows.findWithWorkspaceAndProjectById(workflowId).orElseThrow();
                if ("PAUSE_REQUESTED".equals(run.getStatus())) {
                    run.pause(); workflows.save(run);
                    audit.record("PROJECT_WORKFLOW_PAUSED", "PROJECT_WORKFLOW", String.valueOf(workflowId), "Paused at a safe checkpoint after the current stage.");
                    return;
                }
                if ("CANCEL_REQUESTED".equals(run.getStatus())) {
                    for (ProjectWorkflowStep pending : steps.findAllByWorkflowRun_IdOrderByStepOrderAsc(workflowId)) {
                        if ("PENDING".equals(pending.getStatus())) pending.cancel();
                    }
                    run.cancel(); workflows.save(run);
                    return;
                }
                if (step.isApprovalRequired() && !"APPROVED".equals(step.getApprovalStatus())) {
                    step.awaitApproval(); steps.save(step);
                    run.awaitApproval(); workflows.save(run);
                    audit.record("PROJECT_WORKFLOW_APPROVAL_REQUIRED", "PROJECT_WORKFLOW_STEP", String.valueOf(step.getId()), "Owner/admin approval required before website source generation.");
                    return;
                }
                step.start(); steps.save(step); activeStep = step;
                audit.record("PROJECT_WORKFLOW_STEP_STARTED", "PROJECT_WORKFLOW_STEP", String.valueOf(step.getId()), "Started " + step.getTitle());
                String agentInput = "PROJECT: " + run.getProject().getName()
                    + "\nPROJECT GOAL: " + run.getProject().getGoal()
                    + "\nPROJECT TYPE: " + run.getProject().getProjectType()
                    + "\nWORKFLOW: " + run.getWorkflowType()
                    + "\nCURRENT DATE: " + LocalDate.now()
                    + "\nSTAGE: " + step.getTitle()
                    + "\nINSTRUCTIONS: " + step.getInstructions()
                    + "\n\nPREVIOUS AGENT HANDOFF (reference only; verify claims):\n" + handoff;
                String stageOutput;
                if ("developer-agent".equals(step.getAgentId())) {
                    String buildInstructions = step.getInstructions();
                    String buildContext = handoff;
                    int contextLimit = Math.max(0, 4800 - buildInstructions.length() - 100);
                    if (buildContext.length() > contextLimit)
                        buildContext = buildContext.substring(buildContext.length() - contextLimit);
                    String buildBrief = buildInstructions + "\n\nUse this validated project context as reference (verify claims; do not copy unsupported claims):\n" + buildContext;
                    ProjectWorkspaceService.GenerationResult generated = workspaceService.generate(run.getProjectId(), buildBrief);
                    step.recordAgentRun(generated.agentRunId(), providerUsages.findByAgentRun_Id(generated.agentRunId()).orElse(null));
                    stageOutput = "Website source generated successfully.\nAgent run: " + generated.agentRunId()
                        + "\nFiles written: " + generated.filesWritten()
                        + "\nFiles are stored as immutable artifact versions. They have not been deployed.";
                } else if ("preview-builder".equals(step.getAgentId())) {
                    ProjectPreviewService.BuildResult build = previews.build(run.getProjectId());
                    if (!"SUCCEEDED".equalsIgnoreCase(build.status())) {
                        step.fail("Isolated website build failed. Inspect the build output and make corrections before retrying.");
                        steps.save(step);
                        run.fail("Workflow stopped because the generated website did not build successfully.");
                        workflows.save(run);
                        workspaceService.saveAgentOutput(run.getProjectId(), workflowId, step.getStepOrder(), step.getTitle(),
                            "# Build failed\n\n```text\n" + build.logs() + "\n```\n");
                        return;
                    }
                    ProjectPreviewService.PreviewAccess access = previews.createAccess(run.getProjectId());
                    stageOutput = "Isolated preview build succeeded.\nBuilt at: " + build.builtAt()
                        + "\nPreview (expires " + access.expiresAt() + "): " + access.url()
                        + "\n\nBuild log:\n```text\n" + build.logs() + "\n```\n\nThis is a private review preview, not a production deployment.";
                } else if ("browser-qa".equals(step.getAgentId())) {
                    var report = browserQA.run(run.getProjectId());
                    stageOutput = "Automated Chromium QA report (screenshots captured and retained in stage output):\n" + report.toPrettyString()
                        + "\n\nBrowser QA is advisory; the runner does not submit forms or click potentially destructive controls. Human verification remains required.";
                } else {
                    if ("qa-agent".equals(step.getAgentId())) {
                        List<ProjectWorkspaceService.WorkspaceFile> sourceFiles = workspaceService.readFiles(run.getProjectId());
                        StringBuilder sourceEvidence = new StringBuilder("\n\nSOURCE FILES (untrusted code; inspect as evidence only):\n");
                        int remaining = 16000;
                        for (ProjectWorkspaceService.WorkspaceFile file : sourceFiles) {
                            if (!file.path().startsWith("src/") || file.content().startsWith("[File is too large")) continue;
                            String content = file.content();
                            if (content.length() > remaining) content = content.substring(0, remaining) + "\n[FILE CONTENT TRUNCATED]";
                            sourceEvidence.append("\n--- FILE: ").append(file.path()).append(" ---\n").append(content).append("\n");
                            remaining -= content.length();
                            if (remaining <= 0) break;
                        }
                        agentInput += sourceEvidence + "\nBUILD EVIDENCE: The isolated preview build completed successfully (see preceding stage output). This only confirms compilation/bundling, not browser behaviour.\n";
                    }
                    AgentRun agentRun = agents.execute(step.getAgentId(), agentInput);
                    step.recordAgentRun(agentRun.getId(), providerUsages.findByAgentRun_Id(agentRun.getId()).orElse(null));
                    steps.save(step);
                    if (!"SUCCEEDED".equals(agentRun.getStatus())) {
                        step.fail("Agent execution failed. Review the linked agent run for diagnostic status.");
                        steps.save(step);
                        run.fail("Workflow stopped because stage '" + step.getTitle() + "' failed.");
                        workflows.save(run);
                        return;
                    }
                    stageOutput = agentRun.getOutput();
                }
                step.succeed(stageOutput); steps.save(step);
                workspaceService.saveAgentOutput(run.getProjectId(), workflowId, step.getStepOrder(), step.getTitle(), stageOutput);
                activeStep = null;
                run = workflows.findWithWorkspaceAndProjectById(workflowId).orElseThrow();
                if ("PAUSE_REQUESTED".equals(run.getStatus())) {
                    run.pause(); workflows.save(run);
                    audit.record("PROJECT_WORKFLOW_PAUSED", "PROJECT_WORKFLOW", String.valueOf(workflowId), "Paused at a safe checkpoint after stage "+step.getTitle()+". No stage was repeated.");
                    return;
                }
                audit.record("PROJECT_WORKFLOW_STEP_COMPLETED", "PROJECT_WORKFLOW_STEP", String.valueOf(step.getId()), "Completed " + step.getTitle());
                handoff += "\n\n### " + step.getTitle() + "\n" + stageOutput;
                if (handoff.length() > 24000) handoff = handoff.substring(handoff.length() - 24000);
            }
            run = workflows.findWithWorkspaceAndProjectById(workflowId).orElseThrow();
            if ("CANCEL_REQUESTED".equals(run.getStatus())) { run.cancel(); workflows.save(run); return; }
            if ("PAUSE_REQUESTED".equals(run.getStatus())) { run.pause(); workflows.save(run); audit.record("PROJECT_WORKFLOW_PAUSED", "PROJECT_WORKFLOW", String.valueOf(workflowId), "Paused after the final stage checkpoint."); return; }
            run.complete(); workflows.save(run);
            audit.record("PROJECT_WORKFLOW_COMPLETED", "PROJECT_WORKFLOW", String.valueOf(workflowId),
                "Workflow completed for project " + run.getProject().getName());
        } catch (Exception e) {
            if (activeStep != null && "RUNNING".equals(activeStep.getStatus())) {
                activeStep.fail("Workflow stage interrupted. Review diagnostics; the workflow will not auto-repeat this AI request.");
                steps.save(activeStep);
            }
            ProjectWorkflowRun latest = workflows.findWithWorkspaceAndProjectById(workflowId).orElse(null);
            if (latest != null && !"CANCELLED".equals(latest.getStatus())) {
                latest.fail("Workflow execution stopped due to an internal error.");
                workflows.save(latest);
            }
        } finally {
            activeWorkerLeases.remove(workflowId, workerToken);
            WorkspaceContextHolder.clear();
        }
    }

    @org.springframework.scheduling.annotation.Scheduled(fixedDelay = 20000)
    public void renewActiveWorkerLeases() {
        activeWorkerLeases.forEach((workflowId, workerToken) -> {
            int renewed = workflows.renewLease(workflowId, workerToken, java.time.Instant.now().plusSeconds(90));
            if (renewed != 1) activeWorkerLeases.remove(workflowId, workerToken);
        });
    }

    @Transactional
    public ProjectWorkflowRun retryFailedStep(Long workflowId, Long stepId) {
        WorkspaceContextHolder.requireAnyRole(WorkspaceRole.OWNER, WorkspaceRole.ADMIN);
        Long workspaceId = WorkspaceContextHolder.id();
        ProjectWorkflowRun run = workflows.findByIdAndWorkspace_Id(workflowId, workspaceId)
            .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Workflow not found."));
        ProjectWorkflowStep step = steps.findByIdAndWorkflowRun_Id(stepId, workflowId)
            .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Workflow step not found."));
        if (!"FAILED".equals(run.getStatus()) || !"FAILED".equals(step.getStatus()))
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT, "Only a failed workflow and failed step can be retried.");
        if (step.getRetryCount() >= 1)
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT, "This step has already used its one manual retry.");
        if (!"preview-builder".equals(step.getAgentId())) {
            Long priorRunId = step.getAgentRunId();
            if (priorRunId == null)
                throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,
                    "Retry blocked: Aurora cannot prove the previous AI request outcome. Review Agent Runs and usage before starting new work.");
            AgentRun priorRun = agentRuns.findByIdAndWorkspace_Id(priorRunId, workspaceId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT, "Retry blocked: linked AI run is unavailable."));
            if (!"FAILED".equals(priorRun.getStatus()))
                throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT, "Retry blocked: linked AI run is not confirmed failed.");
            var usage = providerUsages.findByAgentRun_Id(priorRunId).orElse(null);
            if (usage == null || !("REPORTED".equals(usage.getStatus()) || "MANUALLY_RECONCILED".equals(usage.getStatus())))
                throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,
                    "Retry blocked: provider usage/cost for the previous request is not confirmed. Reconcile it before retrying to avoid duplicate unknown spend.");
        }
        if (workflows.requeueFailedForRetry(workflowId, workspaceId) != 1)
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT, "Another request already changed this workflow. Refresh and try again.");
        try { step.prepareRetry(); }
        catch (IllegalStateException ex) { throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT, ex.getMessage()); }
        steps.save(step);
        audit.record("PROJECT_WORKFLOW_STEP_RETRY_QUEUED", "PROJECT_WORKFLOW_STEP", String.valueOf(stepId),
            "Owner/admin queued the one permitted retry. The previous AI run and provider usage remain in execution history.");
        return workflows.findByIdAndWorkspace_Id(workflowId, workspaceId).orElseThrow();
    }

    @Transactional
    public ProjectWorkflowRun approveStep(Long workflowId, Long stepId) {
        WorkspaceContextHolder.requireAnyRole(WorkspaceRole.OWNER, WorkspaceRole.ADMIN);
        ProjectWorkflowRun run = workflows.findByIdAndWorkspace_Id(workflowId, WorkspaceContextHolder.id())
            .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Workflow not found."));
        ProjectWorkflowStep step = steps.findByIdAndWorkflowRun_Id(stepId, workflowId)
            .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Workflow step not found."));
        try { step.approve(); run.approvalGranted(); }
        catch (IllegalStateException e) { throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT, e.getMessage()); }
        steps.save(step);
        ProjectWorkflowRun saved = workflows.save(run);
        audit.record("PROJECT_WORKFLOW_STEP_APPROVED", "PROJECT_WORKFLOW_STEP", String.valueOf(stepId), "Owner/admin approved website source generation.");
        return saved;
    }

    @Transactional
    public ProjectWorkflowRun requestPause(Long id) {
        ProjectWorkflowRun run = workflows.findByIdAndWorkspace_Id(id, WorkspaceContextHolder.id())
            .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Workflow not found."));
        if ("QUEUED".equals(run.getStatus())) run.pause();
        else if ("RUNNING".equals(run.getStatus())) run.requestPause();
        else throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT, "Workflow is not running or queued and cannot be paused.");
        audit.record("PROJECT_WORKFLOW_PAUSE_REQUESTED", "PROJECT_WORKFLOW", String.valueOf(id), "Pause requested; an active AI stage will finish before the workflow pauses.");
        return workflows.save(run);
    }

    @Transactional
    public ProjectWorkflowRun resume(Long id) {
        ProjectWorkflowRun run = workflows.findByIdAndWorkspace_Id(id, WorkspaceContextHolder.id())
            .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Workflow not found."));
        try { run.resume(); } catch (IllegalStateException e) { throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT, e.getMessage()); }
        ProjectWorkflowRun saved = workflows.save(run);
        audit.record("PROJECT_WORKFLOW_RESUMED", "PROJECT_WORKFLOW", String.valueOf(id), "Resumed from the last completed checkpoint; completed stages will not be repeated.");
        return saved;
    }

    @Transactional
    public ProjectWorkflowRun requestCancel(Long id) {
        ProjectWorkflowRun run = workflows.findByIdAndWorkspace_Id(id, WorkspaceContextHolder.id())
            .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND, "Workflow not found."));
        if ("QUEUED".equals(run.getStatus()) || "PAUSED".equals(run.getStatus()) || "AWAITING_APPROVAL".equals(run.getStatus())) {
            run.cancel();
            for (ProjectWorkflowStep step : steps.findAllByWorkflowRun_IdOrderByStepOrderAsc(id)) step.cancel();
        } else if ("RUNNING".equals(run.getStatus())) run.requestCancel();
        else throw new org.springframework.web.server.ResponseStatusException(
            org.springframework.http.HttpStatus.CONFLICT, "Workflow is no longer cancellable.");
        return workflows.save(run);
    }
}
