package com.aurora.research;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface CustomerInsightRepository extends JpaRepository<CustomerInsight,Long> {
    List<CustomerInsight> findTop200ByWorkspace_IdOrderByCreatedAtDesc(Long workspaceId);
    java.util.Optional<CustomerInsight> findByIdAndWorkspace_Id(Long id, Long workspaceId);
}
