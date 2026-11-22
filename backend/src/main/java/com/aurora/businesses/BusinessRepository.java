package com.aurora.businesses;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface BusinessRepository extends JpaRepository<BusinessWorkspace,Long>{List<BusinessWorkspace> findAllByWorkspace_IdOrderByCreatedAtDesc(Long workspaceId); java.util.Optional<BusinessWorkspace> findByIdAndWorkspace_Id(Long id, Long workspaceId); long countByWorkspace_Id(Long workspaceId);}
