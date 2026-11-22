package com.aurora.agents;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.Instant;

public interface AiBudgetReservationRepository extends JpaRepository<AiBudgetReservation, Long> {
    @Query("select coalesce(sum(r.reservedUsd), 0) from AiBudgetReservation r where r.status = 'HELD'")
    BigDecimal sumAllHeld();
    @Query("select coalesce(sum(r.reservedUsd), 0) from AiBudgetReservation r where r.status = 'HELD' and r.business.id = :businessId")
    BigDecimal sumHeldForBusiness(@Param("businessId") Long businessId);
    @Query("select coalesce(sum(r.actualCostUsd), 0) from AiBudgetReservation r where r.status = 'SETTLED' and r.createdAt >= :from and r.createdAt < :until")
    BigDecimal sumSettledCost(@Param("from") Instant from, @Param("until") Instant until);
    @Query("select coalesce(sum(r.actualCostUsd), 0) from AiBudgetReservation r where r.status = 'SETTLED' and r.business.id = :businessId and r.createdAt >= :from and r.createdAt < :until")
    BigDecimal sumSettledCostForBusiness(@Param("businessId") Long businessId, @Param("from") Instant from, @Param("until") Instant until);
    @Query("select coalesce(sum(u.providerCostUsd), 0) from AiProviderUsage u where u.recordedAt >= :from and u.recordedAt < :until and not exists (select r.id from AiBudgetReservation r where r.agentRun = u.agentRun)")
    BigDecimal sumUnreservedProviderCost(@Param("from") Instant from, @Param("until") Instant until);
    @Query("select coalesce(sum(u.providerCostUsd), 0) from AiProviderUsage u where u.agentRun.business.id = :businessId and u.recordedAt >= :from and u.recordedAt < :until and not exists (select r.id from AiBudgetReservation r where r.agentRun = u.agentRun)")
    BigDecimal sumUnreservedProviderCostForBusiness(@Param("businessId") Long businessId, @Param("from") Instant from, @Param("until") Instant until);
    @EntityGraph(attributePaths = {"business", "agentRun"})
    @Query("select r from AiBudgetReservation r where r.workspace.id = :workspaceId and r.status = 'HELD' and (r.agentRun.status <> 'RUNNING' or r.agentRun.startedAt < :staleBefore) order by r.createdAt asc")
    List<AiBudgetReservation> findUnresolvedForWorkspace(@Param("workspaceId") Long workspaceId,
                                                          @Param("staleBefore") Instant staleBefore,
                                                          Pageable pageable);
    @EntityGraph(attributePaths = {"business", "agentRun"})
    java.util.Optional<AiBudgetReservation> findByIdAndWorkspace_Id(Long id, Long workspaceId);
}
