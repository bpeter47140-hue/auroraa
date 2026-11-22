package com.aurora.agents;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface AiBudgetGuardLockRepository extends JpaRepository<AiBudgetGuardLock, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from AiBudgetGuardLock l where l.id = :id")
    Optional<AiBudgetGuardLock> findForUpdate(@Param("id") Long id);
}
