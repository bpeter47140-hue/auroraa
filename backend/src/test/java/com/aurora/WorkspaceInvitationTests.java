package com.aurora;

import com.aurora.identity.*;
import com.aurora.security.WorkspaceContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class WorkspaceInvitationTests {
    @Autowired WorkspaceMemberService service;
    @Autowired TenantWorkspaceRepository workspaces;
    @Autowired WorkspaceInvitationRepository invitations;
    @Autowired UserAccountRepository users;
    @Autowired WorkspaceMembershipRepository memberships;
    @Autowired PasswordEncoder passwordEncoder;

    @AfterEach void clearContext() { WorkspaceContextHolder.clear(); }

    @Test void inviteTokenIsHashedAndNewInviteeCanJoinOnlyOnce() {
        TenantWorkspace workspace = workspaces.saveAndFlush(new TenantWorkspace("Invite test"));
        WorkspaceContextHolder.set(workspace, WorkspaceRole.OWNER);

        WorkspaceMemberService.CreatedInvitation created = service.invite("New.Person@example.com", WorkspaceRole.MEMBER);
        WorkspaceInvitation stored = invitations.findById(created.invitation().id()).orElseThrow();
        assertThat(stored.getEmail()).isEqualTo("new.person@example.com");
        assertThat(stored.getTokenHash()).isNotEqualTo(created.invitationToken());
        assertThat(stored.getTokenHash()).hasSize(64);

        WorkspaceMemberService.MemberView joined = service.acceptAndRegister(created.invitationToken(), "New Person", "a-very-long-test-password");
        assertThat(joined.email()).isEqualTo("new.person@example.com");
        assertThat(joined.role()).isEqualTo(WorkspaceRole.MEMBER);
        assertThat(passwordEncoder.matches("a-very-long-test-password", users.findByEmail(joined.email()).orElseThrow().getPasswordHash())).isTrue();
        assertThat(memberships.existsByUserIdAndWorkspaceId(joined.userId(), workspace.getId())).isTrue();
        assertThat(stored.getAcceptedAt()).isNotNull();
        assertThatThrownBy(() -> service.acceptAndRegister(created.invitationToken(), "New Person", "another-long-password"))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("410");
    }

    @Test void administratorsCanInviteMembersButCannotPromoteOrManageAdmins() {
        TenantWorkspace workspace = workspaces.saveAndFlush(new TenantWorkspace("Admin invite test"));
        WorkspaceContextHolder.set(workspace, WorkspaceRole.ADMIN);
        assertThat(service.invite("member@example.com", WorkspaceRole.MEMBER).invitation().role()).isEqualTo(WorkspaceRole.MEMBER);
        assertThatThrownBy(() -> service.invite("admin@example.com", WorkspaceRole.ADMIN))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("403");
    }

    @Test void duplicatePendingInvitationsAreRejected() {
        TenantWorkspace workspace = workspaces.saveAndFlush(new TenantWorkspace("Duplicate invite test"));
        WorkspaceContextHolder.set(workspace, WorkspaceRole.OWNER);
        service.invite("same.person@example.com", WorkspaceRole.MEMBER);
        assertThatThrownBy(() -> service.invite("SAME.PERSON@example.com", WorkspaceRole.VIEWER))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("409");
        assertThat(invitations.findAllByWorkspace_IdOrderByCreatedAtDesc(workspace.getId())).hasSize(1);
    }

    @Test void lastOwnerCannotBeDemotedOrRemoved() {
        TenantWorkspace workspace = workspaces.saveAndFlush(new TenantWorkspace("Last owner test"));
        UserAccount owner = users.saveAndFlush(new UserAccount("last.owner@example.com", passwordEncoder.encode("another-long-password"), "Last Owner"));
        WorkspaceMembership membership = memberships.saveAndFlush(new WorkspaceMembership(workspace, owner, WorkspaceRole.OWNER));
        WorkspaceContextHolder.set(workspace, WorkspaceRole.OWNER);
        assertThatThrownBy(() -> service.changeRole(membership.getId(), WorkspaceRole.ADMIN))
            .isInstanceOf(ResponseStatusException.class).hasMessageContaining("409");
        assertThatThrownBy(() -> service.remove(membership.getId()))
            .isInstanceOf(ResponseStatusException.class).hasMessageContaining("409");
        assertThat(memberships.existsByUserIdAndWorkspaceId(owner.getId(), workspace.getId())).isTrue();
    }

    @Test void ownerCanInviteAnAdministrator() {
        TenantWorkspace workspace = workspaces.saveAndFlush(new TenantWorkspace("Owner admin invite test"));
        WorkspaceContextHolder.set(workspace, WorkspaceRole.OWNER);
        WorkspaceMemberService.CreatedInvitation created = service.invite("new.admin@example.com", WorkspaceRole.ADMIN);
        assertThat(created.invitation().role()).isEqualTo(WorkspaceRole.ADMIN);
    }

    @Test void wrongAccountCannotAcceptInvitation() {
        TenantWorkspace workspace = workspaces.saveAndFlush(new TenantWorkspace("Email binding test"));
        WorkspaceContextHolder.set(workspace, WorkspaceRole.OWNER);
        WorkspaceMemberService.CreatedInvitation created = service.invite("intended@example.com", WorkspaceRole.VIEWER);
        UserAccount wrongUser = users.saveAndFlush(new UserAccount("other@example.com", passwordEncoder.encode("another-long-password"), "Other User"));
        assertThatThrownBy(() -> service.accept(created.invitationToken(), wrongUser))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("403");
        assertThat(memberships.existsByUserIdAndWorkspaceId(wrongUser.getId(), workspace.getId())).isFalse();
    }
}
