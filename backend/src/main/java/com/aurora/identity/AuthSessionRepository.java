package com.aurora.identity;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.Optional;

public interface AuthSessionRepository extends JpaRepository<AuthSession, Long> {
    Optional<AuthSession> findByTokenHash(String tokenHash);
    java.util.List<AuthSession> findAllByUser_Id(Long userId);
    long deleteByExpiresAtBeforeOrRevokedAtBefore(Instant expiredBefore, Instant revokedBefore);
}
