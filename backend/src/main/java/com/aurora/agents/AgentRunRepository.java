package com.aurora.agents;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AgentRunRepository extends JpaRepository<AgentRun, Long> {
    List<AgentRun> findTop50ByWorkspace_IdOrderByStartedAtDesc(Long workspaceId);
}
