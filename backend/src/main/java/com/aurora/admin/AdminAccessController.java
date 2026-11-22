package com.aurora.admin;

import com.aurora.identity.AuthSession;
import com.aurora.identity.AuthSessionRepository;
import com.aurora.identity.UserAccount;
import com.aurora.identity.UserAccountRepository;
import com.aurora.identity.WorkspaceInvitationRepository;
import com.aurora.identity.AccountStatus;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/access")
public class AdminAccessController {
    private static final long ONLINE_WINDOW_MINUTES = 5;
    private final UserAccountRepository users;
    private final AuthSessionRepository sessions;
    private final WorkspaceInvitationRepository invitations;

    public AdminAccessController(UserAccountRepository users, AuthSessionRepository sessions,
                                 WorkspaceInvitationRepository invitations) {
        this.users = users;
        this.sessions = sessions;
        this.invitations = invitations;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public Snapshot snapshot(HttpServletRequest request) {
        requirePlatformAdmin(request);
        Instant now = Instant.now();
        Instant onlineSince = now.minus(ONLINE_WINDOW_MINUTES, ChronoUnit.MINUTES);
        Instant thirtyDaysAgo = now.minus(30, ChronoUnit.DAYS);
        List<SessionView> recentSessions = sessions
            .findTop100ByRevokedAtIsNullAndExpiresAtAfterAndUser_StatusOrderByLastSeenAtDesc(now, AccountStatus.ACTIVE)
            .stream()
            .map(s -> new SessionView(s.getId(), s.getUser().getEmail(),
                s.getUser().getDisplayName(), s.getCreatedAt(), s.getLastSeenAt(),
                s.getExpiresAt(), !s.getLastSeenAt().isBefore(onlineSince)))
            .toList();
        return new Snapshot(users.count(), users.countByStatus(AccountStatus.ACTIVE),
            users.countByStatus(com.aurora.identity.AccountStatus.DISABLED),
            users.countByLastLoginAtIsNotNull(), invitations.countByAcceptedAtIsNullAndRevokedAtIsNullAndExpiresAtAfter(now),
            sessions.countDistinctOnlineUsers(now, onlineSince),
            sessions.countValidSessions(now), sessions.countSessionsCreatedSince(thirtyDaysAgo),
            recentSessions, now, ONLINE_WINDOW_MINUTES);
    }

    @PostMapping("/sessions/{sessionId}/revoke")
    @Transactional
    public void revokeSession(@PathVariable Long sessionId, HttpServletRequest request) {
        requirePlatformAdmin(request);
        AuthSession session = sessions.findById(sessionId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found."));
        session.revoke();
    }

    private static void requirePlatformAdmin(HttpServletRequest request) {
        Object value = request.getAttribute("aurora.authenticatedUser");
        if (!(value instanceof UserAccount user) || !user.isPlatformAdmin())
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Platform administrator access required.");
    }

    public record Snapshot(long registeredAccounts, long activeAccounts, long disabledAccounts,
                           long accountsEverLoggedIn, long pendingInvitations, long onlineUsers, long activeSessions,
                           long sessionsStartedLast30Days, List<SessionView> sessions,
                           Instant generatedAt, long onlineWindowMinutes) {}
    public record SessionView(Long id, String email, String displayName, Instant createdAt,
                              Instant lastSeenAt, Instant expiresAt, boolean online) {}
}
