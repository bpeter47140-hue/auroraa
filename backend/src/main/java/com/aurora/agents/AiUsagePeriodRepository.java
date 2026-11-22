package com.aurora.agents;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.Optional;

public interface AiUsagePeriodRepository extends JpaRepository<AiUsagePeriod, Long> {
    Optional<AiUsagePeriod> findByWorkspace_IdAndPeriodStart(Long workspaceId, LocalDate periodStart);
}
