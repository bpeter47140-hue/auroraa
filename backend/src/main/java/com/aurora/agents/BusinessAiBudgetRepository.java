package com.aurora.agents;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface BusinessAiBudgetRepository extends JpaRepository<BusinessAiBudget, Long> {
    Optional<BusinessAiBudget> findByBusinessIdAndWorkspace_Id(Long businessId, Long workspaceId);
    void deleteByBusinessIdAndWorkspace_Id(Long businessId, Long workspaceId);
}
