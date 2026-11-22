package com.aurora.memory;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MemoryRepository extends JpaRepository<MemoryEntry, Long> {
    List<MemoryEntry> findTop100ByWorkspace_IdOrderByCreatedAtDesc(Long workspaceId);
    java.util.Optional<MemoryEntry> findByIdAndWorkspace_Id(Long id, Long workspaceId);
}
