package com.aurora.research;

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
@RequestMapping("/api/v1/customer-insights")
public class CustomerResearchController {
    private final CustomerInsightRepository insights;
    private final AuditService audit;
    public CustomerResearchController(CustomerInsightRepository insights, AuditService audit) { this.insights=insights; this.audit=audit; }

    public record InsightRequest(
        @NotBlank @Size(max=40) String insightType,
        @NotBlank @Size(max=100) String participantLabel,
        @NotBlank @Size(max=2000) String problem,
        @Size(max=1500) String currentSolution,
        @Size(max=1000) String frequency,
        @Size(max=1000) String willingness,
        @Size(max=2000) String evidence,
        @NotBlank @Size(max=30) String validationStatus
    ) {}

    @GetMapping public List<CustomerInsight> list(){ return insights.findTop200ByWorkspace_IdOrderByCreatedAtDesc(WorkspaceContextHolder.id()); }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public CustomerInsight create(@Valid @RequestBody InsightRequest r) {
        WorkspaceContextHolder.requireAnyRole(com.aurora.identity.WorkspaceRole.OWNER, com.aurora.identity.WorkspaceRole.ADMIN, com.aurora.identity.WorkspaceRole.MEMBER);
        String type=r.insightType().trim().toUpperCase(), status=r.validationStatus().trim().toUpperCase();
        if (!List.of("PROBLEM","CURRENT_SOLUTION","OBJECTION","FEATURE_REQUEST","OTHER").contains(type))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Unsupported insight type.");
        if (!List.of("UNVERIFIED","REPORTED","OBSERVED").contains(status))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Unsupported evidence status.");
        CustomerInsight item=new CustomerInsight(type,r.participantLabel().trim(),r.problem().trim(),
            clean(r.currentSolution()),clean(r.frequency()),clean(r.willingness()),clean(r.evidence()),status);
        item.assignWorkspace(WorkspaceContextHolder.require());
        CustomerInsight saved=insights.save(item);
        audit.record("CUSTOMER_INSIGHT_SAVED","CUSTOMER_INSIGHT",saved.getId().toString(),"Saved "+type+" insight from "+saved.getParticipantLabel());
        return saved;
    }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id){
        WorkspaceContextHolder.requireAnyRole(com.aurora.identity.WorkspaceRole.OWNER, com.aurora.identity.WorkspaceRole.ADMIN);
        CustomerInsight item=insights.findByIdAndWorkspace_Id(id, WorkspaceContextHolder.id()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        insights.delete(item); audit.record("CUSTOMER_INSIGHT_DELETED","CUSTOMER_INSIGHT",id.toString(),"Deleted customer insight #"+id);
    }
    private String clean(String s){return s==null||s.isBlank()?null:s.trim();}
}
