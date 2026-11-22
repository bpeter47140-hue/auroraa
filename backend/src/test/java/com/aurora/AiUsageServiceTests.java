package com.aurora;

import com.aurora.agents.AiUsageService;
import com.aurora.identity.TenantWorkspace;
import com.aurora.identity.TenantWorkspaceRepository;
import com.aurora.identity.WorkspaceRole;
import com.aurora.security.WorkspaceContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "aurora.ai.monthly-request-limit=2")
class AiUsageServiceTests {
    @Autowired AiUsageService usage;
    @Autowired TenantWorkspaceRepository workspaces;

    @AfterEach
    void clearContext() { WorkspaceContextHolder.clear(); }

    @Test
    void enforcesMonthlyWorkspaceAllowanceAndReportsRemainingRequests() {
        TenantWorkspace workspace = workspaces.saveAndFlush(new TenantWorkspace("AI quota test"));
        WorkspaceContextHolder.set(workspace, WorkspaceRole.OWNER);

        usage.reserveRequest();
        usage.reserveRequest();

        AiUsageService.UsageSummary summary = usage.currentUsage();
        assertEquals(2, summary.used());
        assertEquals(2, summary.limit());
        assertEquals(0, summary.remaining());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, usage::reserveRequest);
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, exception.getStatusCode());
    }
}
