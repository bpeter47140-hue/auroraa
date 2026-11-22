package com.aurora.agents;

import com.aurora.security.WorkspaceContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai-usage")
public class AiUsageController {
    private final AiUsageService usage;

    public AiUsageController(AiUsageService usage) { this.usage = usage; }

    @GetMapping
    public AiUsageService.UsageSummary current() {
        WorkspaceContextHolder.requireAnyRole(
            com.aurora.identity.WorkspaceRole.OWNER,
            com.aurora.identity.WorkspaceRole.ADMIN,
            com.aurora.identity.WorkspaceRole.MEMBER,
            com.aurora.identity.WorkspaceRole.VIEWER
        );
        return usage.currentUsage();
    }
}
