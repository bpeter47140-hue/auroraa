package com.aurora.projects;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface MarketingCampaignRepository extends JpaRepository<MarketingCampaign, Long> {
    List<MarketingCampaign> findAllByProject_IdAndWorkspace_IdOrderByCreatedAtDesc(Long projectId, Long workspaceId);
    Optional<MarketingCampaign> findByIdAndProject_IdAndWorkspace_Id(Long id, Long projectId, Long workspaceId);
}
