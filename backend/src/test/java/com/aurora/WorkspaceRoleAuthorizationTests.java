package com.aurora;

import com.aurora.identity.TenantWorkspace;
import com.aurora.identity.WorkspaceRole;
import com.aurora.security.WorkspaceContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkspaceRoleAuthorizationTests {
    @AfterEach void clearContext() { WorkspaceContextHolder.clear(); }

    @Test void memberCanPerformMemberActionsButNotAdminActions() {
        WorkspaceContextHolder.set(new TenantWorkspace("Member test"), WorkspaceRole.MEMBER);
        WorkspaceContextHolder.requireAnyRole(WorkspaceRole.OWNER, WorkspaceRole.ADMIN, WorkspaceRole.MEMBER);
        assertThatThrownBy(() -> WorkspaceContextHolder.requireAnyRole(WorkspaceRole.OWNER, WorkspaceRole.ADMIN))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("403");
    }

    @Test void viewerIsReadOnly() {
        WorkspaceContextHolder.set(new TenantWorkspace("Viewer test"), WorkspaceRole.VIEWER);
        assertThatThrownBy(() -> WorkspaceContextHolder.requireAnyRole(WorkspaceRole.OWNER, WorkspaceRole.ADMIN, WorkspaceRole.MEMBER))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("403");
    }

    @Test void ownerCanPerformPrivilegedActions() {
        WorkspaceContextHolder.set(new TenantWorkspace("Owner test"), WorkspaceRole.OWNER);
        WorkspaceContextHolder.requireAnyRole(WorkspaceRole.OWNER, WorkspaceRole.ADMIN);
        assertThat(WorkspaceContextHolder.role()).isEqualTo(WorkspaceRole.OWNER);
    }
}
