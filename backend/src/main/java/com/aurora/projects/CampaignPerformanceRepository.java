package com.aurora.projects;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CampaignPerformanceRepository extends JpaRepository<CampaignPerformanceMetric,Long> {
    List<CampaignPerformanceMetric> findAllByCampaign_IdAndWorkspace_IdOrderByReportedDateDesc(Long campaignId,Long workspaceId);
    Optional<CampaignPerformanceMetric> findByCampaign_IdAndWorkspace_IdAndReportedDate(Long campaignId,Long workspaceId,LocalDate reportedDate);
}
