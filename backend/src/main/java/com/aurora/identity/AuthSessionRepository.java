package com.aurora.identity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.Optional;
import java.util.List;

public interface AuthSessionRepository extends JpaRepository<AuthSession, Long> {
    Optional<AuthSession> findByTokenHash(String tokenHash);
    java.util.List<AuthSession> findAllByUser_Id(Long userId);
    long deleteByExpiresAtBeforeOrRevokedAtBefore(Instant expiredBefore, Instant revokedBefore);

    @EntityGraph(attributePaths = "user")
    List<AuthSession> findTop100ByRevokedAtIsNullAndExpiresAtAfterAndUser_StatusOrderByLastSeenAtDesc(Instant now, AccountStatus status);

    @Query("select count(s) from AuthSession s where s.revokedAt is null and s.expiresAt > :now and s.user.status = com.aurora.identity.AccountStatus.ACTIVE")
    long countValidSessions(@Param("now") Instant now);

    @Query("select count(distinct s.user.id) from AuthSession s where s.revokedAt is null and s.expiresAt > :now and s.lastSeenAt >= :onlineSince and s.user.status = com.aurora.identity.AccountStatus.ACTIVE")
    long countDistinctOnlineUsers(@Param("now") Instant now, @Param("onlineSince") Instant onlineSince);

    @Query("select count(s) from AuthSession s where s.createdAt >= :since")
    long countSessionsCreatedSince(@Param("since") Instant since);

    @Query("select count(distinct s.user.id) from AuthSession s")
    long countDistinctUsersWithAnySession();
}
