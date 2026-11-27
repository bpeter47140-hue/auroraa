package com.aurora.projects;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface ProjectArtifactVersionRepository extends JpaRepository<ProjectArtifactVersion,Long>{
 List<ProjectArtifactVersion> findAllByArtifact_IdAndWorkspace_IdOrderByVersionNumberDesc(Long artifactId,Long workspaceId);
 Optional<ProjectArtifactVersion> findByIdAndArtifact_IdAndWorkspace_Id(Long id,Long artifactId,Long workspaceId);
 int countByArtifact_IdAndWorkspace_Id(Long artifactId,Long workspaceId);
 Optional<ProjectArtifactVersion> findFirstByArtifact_IdAndWorkspace_IdAndSha256OrderByVersionNumberDesc(Long artifactId,Long workspaceId,String sha256);
}
