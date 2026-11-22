package com.aurora.identity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "auth_sessions",
    uniqueConstraints = @UniqueConstraint(name = "uk_auth_session_token_hash", columnNames = "token_hash"),
    indexes = @Index(name = "idx_auth_session_user_expiry", columnList = "user_id, expires_at")
)
public class AuthSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_auth_session_user"))
    private UserAccount user;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AuthSession() {}

    public AuthSession(UserAccount user, String tokenHash, Instant expiresAt) {
        if (tokenHash == null || !tokenHash.matches("[a-fA-F0-9]{64}"))
            throw new IllegalArgumentException("Session token hash must be a SHA-256 hex digest.");
        if (expiresAt == null || !expiresAt.isAfter(Instant.now()))
            throw new IllegalArgumentException("Session expiry must be in the future.");
        this.user = user;
        this.tokenHash = tokenHash.toLowerCase();
        this.expiresAt = expiresAt;
    }

    @PrePersist
    void onCreate() { createdAt = Instant.now(); }

    public Long getId() { return id; }
    public UserAccount getUser() { return user; }
    public String getTokenHash() { return tokenHash; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getRevokedAt() { return revokedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public boolean isUsableAt(Instant now) {
        return revokedAt == null && expiresAt.isAfter(now) && user.getStatus() == AccountStatus.ACTIVE;
    }
    public void revoke() { if (revokedAt == null) revokedAt = Instant.now(); }
}
