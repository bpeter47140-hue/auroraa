package com.aurora.identity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import java.util.List;
import java.util.Optional;

public interface WorkspaceMembershipRepository extends JpaRepository<WorkspaceMembership, Long> {
    @EntityGraph(attributePaths = "workspace")
    List<WorkspaceMembership> findAllByUserIdOrderByCreatedAtAsc(Long userId);
    @EntityGraph(attributePaths = "workspace")
    Optional<WorkspaceMembership> findByUserIdAndWorkspaceId(Long userId, Long workspaceId);
    boolean existsByUserIdAndWorkspaceId(Long userId, Long workspaceId);
    @EntityGraph(attributePaths = "user")
    List<WorkspaceMembership> findAllByWorkspace_IdOrderByCreatedAtAsc(Long workspaceId);
    long countByWorkspace_IdAndRole(Long workspaceId, WorkspaceRole role);
}
