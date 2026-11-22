package com.aurora.businesses;
import com.aurora.audit.AuditService;
import com.aurora.security.WorkspaceContextHolder;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
@RestController @RequestMapping("/api/v1/businesses")
public class BusinessController {
 private final BusinessRepository businesses; private final AuditService audit;
 public BusinessController(BusinessRepository businesses,AuditService audit){this.businesses=businesses;this.audit=audit;}
 public record BusinessRequest(@NotBlank @Size(max=140) String name,@Size(max=2500) String description,@Size(max=1000) String goal,String status){}
 @GetMapping public List<BusinessWorkspace> list(){return businesses.findAllByWorkspace_IdOrderByCreatedAtDesc(WorkspaceContextHolder.id());}
 @PostMapping @ResponseStatus(HttpStatus.CREATED)
 public BusinessWorkspace create(@Valid @RequestBody BusinessRequest request){
  WorkspaceContextHolder.requireAnyRole(com.aurora.identity.WorkspaceRole.OWNER, com.aurora.identity.WorkspaceRole.ADMIN);
  String status=validStatus(request.status());
  BusinessWorkspace item=new BusinessWorkspace(request.name().trim(),request.description(),request.goal());
  item.update(item.getName(),item.getDescription(),item.getGoal(),status);
  item.assignWorkspace(WorkspaceContextHolder.require());
  BusinessWorkspace saved=businesses.save(item);
  audit.record("BUSINESS_CREATED","BUSINESS",saved.getId().toString(),"Created business workspace: "+saved.getName()); return saved;
 }
 @PutMapping("/{id}")
 public BusinessWorkspace update(@PathVariable Long id,@Valid @RequestBody BusinessRequest request){
  WorkspaceContextHolder.requireAnyRole(com.aurora.identity.WorkspaceRole.OWNER, com.aurora.identity.WorkspaceRole.ADMIN);
  BusinessWorkspace item=businesses.findByIdAndWorkspace_Id(id, WorkspaceContextHolder.id()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
  item.update(request.name().trim(),request.description(),request.goal(),validStatus(request.status()));
  item.assignWorkspace(WorkspaceContextHolder.require());
  BusinessWorkspace saved=businesses.save(item); audit.record("BUSINESS_UPDATED","BUSINESS",id.toString(),"Updated business workspace: "+saved.getName()); return saved;
 }
 private String validStatus(String value){
  String status=value==null?"IDEA":value.toUpperCase();
  if(!List.of("IDEA","RESEARCH","VALIDATION","ACTIVE","PAUSED","ARCHIVED").contains(status)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid business status");
  return status;
 }
}
