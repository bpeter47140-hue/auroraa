package com.aurora;

import com.aurora.identity.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class IdentityAuthServiceTests {
    @Autowired IdentityAuthService auth;
    @Autowired UserAccountRepository users;
    @Autowired AuthSessionRepository sessions;
    @Autowired TenantWorkspaceRepository workspaces;
    @Autowired WorkspaceMembershipRepository memberships;
    @Autowired WorkspaceInvitationRepository invitations;

    @Test
    void firstOwnerAdoptsExistingLegacyWorkspace() {
        TenantWorkspace legacy = workspaces.saveAndFlush(new TenantWorkspace("Legacy workspace"));
        TenantWorkspace expectedWorkspace = workspaces.findFirstByOrderByCreatedAtAsc().orElseThrow();
        long workspaceCountAfterSetup = workspaces.count();

        AuthDtos.AuthResponse registered = auth.register(new AuthDtos.RegisterRequest(
            "legacy.owner@example.com", "a-secure-password-123", "Legacy Owner", "New workspace name"
        ));

        assertThat(registered.workspaces()).hasSize(1);
        assertThat(registered.workspaces().getFirst().id()).isEqualTo(expectedWorkspace.getId());
        assertThat(memberships.findAllByUserIdOrderByCreatedAtAsc(registered.user().id()))
            .extracting(m -> m.getWorkspace().getId()).containsExactly(expectedWorkspace.getId());
        assertThat(workspaces.count()).isEqualTo(workspaceCountAfterSetup);
        assertThat(legacy.getStatus()).isEqualTo(WorkspaceStatus.ACTIVE);
    }

    @Test
    void firstOwnerCanRegisterThenLoginAndLogout() {
        assertThat(auth.isRegistrationOpen()).isTrue();

        AuthDtos.AuthResponse registered = auth.register(new AuthDtos.RegisterRequest(
            "First.Owner@example.com", "a-secure-password-123", "First Owner", "First workspace"
        ));

        assertThat(registered.token()).isNotBlank();
        assertThat(registered.user().email()).isEqualTo("first.owner@example.com");
        assertThat(registered.workspaces()).hasSize(1);
        assertThat(registered.workspaces().getFirst().role()).isEqualTo(WorkspaceRole.OWNER);
        assertThat(auth.authenticate(registered.token())).isNotNull();
        assertThat(users.findByEmail("first.owner@example.com").orElseThrow().isPlatformAdmin()).isTrue();
        assertThat(users.countByLastLoginAtIsNotNull()).isEqualTo(1);
        assertThat(auth.isRegistrationOpen()).isTrue();

        AuthDtos.AuthResponse second = auth.register(new AuthDtos.RegisterRequest(
            "second@example.com", "a-secure-password-456", "Second Owner", "Second workspace"
        ));
        assertThat(second.workspaces()).hasSize(1);
        assertThat(second.workspaces().getFirst().name()).isEqualTo("Second workspace");
        assertThat(second.workspaces().getFirst().role()).isEqualTo(WorkspaceRole.OWNER);
        assertThat(second.user().platformAdmin()).isFalse();
        assertThat(users.findByEmail("second@example.com").orElseThrow().isPlatformAdmin()).isFalse();
        assertThat(second.workspaces().getFirst().id()).isNotEqualTo(registered.workspaces().getFirst().id());
        assertThat(auth.isRegistrationOpen()).isTrue();

        AuthDtos.AuthResponse login = auth.login(new AuthDtos.LoginRequest(
            "FIRST.OWNER@example.com", "a-secure-password-123"
        ));
        assertThat(auth.authenticate(login.token())).isNotNull();
        auth.recordActivity(login.token());
        Instant now = Instant.now();
        assertThat(sessions.countValidSessions(now)).isEqualTo(3);
        assertThat(sessions.countDistinctOnlineUsers(now, now.minusSeconds(300))).isEqualTo(2);
        assertThat(sessions.countSessionsCreatedSince(now.minusSeconds(60))).isEqualTo(3);
        assertThat(sessions.findTop100ByRevokedAtIsNullAndExpiresAtAfterAndUser_StatusOrderByLastSeenAtDesc(now, AccountStatus.ACTIVE))
            .hasSize(3);

        auth.logout(login.token());
        assertThat(auth.authenticate(login.token())).isNull();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    void concurrentPublicRegistrationsCreateIndependentWorkspaces() throws Exception {
        long usersBefore = users.count();
        long workspacesBefore = workspaces.count();
        long platformAdminsBefore = users.findAll().stream().filter(UserAccount::isPlatformAdmin).count();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<Boolean> first = executor.submit(() -> attemptRegistration(
                "first.concurrent@example.com", ready, start
            ));
            Future<Boolean> second = executor.submit(() -> attemptRegistration(
                "second.concurrent@example.com", ready, start
            ));

            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            boolean firstWon = first.get(30, TimeUnit.SECONDS);
            boolean secondWon = second.get(30, TimeUnit.SECONDS);
            assertThat(firstWon).isTrue();
            assertThat(secondWon).isTrue();
            assertThat(users.count()).isEqualTo(usersBefore + 2);
            assertThat(workspaces.count()).isEqualTo(workspacesBefore
                + (usersBefore == 0 && workspacesBefore > 0 ? 1 : 2));
            UserAccount firstAccount = users.findByEmail("first.concurrent@example.com").orElseThrow();
            UserAccount secondAccount = users.findByEmail("second.concurrent@example.com").orElseThrow();
            Long firstWorkspace = memberships.findAllByUserIdOrderByCreatedAtAsc(firstAccount.getId()).getFirst().getWorkspace().getId();
            Long secondWorkspace = memberships.findAllByUserIdOrderByCreatedAtAsc(secondAccount.getId()).getFirst().getWorkspace().getId();
            assertThat(firstWorkspace).isNotEqualTo(secondWorkspace);
            assertThat(users.findAll()).filteredOn(UserAccount::isPlatformAdmin)
                .hasSize((int) (platformAdminsBefore + (usersBefore == 0 ? 1 : 0)));
        }
    }

    private boolean attemptRegistration(String email, CountDownLatch ready, CountDownLatch start)
        throws InterruptedException {
        ready.countDown();
        if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Start signal timed out.");
        try {
            auth.register(new AuthDtos.RegisterRequest(
                email, "a-secure-password-123", "Concurrent Owner", "Concurrent workspace"
            ));
            return true;
        } catch (IdentityAuthService.AuthException expected) {
            return false;
        }
    }

    @Test
    void pendingInvitationCountExcludesAcceptedRevokedAndExpiredInvitations() {
        TenantWorkspace workspace = workspaces.saveAndFlush(new TenantWorkspace("Invitation count workspace"));
        Instant now = Instant.now();
        WorkspaceInvitation pending = new WorkspaceInvitation(workspace, "pending@example.com",
            WorkspaceRole.MEMBER, "a".repeat(64), now.plusSeconds(3600));
        WorkspaceInvitation accepted = new WorkspaceInvitation(workspace, "accepted@example.com",
            WorkspaceRole.MEMBER, "b".repeat(64), now.plusSeconds(3600));
        WorkspaceInvitation revoked = new WorkspaceInvitation(workspace, "revoked@example.com",
            WorkspaceRole.MEMBER, "c".repeat(64), now.plusSeconds(3600));
        WorkspaceInvitation expired = new WorkspaceInvitation(workspace, "expired@example.com",
            WorkspaceRole.MEMBER, "d".repeat(64), now.minusSeconds(1));
        invitations.saveAndFlush(pending);
        accepted.accept();
        revoked.revoke();
        invitations.saveAndFlush(accepted);
        invitations.saveAndFlush(revoked);
        invitations.saveAndFlush(expired);

        assertThat(invitations.countByAcceptedAtIsNullAndRevokedAtIsNullAndExpiresAtAfter(Instant.now()))
            .isEqualTo(1);
    }

    @Test
    void rejectsInvalidCredentialsAndStoresOnlyTokenDigest() {
        auth.register(new AuthDtos.RegisterRequest(
            "owner@example.com", "a-secure-password-123", "Owner", "Workspace"
        ));

        assertThatThrownBy(() -> auth.login(new AuthDtos.LoginRequest(
            "owner@example.com", "incorrect-password"
        ))).isInstanceOf(IdentityAuthService.AuthException.class);

        AuthDtos.AuthResponse login = auth.login(new AuthDtos.LoginRequest(
            "owner@example.com", "a-secure-password-123"
        ));
        assertThat(sessions.findByTokenHash(login.token())).isEmpty();
        assertThat(sessions.findByTokenHash(hashForTest(login.token()))).isPresent();
    }

    private static String hashForTest(String token) {
        try {
            return java.util.HexFormat.of().formatHex(
                java.security.MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8))
            );
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
