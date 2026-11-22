package com.aurora.identity;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

@Service
public class PasswordResetService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final long TOKEN_MINUTES = 30;

    private final UserAccountRepository users;
    private final PasswordResetTokenRepository resetTokens;
    private final AuthSessionRepository sessions;
    private final PasswordEncoder passwordEncoder;
    private final InvitationEmailService email;

    public PasswordResetService(UserAccountRepository users, PasswordResetTokenRepository resetTokens,
                                AuthSessionRepository sessions, PasswordEncoder passwordEncoder,
                                InvitationEmailService email) {
        this.users = users;
        this.resetTokens = resetTokens;
        this.sessions = sessions;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
    }

    @Transactional
    public void requestReset(String emailAddress) {
        String normalized = UserAccount.normalizeEmail(emailAddress);
        UserAccount user = users.findByEmail(normalized)
            .filter(account -> account.getStatus() == AccountStatus.ACTIVE)
            .orElse(null);
        if (user == null) return;

        resetTokens.findAllByUser_IdAndUsedAtIsNull(user.getId()).forEach(PasswordResetToken::markUsed);
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant expiresAt = Instant.now().plus(TOKEN_MINUTES, ChronoUnit.MINUTES);
        resetTokens.save(new PasswordResetToken(user, sha256(rawToken), expiresAt));
        // A missing or failing mail provider must not reveal whether an account exists.
        email.sendPasswordReset(user.getEmail(), rawToken, expiresAt);
    }

    @Transactional
    public void completeReset(String rawToken, String newPassword) {
        if (rawToken == null || rawToken.length() < 40 || rawToken.length() > 100)
            throw invalidToken();
        PasswordResetToken token = resetTokens.findByTokenHash(sha256(rawToken))
            .filter(value -> value.isUsableAt(Instant.now()))
            .orElseThrow(PasswordResetService::invalidToken);
        UserAccount user = token.getUser();
        user.changePasswordHash(passwordEncoder.encode(newPassword));
        token.markUsed();
        resetTokens.findAllByUser_IdAndUsedAtIsNull(user.getId()).forEach(PasswordResetToken::markUsed);
        sessions.findAllByUser_Id(user.getId()).forEach(AuthSession::revoke);
    }

    private static ResponseStatusException invalidToken() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "This password reset link is invalid or has expired. Request a new link.");
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (Exception impossible) {
            throw new IllegalStateException("SHA-256 is unavailable.", impossible);
        }
    }
}
