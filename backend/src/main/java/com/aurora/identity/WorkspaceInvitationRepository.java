package com.aurora.identity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import java.util.List;
import java.util.Optional;
public interface WorkspaceInvitationRepository extends JpaRepository<WorkspaceInvitation,Long>{
 @EntityGraph(attributePaths="workspace") List<WorkspaceInvitation> findAllByWorkspace_IdOrderByCreatedAtDesc(Long workspaceId);
 @EntityGraph(attributePaths="workspace") Optional<WorkspaceInvitation> findByTokenHash(String tokenHash);
 List<WorkspaceInvitation> findAllByWorkspace_IdAndEmail(Long workspaceId, String email);
}
