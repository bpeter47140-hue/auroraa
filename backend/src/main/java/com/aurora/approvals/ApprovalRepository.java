package com.aurora.approvals;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ApprovalRepository extends JpaRepository<ApprovalRequest,Long> {
    List<ApprovalRequest> findAllByWorkspace_IdOrderByCreatedAtDesc(Long workspaceId);
    java.util.Optional<ApprovalRequest> findByIdAndWorkspace_Id(Long id, Long workspaceId);
    long countByWorkspace_IdAndStatus(Long workspaceId, String status);
}
