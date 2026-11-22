package com.aurora.audit;
import org.springframework.stereotype.Service;
import com.aurora.security.WorkspaceContextHolder;
import java.util.List;
@Service
public class AuditService {
    private final AuditRepository repository;
    public AuditService(AuditRepository repository){this.repository=repository;}
    public void record(String type,String entityType,String entityId,String details){
        AuditEvent event = new AuditEvent(type,entityType,entityId,details);
        event.assignWorkspace(WorkspaceContextHolder.require());
        repository.save(event);
    }
    public List<AuditEvent> recent(){return repository.findTop30ByWorkspace_IdOrderByCreatedAtDesc(WorkspaceContextHolder.id());}
}
