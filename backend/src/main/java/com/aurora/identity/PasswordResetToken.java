package com.aurora.identity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "password_reset_tokens",
    uniqueConstraints = @UniqueConstraint(name = "uk_password_reset_token_hash", columnNames = "token_hash"),
    indexes = @Index(name = "idx_password_reset_user_expiry", columnList = "user_id, expires_at"))
public class PasswordResetToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_password_reset_user"))
    private UserAccount user;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PasswordResetToken() {}

    public PasswordResetToken(UserAccount user, String tokenHash, Instant expiresAt) {
        if (tokenHash == null || !tokenHash.matches("[a-fA-F0-9]{64}"))
            throw new IllegalArgumentException("Reset token hash must be a SHA-256 hex digest.");
        if (expiresAt == null || !expiresAt.isAfter(Instant.now()))
            throw new IllegalArgumentException("Reset token expiry must be in the future.");
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
    public Instant getUsedAt() { return usedAt; }
    public boolean isUsableAt(Instant now) {
        return usedAt == null && expiresAt.isAfter(now) && user.getStatus() == AccountStatus.ACTIVE;
    }
    public void markUsed() { if (usedAt == null) usedAt = Instant.now(); }
}
