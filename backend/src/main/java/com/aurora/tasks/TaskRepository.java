package com.aurora.tasks;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TaskRepository extends JpaRepository<TaskItem, Long> {
    List<TaskItem> findAllByWorkspace_IdOrderByCreatedAtDesc(Long workspaceId);
    java.util.Optional<TaskItem> findByIdAndWorkspace_Id(Long id, Long workspaceId);
    long countByWorkspace_Id(Long workspaceId);
    long countByWorkspace_IdAndStatus(Long workspaceId, String status);
}
