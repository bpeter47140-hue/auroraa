package com.aurora;

import com.aurora.identity.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class IdentityFoundationTests {
    @Autowired UserAccountRepository users;
    @Autowired TenantWorkspaceRepository workspaces;
    @Autowired WorkspaceMembershipRepository memberships;
    @Autowired AuthSessionRepository sessions;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    void persistsUsersWorkspacesMembershipsAndHashedSessions() {
        String passwordHash = passwordEncoder.encode("a-long-test-password");
        UserAccount user = users.save(new UserAccount("  OWNER@EXAMPLE.COM ", passwordHash, "Owner"));
        TenantWorkspace workspace = workspaces.save(new TenantWorkspace("Owner workspace"));
        WorkspaceMembership membership = memberships.save(
            new WorkspaceMembership(workspace, user, WorkspaceRole.OWNER)
        );
        AuthSession session = sessions.save(new AuthSession(
            user, "a".repeat(64), Instant.now().plusSeconds(3600)
        ));

        assertThat(user.getEmail()).isEqualTo("owner@example.com");
        assertThat(user.getPasswordHash()).isNotEqualTo("a-long-test-password");
        assertThat(passwordEncoder.matches("a-long-test-password", user.getPasswordHash())).isTrue();
        assertThat(memberships.findByUserIdAndWorkspaceId(user.getId(), workspace.getId()))
            .get().extracting(WorkspaceMembership::getRole).isEqualTo(WorkspaceRole.OWNER);
        assertThat(sessions.findByTokenHash("a".repeat(64))).isPresent();
        assertThat(session.isUsableAt(Instant.now())).isTrue();

        session.revoke();
        assertThat(session.isUsableAt(Instant.now())).isFalse();
    }

    @Test
    void rejectsInvalidSessionHashesAndMissingIdentityFields() {
        UserAccount user = users.save(new UserAccount("owner@example.com", passwordEncoder.encode("password"), "Owner"));
        assertThatThrownBy(() -> new AuthSession(user, "raw-token", Instant.now().plusSeconds(600)))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new UserAccount(" ", "hash", "Owner"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TenantWorkspace(" "))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
