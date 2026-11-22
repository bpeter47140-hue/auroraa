package com.aurora.approvals;

import com.aurora.audit.AuditService;
import com.aurora.security.WorkspaceContextHolder;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/approvals")
public class ApprovalController {
    private final ApprovalRepository approvals;
    private final AuditService audit;
    public ApprovalController(ApprovalRepository approvals,AuditService audit){this.approvals=approvals;this.audit=audit;}

    public record CreateApprovalRequest(@NotBlank @Size(max=160) String title,
        @NotBlank @Size(max=2000) String description,@NotBlank @Size(max=40) String actionType){}
    public record DecisionRequest(@Size(max=1000) String note){}

    @GetMapping public List<ApprovalRequest> list(){return approvals.findAllByWorkspace_IdOrderByCreatedAtDesc(WorkspaceContextHolder.id());}

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public ApprovalRequest create(@Valid @RequestBody CreateApprovalRequest request){
        WorkspaceContextHolder.requireAnyRole(com.aurora.identity.WorkspaceRole.OWNER, com.aurora.identity.WorkspaceRole.ADMIN, com.aurora.identity.WorkspaceRole.MEMBER);
        ApprovalRequest item=new ApprovalRequest(request.title().trim(),request.description().trim(),request.actionType().toUpperCase()); item.assignWorkspace(WorkspaceContextHolder.require()); ApprovalRequest saved=approvals.save(item);
        audit.record("APPROVAL_REQUESTED","APPROVAL",saved.getId().toString(),"Approval requested: "+saved.getTitle());
        return saved;
    }

    @PostMapping("/{id}/approve")
    public ApprovalRequest approve(@PathVariable Long id,@RequestBody(required=false) DecisionRequest request){
        return decide(id,"APPROVED",request);
    }
    @PostMapping("/{id}/reject")
    public ApprovalRequest reject(@PathVariable Long id,@RequestBody(required=false) DecisionRequest request){
        return decide(id,"REJECTED",request);
    }
    private ApprovalRequest decide(Long id,String decision,DecisionRequest request){
        WorkspaceContextHolder.requireAnyRole(com.aurora.identity.WorkspaceRole.OWNER, com.aurora.identity.WorkspaceRole.ADMIN);
        ApprovalRequest item=approvals.findByIdAndWorkspace_Id(id, WorkspaceContextHolder.id()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        if(!"PENDING".equals(item.getStatus())) throw new ResponseStatusException(HttpStatus.CONFLICT,"This request has already been decided");
        String note=request==null?null:request.note();
        item.decide(decision,note);
        ApprovalRequest saved=approvals.save(item);
        audit.record("APPROVAL_"+decision,"APPROVAL",id.toString(),"Owner "+decision.toLowerCase()+" request: "+item.getTitle());
        return saved;
    }
}
