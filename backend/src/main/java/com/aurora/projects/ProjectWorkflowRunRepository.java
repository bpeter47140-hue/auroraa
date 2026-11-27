package com.aurora.projects;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.time.Instant;

public interface ProjectWorkflowRunRepository extends JpaRepository<ProjectWorkflowRun, Long> {
    List<ProjectWorkflowRun> findAllByWorkspace_IdOrderByCreatedAtDesc(Long workspaceId);
    List<ProjectWorkflowRun> findAllByStatusOrderByCreatedAtAsc(String status);
    List<ProjectWorkflowRun> findAllByProject_IdAndWorkspace_IdOrderByCreatedAtDesc(Long projectId, Long workspaceId);
    @EntityGraph(attributePaths = {"project"})
    Optional<ProjectWorkflowRun> findByIdAndWorkspace_Id(Long id, Long workspaceId);
    @EntityGraph(attributePaths = {"workspace", "project"})
    Optional<ProjectWorkflowRun> findWithWorkspaceAndProjectById(Long id);

    // Compare-and-set claim: only one worker can transition a queued run to RUNNING.
    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "UPDATE project_workflow_runs SET status = 'RUNNING', started_at = CURRENT_TIMESTAMP " +
        "WHERE id = :id AND status = 'QUEUED'", nativeQuery = true)
    int claimQueued(@Param("id") Long id);

    @Transactional @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "UPDATE project_workflow_runs SET status='RUNNING', started_at=COALESCE(started_at,CURRENT_TIMESTAMP), worker_id=:worker, heartbeat_at=CURRENT_TIMESTAMP, lease_expires_at=:expiry WHERE id=:id AND status='QUEUED'", nativeQuery=true)
    int claimQueuedWithLease(@Param("id") Long id, @Param("worker") String worker, @Param("expiry") Instant expiry);

    @Transactional @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "UPDATE project_workflow_runs SET heartbeat_at=CURRENT_TIMESTAMP, lease_expires_at=:expiry WHERE id=:id AND status='RUNNING' AND worker_id=:worker AND lease_expires_at > CURRENT_TIMESTAMP", nativeQuery=true)
    int renewLease(@Param("id") Long id, @Param("worker") String worker, @Param("expiry") Instant expiry);

    List<ProjectWorkflowRun> findAllByStatusAndLeaseExpiresAtBeforeOrderByCreatedAtAsc(String status, Instant now);

    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "UPDATE project_workflow_runs SET status = 'QUEUED', finished_at = NULL, error = NULL " +
        "WHERE id = :id AND workspace_id = :workspaceId AND status = 'FAILED'", nativeQuery = true)
    int requeueFailedForRetry(@Param("id") Long id, @Param("workspaceId") Long workspaceId);
}
