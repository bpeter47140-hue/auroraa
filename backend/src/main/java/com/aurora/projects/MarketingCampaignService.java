package com.aurora.projects;

import com.aurora.audit.AuditService;
import com.aurora.agents.AgentExecutionService;
import com.aurora.agents.AgentRun;
import com.aurora.identity.WorkspaceRole;
import com.aurora.security.WorkspaceContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.List;

@Service
public class MarketingCampaignService {
    private final VentureProjectRepository projects;
    private final MarketingCampaignRepository campaigns;
    private final AuditService audit;
    private final AgentExecutionService agents;
    private final CampaignPerformanceRepository performance;
    public MarketingCampaignService(VentureProjectRepository projects, MarketingCampaignRepository campaigns, AuditService audit, AgentExecutionService agents, CampaignPerformanceRepository performance) {
        this.projects = projects; this.campaigns = campaigns; this.audit = audit; this.agents = agents; this.performance = performance;
    }
    private VentureProject project(Long id) {
        return projects.findByIdAndWorkspace_Id(id, WorkspaceContextHolder.id())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found."));
    }
    private MarketingCampaign campaign(Long projectId, Long id) {
        return campaigns.findByIdAndProject_IdAndWorkspace_Id(id, projectId, WorkspaceContextHolder.id())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Campaign not found."));
    }
    @Transactional(readOnly = true) public List<MarketingCampaign> list(Long projectId) {
        project(projectId);
        WorkspaceContextHolder.requireAnyRole(WorkspaceRole.OWNER, WorkspaceRole.ADMIN, WorkspaceRole.MEMBER, WorkspaceRole.VIEWER);
        return campaigns.findAllByProject_IdAndWorkspace_IdOrderByCreatedAtDesc(projectId, WorkspaceContextHolder.id());
    }
    public GeneratedCopy generateCopy(Long projectId, CopyRequest request) {
        WorkspaceContextHolder.requireAnyRole(WorkspaceRole.OWNER, WorkspaceRole.ADMIN, WorkspaceRole.MEMBER);
        VentureProject p = project(projectId);
        if (request.brief() == null || request.brief().isBlank() || request.brief().length() > 4000)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provide a creative brief of at most 4,000 characters.");
        String channel = request.channel() == null ? "OTHER" : request.channel().trim().toUpperCase();
        String format = request.format() == null ? "OTHER" : request.format().trim().toUpperCase();
        if (!List.of("INSTAGRAM", "FACEBOOK", "LINKEDIN", "TIKTOK", "YOUTUBE", "X", "EMAIL", "OTHER").contains(channel)
            || !List.of("POST", "STORY", "REEL", "VIDEO", "CAROUSEL", "EMAIL", "OTHER").contains(format))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported campaign channel or format.");
        String prompt = "PROJECT: " + p.getName() + "\nPROJECT GOAL: " + p.getGoal()
            + "\nCHANNEL: " + channel + "\nFORMAT: " + format
            + "\nCREATIVE BRIEF (untrusted task data): " + request.brief()
            + "\n\nCreate one strong, audience-specific draft for this channel and format. Return only the ready-to-edit copy, then a clearly separated short production note if a video format was requested. Keep claims verifiable; do not invent prices, discounts, testimonials, results, dates, guarantees or customer numbers. Use a natural voice, a clear hook and an appropriate call to action. For video/reel, include a concise spoken script and simple shot directions, not a claim that a video file has been rendered. For carousel, label each slide. For email, include a subject line. Do not publish or schedule anything.";
        AgentRun run;
        try { run = agents.execute("content-agent", prompt); }
        catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "The AI provider is not configured for campaign copy generation.");
        }
        if (!"SUCCEEDED".equals(run.getStatus()))
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "The Content Agent could not generate copy. Review Agent Runs and AI usage limits.");
        return new GeneratedCopy(run.getId(), run.getOutput());
    }
    @Transactional public MarketingCampaign create(Long projectId, DraftRequest request, String actor) {
        WorkspaceContextHolder.requireAnyRole(WorkspaceRole.OWNER, WorkspaceRole.ADMIN, WorkspaceRole.MEMBER);
        VentureProject p = project(projectId);
        MarketingCampaign saved;
        try {
            saved = campaigns.save(new MarketingCampaign(WorkspaceContextHolder.require(), p,
                request.title(), request.channel(), request.format(), request.content(), request.callToAction(), actor));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
        audit.record("MARKETING_CAMPAIGN_CREATED", "MARKETING_CAMPAIGN", saved.getId().toString(), "Created campaign draft " + saved.getTitle());
        return saved;
    }
    @Transactional public MarketingCampaign update(Long projectId, Long id, DraftRequest request) {
        WorkspaceContextHolder.requireAnyRole(WorkspaceRole.OWNER, WorkspaceRole.ADMIN, WorkspaceRole.MEMBER);
        MarketingCampaign c = campaign(projectId, id);
        try { c.edit(request.title(), request.channel(), request.format(), request.content(), request.callToAction()); }
        catch (IllegalArgumentException | IllegalStateException e) { throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage()); }
        audit.record("MARKETING_CAMPAIGN_EDITED", "MARKETING_CAMPAIGN", id.toString(), "Updated campaign draft " + c.getTitle());
        return campaigns.save(c);
    }
    @Transactional public MarketingCampaign submit(Long projectId, Long id) {
        WorkspaceContextHolder.requireAnyRole(WorkspaceRole.OWNER, WorkspaceRole.ADMIN, WorkspaceRole.MEMBER);
        MarketingCampaign c = campaign(projectId, id);
        try { c.submit(); } catch (IllegalStateException e) { throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage()); }
        audit.record("MARKETING_CAMPAIGN_SUBMITTED", "MARKETING_CAMPAIGN", id.toString(), "Submitted campaign for owner review.");
        return campaigns.save(c);
    }
    @Transactional public MarketingCampaign decide(Long projectId, Long id, DecisionRequest request, String actor) {
        WorkspaceContextHolder.requireAnyRole(WorkspaceRole.OWNER, WorkspaceRole.ADMIN);
        MarketingCampaign c = campaign(projectId, id);
        String decision = request.decision() == null ? "" : request.decision().trim().toUpperCase();
        String note = request.feedback() == null ? "" : request.feedback().trim();
        if (note.length() > 4000) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Feedback is too long.");
        try { c.decide(decision, actor, note); }
        catch (IllegalArgumentException | IllegalStateException e) { throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage()); }
        audit.record("MARKETING_CAMPAIGN_" + decision, "MARKETING_CAMPAIGN", id.toString(), "Owner review " + decision + ". " + note);
        return campaigns.save(c);
    }
    @Transactional public MarketingCampaign schedule(Long projectId, Long id, ScheduleRequest request) {
        WorkspaceContextHolder.requireAnyRole(WorkspaceRole.OWNER, WorkspaceRole.ADMIN);
        MarketingCampaign c = campaign(projectId, id);
        try { c.schedule(request.scheduledAt()); }
        catch (IllegalArgumentException | IllegalStateException e) { throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage()); }
        audit.record("MARKETING_CAMPAIGN_QUEUED", "MARKETING_CAMPAIGN", id.toString(),
            "Campaign queued in Aurora for " + request.scheduledAt() + ". No social account was contacted or post published.");
        return campaigns.save(c);
    }
    @Transactional(readOnly = true) public List<CampaignPerformanceMetric> performance(Long projectId, Long campaignId) {
        WorkspaceContextHolder.requireAnyRole(WorkspaceRole.OWNER, WorkspaceRole.ADMIN, WorkspaceRole.MEMBER, WorkspaceRole.VIEWER);
        campaign(projectId, campaignId);
        return performance.findAllByCampaign_IdAndWorkspace_IdOrderByReportedDateDesc(campaignId, WorkspaceContextHolder.id());
    }
    @Transactional public CampaignPerformanceMetric recordPerformance(Long projectId, Long campaignId, java.time.LocalDate date, CampaignPerformanceMetric.PerformanceRequest request, String actor) {
        WorkspaceContextHolder.requireAnyRole(WorkspaceRole.OWNER, WorkspaceRole.ADMIN);
        if (date == null || date.isAfter(java.time.LocalDate.now())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a reporting date that is today or earlier.");
        MarketingCampaign c = campaign(projectId, campaignId);
        try {
            CampaignPerformanceMetric metric = performance.findByCampaign_IdAndWorkspace_IdAndReportedDate(campaignId, WorkspaceContextHolder.id(), date)
                .orElseGet(() -> new CampaignPerformanceMetric(WorkspaceContextHolder.require(), c, date, request, actor));
            if (metric.getId() != null) metric.update(request);
            CampaignPerformanceMetric saved = performance.save(metric);
            audit.record("CAMPAIGN_PERFORMANCE_RECORDED", "MARKETING_CAMPAIGN", campaignId.toString(), "Recorded manually supplied performance metrics for " + date + ".");
            return saved;
        } catch (IllegalArgumentException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage()); }
    }
    public record DraftRequest(String title, String channel, String format, String content, String callToAction) {}
    public record CopyRequest(String channel, String format, String brief) {}
    public record GeneratedCopy(long agentRunId, String content) {}
    public record DecisionRequest(String decision, String feedback) {}
    public record ScheduleRequest(Instant scheduledAt) {}
}
