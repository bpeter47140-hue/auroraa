package com.aurora.projects;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface VentureProjectRepository extends JpaRepository<VentureProject, Long> {
    List<VentureProject> findAllByWorkspace_IdOrderByUpdatedAtDesc(Long workspaceId);
    Optional<VentureProject> findByIdAndWorkspace_Id(Long id, Long workspaceId);
    boolean existsByIdAndWorkspace_Id(Long id, Long workspaceId);
}
