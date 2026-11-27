package com.aurora.projects;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProjectWorkflowStepRepository extends JpaRepository<ProjectWorkflowStep, Long> {
    List<ProjectWorkflowStep> findAllByWorkflowRun_IdOrderByStepOrderAsc(Long workflowRunId);
    java.util.Optional<ProjectWorkflowStep> findByIdAndWorkflowRun_Id(Long id, Long workflowRunId);
}
