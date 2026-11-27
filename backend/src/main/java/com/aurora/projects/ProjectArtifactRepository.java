package com.aurora.projects;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface ProjectArtifactRepository extends JpaRepository<ProjectArtifact,Long>{
 List<ProjectArtifact> findAllByProject_IdAndWorkspace_IdOrderByCreatedAtDesc(Long projectId,Long workspaceId);
 Optional<ProjectArtifact> findByProject_IdAndWorkspace_IdAndRelativePath(Long projectId,Long workspaceId,String relativePath);
}
