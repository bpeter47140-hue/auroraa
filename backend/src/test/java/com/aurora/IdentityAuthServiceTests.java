package com.aurora;

import com.aurora.identity.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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

    @Test
    void firstOwnerAdoptsExistingLegacyWorkspace() {
        TenantWorkspace legacy = workspaces.saveAndFlush(new TenantWorkspace("Legacy workspace"));

        AuthDtos.AuthResponse registered = auth.registerFirstOwner(new AuthDtos.RegisterRequest(
            "legacy.owner@example.com", "a-secure-password-123", "Legacy Owner", "New workspace name"
        ));

        assertThat(registered.workspaces()).hasSize(1);
        assertThat(registered.workspaces().getFirst().id()).isEqualTo(legacy.getId());
        assertThat(registered.workspaces().getFirst().name()).isEqualTo("Legacy workspace");
        assertThat(memberships.findAllByUserIdOrderByCreatedAtAsc(registered.user().id()))
            .extracting(m -> m.getWorkspace().getId()).containsExactly(legacy.getId());
        assertThat(workspaces.count()).isEqualTo(1);
    }

    @Test
    void firstOwnerCanRegisterThenLoginAndLogout() {
        assertThat(auth.isRegistrationOpen()).isTrue();

        AuthDtos.AuthResponse registered = auth.registerFirstOwner(new AuthDtos.RegisterRequest(
            "First.Owner@example.com", "a-secure-password-123", "First Owner", "First workspace"
        ));

        assertThat(registered.token()).isNotBlank();
        assertThat(registered.user().email()).isEqualTo("first.owner@example.com");
        assertThat(registered.workspaces()).hasSize(1);
        assertThat(registered.workspaces().getFirst().role()).isEqualTo(WorkspaceRole.OWNER);
        assertThat(auth.authenticate(registered.token())).isNotNull();
        assertThat(auth.isRegistrationOpen()).isFalse();

        assertThatThrownBy(() -> auth.registerFirstOwner(new AuthDtos.RegisterRequest(
            "second@example.com", "a-secure-password-456", "Second Owner", "Second workspace"
        ))).isInstanceOf(IdentityAuthService.AuthException.class);

        AuthDtos.AuthResponse login = auth.login(new AuthDtos.LoginRequest(
            "FIRST.OWNER@example.com", "a-secure-password-123"
        ));
        assertThat(auth.authenticate(login.token())).isNotNull();

        auth.logout(login.token());
        assertThat(auth.authenticate(login.token())).isNull();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    void concurrentFirstOwnerRegistrationsAllowExactlyOneWinner() throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<Boolean> first = executor.submit(() -> attemptFirstOwner(
                "first.concurrent@example.com", ready, start
            ));
            Future<Boolean> second = executor.submit(() -> attemptFirstOwner(
                "second.concurrent@example.com", ready, start
            ));

            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            boolean firstWon = first.get(30, TimeUnit.SECONDS);
            boolean secondWon = second.get(30, TimeUnit.SECONDS);
            assertThat(firstWon).isNotEqualTo(secondWon);
            assertThat(users.count()).isEqualTo(1);
        }
    }

    private boolean attemptFirstOwner(String email, CountDownLatch ready, CountDownLatch start)
        throws InterruptedException {
        ready.countDown();
        if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Start signal timed out.");
        try {
            auth.registerFirstOwner(new AuthDtos.RegisterRequest(
                email, "a-secure-password-123", "Concurrent Owner", "Concurrent workspace"
            ));
            return true;
        } catch (IdentityAuthService.AuthException expected) {
            return false;
        }
    }

    @Test
    void rejectsInvalidCredentialsAndStoresOnlyTokenDigest() {
        auth.registerFirstOwner(new AuthDtos.RegisterRequest(
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
