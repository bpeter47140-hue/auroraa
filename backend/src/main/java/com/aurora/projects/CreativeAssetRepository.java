package com.aurora.projects;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CreativeAssetRepository extends JpaRepository<CreativeAsset,Long> {
    List<CreativeAsset> findAllByProject_IdAndWorkspace_IdOrderByCreatedAtDesc(Long projectId, Long workspaceId);
    Optional<CreativeAsset> findByIdAndProject_IdAndWorkspace_Id(Long id, Long projectId, Long workspaceId);
}