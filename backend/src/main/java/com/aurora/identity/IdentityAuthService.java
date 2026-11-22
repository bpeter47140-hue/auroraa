package com.aurora.identity;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;

@Service
public class IdentityAuthService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final long SESSION_DAYS = 7;

    private final UserAccountRepository users;
    private final BootstrapGuardRepository bootstrapGuards;
    private final TenantWorkspaceRepository workspaces;
    private final WorkspaceMembershipRepository memberships;
    private final AuthSessionRepository sessions;
    private final PasswordEncoder passwordEncoder;
    private final boolean bootstrapEnabled;

    public IdentityAuthService(UserAccountRepository users, BootstrapGuardRepository bootstrapGuards,
                               TenantWorkspaceRepository workspaces,
                               WorkspaceMembershipRepository memberships, AuthSessionRepository sessions,
                               PasswordEncoder passwordEncoder,
                               @Value("${aurora.auth.bootstrap.enabled:true}") boolean bootstrapEnabled) {
        this.users = users;
        this.bootstrapGuards = bootstrapGuards;
        this.workspaces = workspaces;
        this.memberships = memberships;
        this.sessions = sessions;
        this.passwordEncoder = passwordEncoder;
        this.bootstrapEnabled = bootstrapEnabled;
    }

    @Transactional(readOnly = true)
    public boolean isRegistrationOpen() {
        return bootstrapEnabled && users.count() == 0;
    }

    @Transactional
    public AuthDtos.AuthResponse registerFirstOwner(AuthDtos.RegisterRequest request) {
        // Lock the pre-created singleton row until this transaction commits.
        // This serializes competing first-owner requests across app instances.
        bootstrapGuards.lockById(1L)
            .orElseThrow(() -> new IllegalStateException("Bootstrap guard is missing."));
        if (!bootstrapEnabled || users.count() != 0) {
            throw new AuthException("Initial registration is closed. Ask a workspace owner for an invitation.");
        }
        String email = UserAccount.normalizeEmail(request.email());
        if (users.existsByEmail(email)) throw new AuthException("Unable to create this account.");
        UserAccount user = users.save(new UserAccount(
            email, passwordEncoder.encode(request.password()), request.displayName().trim()
        ));
        TenantWorkspace workspace = workspaces.findFirstByOrderByCreatedAtAsc()
            .filter(existing -> existing.getStatus() == WorkspaceStatus.ACTIVE)
            .orElseGet(() -> workspaces.save(new TenantWorkspace(request.workspaceName().trim())));
        memberships.save(new WorkspaceMembership(workspace, user, WorkspaceRole.OWNER));
        return createSessionResponse(user);
    }

    @Transactional
    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request) {
        String email = UserAccount.normalizeEmail(request.email());
        UserAccount user = users.findByEmail(email).orElse(null);
        // Perform a password hash operation even for unknown email to reduce account enumeration timing.
        if (user == null || user.getStatus() != AccountStatus.ACTIVE
            || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new AuthException("Email or password is incorrect.");
        }
        return createSessionResponse(user);
    }

    @Transactional(readOnly = true)
    public UserAccount authenticate(String rawToken) {
        if (rawToken == null || rawToken.length() < 40 || rawToken.length() > 200) return null;
        return sessions.findByTokenHash(sha256(rawToken))
            .filter(session -> session.isUsableAt(Instant.now()))
            .map(AuthSession::getUser)
            .orElse(null);
    }

    @Transactional
    public void changePassword(String rawToken, String currentPassword, String newPassword) {
        if (rawToken == null || rawToken.length() < 40 || rawToken.length() > 200)
            throw new AuthException("Your session is invalid. Sign in again.");
        String currentHash = sha256(rawToken);
        AuthSession currentSession = sessions.findByTokenHash(currentHash)
            .filter(session -> session.isUsableAt(Instant.now()))
            .orElseThrow(() -> new AuthException("Your session has expired. Sign in again."));
        UserAccount user = currentSession.getUser();
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash()))
            throw new AuthException("Your current password is incorrect.");
        if (passwordEncoder.matches(newPassword, user.getPasswordHash()))
            throw new AuthException("Choose a new password different from your current password.");
        user.changePasswordHash(passwordEncoder.encode(newPassword));
        for (AuthSession session : sessions.findAllByUser_Id(user.getId())) {
            if (!session.getTokenHash().equals(currentHash)) session.revoke();
        }
    }

    @Transactional
    public void logout(String rawToken) {
        if (rawToken == null || rawToken.length() < 40 || rawToken.length() > 200) return;
        sessions.findByTokenHash(sha256(rawToken)).ifPresent(AuthSession::revoke);
    }

    @Transactional(readOnly = true)
    public List<AuthDtos.WorkspaceSummary> listWorkspaces(Long userId) {
        return memberships.findAllByUserIdOrderByCreatedAtAsc(userId).stream()
            .map(m -> new AuthDtos.WorkspaceSummary(
                m.getWorkspace().getId(), m.getWorkspace().getName(), m.getRole()
            )).toList();
    }

    private AuthDtos.AuthResponse createSessionResponse(UserAccount user) {
        byte[] bytes = new byte[48];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant expiresAt = Instant.now().plus(SESSION_DAYS, ChronoUnit.DAYS);
        sessions.save(new AuthSession(user, sha256(token), expiresAt));
        return new AuthDtos.AuthResponse(token, expiresAt,
            new AuthDtos.UserSummary(user.getId(), user.getEmail(), user.getDisplayName()),
            listWorkspaces(user.getId()));
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable.", impossible);
        }
    }

    public static class AuthException extends RuntimeException {
        public AuthException(String message) { super(message); }
    }
}
