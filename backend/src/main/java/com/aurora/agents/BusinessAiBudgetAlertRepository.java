package com.aurora.agents;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BusinessAiBudgetAlertRepository extends JpaRepository<BusinessAiBudgetAlert, Long> {
    boolean existsByBusiness_IdAndPeriodStartAndAlertLevel(Long businessId, LocalDate periodStart, String alertLevel);
    @EntityGraph(attributePaths = "business")
    List<BusinessAiBudgetAlert> findTop50ByWorkspace_IdOrderByCreatedAtDesc(Long workspaceId);
    @EntityGraph(attributePaths = "business")
    Optional<BusinessAiBudgetAlert> findByIdAndWorkspace_Id(Long id, Long workspaceId);
}
