package com.aurora.projects;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface ProjectReviewRepository extends JpaRepository<ProjectReview, Long> {
 List<ProjectReview> findAllByProject_IdAndWorkspace_IdOrderByCreatedAtDesc(Long projectId, Long workspaceId);
 Optional<ProjectReview> findByIdAndProject_IdAndWorkspace_Id(Long id, Long projectId, Long workspaceId);
 boolean existsByArtifact_IdAndArtifactSha256AndStatus(Long artifactId, String hash, String status);
}
