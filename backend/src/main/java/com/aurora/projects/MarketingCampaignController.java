package com.aurora.projects;

import com.aurora.identity.UserAccount;
import com.aurora.security.WorkspaceContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/campaigns")
public class MarketingCampaignController {
    private final MarketingCampaignService service;
    public MarketingCampaignController(MarketingCampaignService service) { this.service = service; }
    public record Draft(@NotBlank @Size(max=180) String title, @NotBlank String channel, @NotBlank String format,
        @NotBlank @Size(max=20000) String content, @Size(max=500) String callToAction) {}
    public record Decision(@NotBlank String decision, @Size(max=4000) String feedback) {}
    public record CopyPrompt(@NotBlank String channel, @NotBlank String format, @NotBlank @Size(max=4000) String brief) {}
    public record Schedule(@jakarta.validation.constraints.NotNull Instant scheduledAt) {}
    @GetMapping public List<MarketingCampaign> list(@PathVariable Long projectId) { return service.list(projectId); }
    @PostMapping("/generate-copy") public MarketingCampaignService.GeneratedCopy generateCopy(@PathVariable Long projectId, @Valid @RequestBody CopyPrompt body) {
        return service.generateCopy(projectId, new MarketingCampaignService.CopyRequest(body.channel(), body.format(), body.brief()));
    }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public MarketingCampaign create(@PathVariable Long projectId, @Valid @RequestBody Draft body, HttpServletRequest request) {
        return service.create(projectId, new MarketingCampaignService.DraftRequest(body.title(), body.channel(), body.format(), body.content(), body.callToAction()), actor(request));
    }
    @PutMapping("/{campaignId}") public MarketingCampaign update(@PathVariable Long projectId, @PathVariable Long campaignId, @Valid @RequestBody Draft body) {
        return service.update(projectId, campaignId, new MarketingCampaignService.DraftRequest(body.title(), body.channel(), body.format(), body.content(), body.callToAction()));
    }
    @PostMapping("/{campaignId}/submit") public MarketingCampaign submit(@PathVariable Long projectId, @PathVariable Long campaignId) { return service.submit(projectId, campaignId); }
    @PostMapping("/{campaignId}/decision") public MarketingCampaign decide(@PathVariable Long projectId, @PathVariable Long campaignId, @Valid @RequestBody Decision body, HttpServletRequest request) {
        return service.decide(projectId, campaignId, new MarketingCampaignService.DecisionRequest(body.decision(), body.feedback()), actor(request));
    }
    @PostMapping("/{campaignId}/schedule") public MarketingCampaign schedule(@PathVariable Long projectId, @PathVariable Long campaignId, @Valid @RequestBody Schedule body) {
        return service.schedule(projectId, campaignId, new MarketingCampaignService.ScheduleRequest(body.scheduledAt()));
    }
    private String actor(HttpServletRequest request) {
        Object value = request.getAttribute("aurora.authenticatedUser");
        if (!(value instanceof UserAccount user)) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required.");
        return user.getEmail();
    }
}
