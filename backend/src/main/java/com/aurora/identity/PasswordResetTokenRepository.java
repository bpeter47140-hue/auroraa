package com.aurora.identity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.EntityGraph;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    // Serialize concurrent reset attempts so a one-time token cannot be consumed twice.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "user")
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);
    List<PasswordResetToken> findAllByUser_IdAndUsedAtIsNull(Long userId);
}
