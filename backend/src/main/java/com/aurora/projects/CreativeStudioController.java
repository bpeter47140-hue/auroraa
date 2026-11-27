package com.aurora.projects;

import com.aurora.identity.UserAccount;
import com.aurora.identity.WorkspaceRole;
import com.aurora.security.WorkspaceContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Size;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/creative-assets")
public class CreativeStudioController {
    private final CreativeStudioService service;
    public CreativeStudioController(CreativeStudioService service){this.service=service;}
    public record AssetView(Long id, Long projectId, String name, String mediaType, String contentType, long sizeBytes, String notes, String uploadedBy, java.time.Instant createdAt) {}
    public record AssetFile(byte[] content, String contentType, String name) {}
    @GetMapping public List<AssetView> list(@PathVariable Long projectId){read();return service.list(projectId).stream().map(CreativeStudioController::view).toList();}
    @PostMapping(consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @ResponseStatus(HttpStatus.CREATED)
    public AssetView upload(@PathVariable Long projectId, @RequestPart("file") MultipartFile file, @RequestPart(value="notes",required=false) @Size(max=1000) String notes, HttpServletRequest request){
        WorkspaceContextHolder.requireAnyRole(WorkspaceRole.OWNER,WorkspaceRole.ADMIN,WorkspaceRole.MEMBER);
        Object actor=request.getAttribute("aurora.authenticatedUser");
        if(!(actor instanceof UserAccount user)) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Authentication required.");
        return view(service.upload(projectId,file,notes,user.getEmail()));
    }
    @GetMapping("/{assetId}/file") public ResponseEntity<byte[]> file(@PathVariable Long projectId,@PathVariable Long assetId){
        read(); AssetFile file=service.file(projectId,assetId);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(file.contentType())).header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.inline().filename(file.name()).build().toString()).header("X-Content-Type-Options","nosniff").header(HttpHeaders.CACHE_CONTROL,"private, no-store").body(file.content());
    }
    @DeleteMapping("/{assetId}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable Long projectId,@PathVariable Long assetId){WorkspaceContextHolder.requireAnyRole(WorkspaceRole.OWNER,WorkspaceRole.ADMIN,WorkspaceRole.MEMBER);service.delete(projectId,assetId);}
    private void read(){WorkspaceContextHolder.requireAnyRole(WorkspaceRole.OWNER,WorkspaceRole.ADMIN,WorkspaceRole.MEMBER,WorkspaceRole.VIEWER);}
    private static AssetView view(CreativeAsset a){return new AssetView(a.getId(),a.getProjectId(),a.getName(),a.getMediaType(),a.getContentType(),a.getSizeBytes(),a.getNotes(),a.getUploadedBy(),a.getCreatedAt());}
}