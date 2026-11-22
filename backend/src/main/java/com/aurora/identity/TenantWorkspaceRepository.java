package com.aurora.identity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface TenantWorkspaceRepository extends JpaRepository<TenantWorkspace, Long> {
    java.util.Optional<TenantWorkspace> findFirstByOrderByCreatedAtAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from TenantWorkspace w where w.id = :id")
    Optional<TenantWorkspace> findByIdForUpdate(@Param("id") Long id);
}
