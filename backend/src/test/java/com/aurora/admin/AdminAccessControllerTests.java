package com.aurora.admin;

import com.aurora.identity.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminAccessControllerTests {
    @Test
    void workspaceAdminDoesNotReceivePlatformWideAccessMetrics() {
        UserAccount member = new UserAccount("member@example.com", "not-a-real-hash", "Workspace Admin");
        AdminAccessController controller = new AdminAccessController(
            mock(UserAccountRepository.class), mock(AuthSessionRepository.class), mock(WorkspaceInvitationRepository.class));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("aurora.authenticatedUser", member);

        assertThatThrownBy(() -> controller.snapshot(request))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Platform administrator access required.");
    }

    @Test
    void platformOwnerReceivesGlobalAccountAndSessionMetrics() {
        UserAccount owner = new UserAccount("owner@example.com", "not-a-real-hash", "Platform Owner");
        owner.grantPlatformAdmin();
        UserAccountRepository users = mock(UserAccountRepository.class);
        AuthSessionRepository sessions = mock(AuthSessionRepository.class);
        WorkspaceInvitationRepository invitations = mock(WorkspaceInvitationRepository.class);
        when(users.count()).thenReturn(8L);
        when(users.countByStatus(AccountStatus.ACTIVE)).thenReturn(7L);
        when(users.countByStatus(AccountStatus.DISABLED)).thenReturn(1L);
        when(users.countByLastLoginAtIsNotNull()).thenReturn(5L);
        when(invitations.countByAcceptedAtIsNullAndRevokedAtIsNullAndExpiresAtAfter(any(Instant.class))).thenReturn(2L);
        when(sessions.findTop100ByRevokedAtIsNullAndExpiresAtAfterAndUser_StatusOrderByLastSeenAtDesc(
            any(Instant.class), eq(AccountStatus.ACTIVE))).thenReturn(List.of());
        when(sessions.countDistinctOnlineUsers(any(Instant.class), any(Instant.class))).thenReturn(2L);
        when(sessions.countValidSessions(any(Instant.class))).thenReturn(3L);
        when(sessions.countSessionsCreatedSince(any(Instant.class))).thenReturn(4L);

        AdminAccessController controller = new AdminAccessController(users, sessions, invitations);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("aurora.authenticatedUser", owner);

        AdminAccessController.Snapshot snapshot = controller.snapshot(request);

        assertThat(snapshot.registeredAccounts()).isEqualTo(8);
        assertThat(snapshot.activeAccounts()).isEqualTo(7);
        assertThat(snapshot.disabledAccounts()).isEqualTo(1);
        assertThat(snapshot.accountsEverLoggedIn()).isEqualTo(5);
        assertThat(snapshot.pendingInvitations()).isEqualTo(2);
        assertThat(snapshot.onlineUsers()).isEqualTo(2);
        assertThat(snapshot.activeSessions()).isEqualTo(3);
        assertThat(snapshot.sessionsStartedLast30Days()).isEqualTo(4);
        assertThat(snapshot.onlineWindowMinutes()).isEqualTo(5);
        assertThat(snapshot.generatedAt()).isNotNull();
    }
}
