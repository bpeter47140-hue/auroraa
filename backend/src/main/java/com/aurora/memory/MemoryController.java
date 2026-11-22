package com.aurora.memory;

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
@RequestMapping("/api/v1/memory")
public class MemoryController {
    private final MemoryRepository memories;
    private final AuditService audit;
    public MemoryController(MemoryRepository memories, AuditService audit) { this.memories=memories; this.audit=audit; }

    public record MemoryRequest(@NotBlank @Size(max=100) String category,
                                @NotBlank @Size(max=180) String title,
                                @NotBlank @Size(max=3000) String content,
                                @Size(max=180) String source) {}

    @GetMapping public List<MemoryEntry> list() { return memories.findTop100ByWorkspace_IdOrderByCreatedAtDesc(WorkspaceContextHolder.id()); }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public MemoryEntry create(@Valid @RequestBody MemoryRequest request) {
        WorkspaceContextHolder.requireAnyRole(com.aurora.identity.WorkspaceRole.OWNER, com.aurora.identity.WorkspaceRole.ADMIN);
        String category=request.category().trim().toUpperCase();
        if (!List.of("DECISION","FINDING","PREFERENCE","CUSTOMER_FEEDBACK","RISK","GENERAL").contains(category))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Unsupported memory category.");
        MemoryEntry item=new MemoryEntry(category,request.title().trim(),request.content().trim(),
                request.source()==null?null:request.source().trim());
        item.assignWorkspace(WorkspaceContextHolder.require());
        MemoryEntry saved=memories.save(item);
        audit.record("MEMORY_SAVED","MEMORY",saved.getId().toString(),"Saved memory: "+saved.getTitle());
        return saved;
    }

    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        WorkspaceContextHolder.requireAnyRole(com.aurora.identity.WorkspaceRole.OWNER, com.aurora.identity.WorkspaceRole.ADMIN);
        MemoryEntry entry=memories.findByIdAndWorkspace_Id(id, WorkspaceContextHolder.id()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        memories.delete(entry);
        audit.record("MEMORY_DELETED","MEMORY",id.toString(),"Deleted memory: "+entry.getTitle());
    }
}
