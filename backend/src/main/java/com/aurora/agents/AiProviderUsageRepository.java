package com.aurora.agents;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.time.Instant;
import java.util.List;

public interface AiProviderUsageRepository extends JpaRepository<AiProviderUsage, Long> {
    java.util.Optional<AiProviderUsage> findByAgentRun_Id(Long agentRunId);
    java.util.List<AiProviderUsage> findAllByAgentRun_IdIn(java.util.Collection<Long> agentRunIds);
    @EntityGraph(attributePaths = {"agentRun", "agentRun.business"})
    List<AiProviderUsage> findByWorkspace_IdAndRecordedAtGreaterThanEqualAndRecordedAtLessThan(
        Long workspaceId, Instant from, Instant until);
}
