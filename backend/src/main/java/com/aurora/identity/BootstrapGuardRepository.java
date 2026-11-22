package com.aurora.identity;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BootstrapGuardRepository extends JpaRepository<BootstrapGuard, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select guard from BootstrapGuard guard where guard.id = :id")
    Optional<BootstrapGuard> lockById(@Param("id") Long id);
}
