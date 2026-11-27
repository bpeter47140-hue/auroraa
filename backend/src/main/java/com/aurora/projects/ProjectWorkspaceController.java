package com.aurora.projects;

import com.aurora.identity.WorkspaceRole;
import com.aurora.security.WorkspaceContextHolder;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/workspace")
public class ProjectWorkspaceController {
 private final ProjectWorkspaceService service;
 private final ProjectPreviewService previews;
 public ProjectWorkspaceController(ProjectWorkspaceService service,ProjectPreviewService previews){this.service=service;this.previews=previews;}
 public record GenerateRequest(@NotBlank @Size(max=5000) String brief){}
 public record SaveFileRequest(@NotBlank @Size(max=50000) String path,@Size(max=50000) String content){}
 private void readAuth(){WorkspaceContextHolder.requireAnyRole(WorkspaceRole.OWNER,WorkspaceRole.ADMIN,WorkspaceRole.MEMBER,WorkspaceRole.VIEWER);}
 @PostMapping("/scaffold") public List<ProjectArtifact> scaffold(@PathVariable Long projectId){WorkspaceContextHolder.requireAnyRole(WorkspaceRole.OWNER,WorkspaceRole.ADMIN,WorkspaceRole.MEMBER);return service.scaffold(projectId);}
 @PostMapping("/generate") public ProjectWorkspaceService.GenerationResult generate(@PathVariable Long projectId,@Valid @RequestBody GenerateRequest request){WorkspaceContextHolder.requireAnyRole(WorkspaceRole.OWNER,WorkspaceRole.ADMIN,WorkspaceRole.MEMBER);return service.generate(projectId,request.brief());}
 @PostMapping("/build") public ProjectPreviewService.BuildResult build(@PathVariable Long projectId){WorkspaceContextHolder.requireAnyRole(WorkspaceRole.OWNER,WorkspaceRole.ADMIN,WorkspaceRole.MEMBER);return previews.build(projectId);}
 @PostMapping("/preview-access") public ProjectPreviewService.PreviewAccess previewAccess(@PathVariable Long projectId){readAuth();return previews.createAccess(projectId);}
 @GetMapping("/preview/{token}/{*path}") public ResponseEntity<byte[]> preview(@PathVariable Long projectId,@PathVariable String token,@PathVariable String path){
  ProjectPreviewService.PreviewFile file=previews.file(projectId,token,path);
  return ResponseEntity.ok().contentType(MediaType.parseMediaType(file.contentType()+(file.contentType().startsWith("text/")||file.contentType().contains("javascript")?";charset=UTF-8":"")))
   .header(HttpHeaders.CACHE_CONTROL,"no-store").header("X-Content-Type-Options","nosniff")
   .header("Referrer-Policy","no-referrer")
   .header("Content-Security-Policy","sandbox allow-scripts; default-src 'self' data: blob:; img-src 'self' data: blob:; style-src 'self' 'unsafe-inline'; font-src 'self' data:; script-src 'self'; connect-src 'none'; form-action 'none'; object-src 'none'; base-uri 'none'; frame-ancestors *")
   .body(file.content());
 }
 @GetMapping("/artifacts") public List<ProjectArtifact> artifacts(@PathVariable Long projectId){readAuth();return service.list(projectId);}
 @GetMapping("/files") public List<ProjectWorkspaceService.WorkspaceFile> files(@PathVariable Long projectId){readAuth();return service.readFiles(projectId);}
 @PutMapping("/file") public ProjectWorkspaceService.WorkspaceFile saveFile(@PathVariable Long projectId,@Valid @RequestBody SaveFileRequest request){return service.saveFile(projectId,request.path(),request.content());}
 @GetMapping("/artifacts/{artifactId}/versions") public List<ProjectWorkspaceService.ArtifactVersionSummary> versions(@PathVariable Long projectId,@PathVariable Long artifactId){readAuth();return service.versionHistory(projectId,artifactId);}
 @GetMapping("/artifacts/{artifactId}/versions/{versionId}") public ProjectWorkspaceService.ArtifactVersionDetail version(@PathVariable Long projectId,@PathVariable Long artifactId,@PathVariable Long versionId){readAuth();return service.version(projectId,artifactId,versionId);}
 @GetMapping("/export.zip") public ResponseEntity<byte[]> export(@PathVariable Long projectId){readAuth();byte[] data=service.exportZip(projectId);return ResponseEntity.ok().contentType(MediaType.parseMediaType("application/zip")).header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=aurora-project-"+projectId+".zip").body(data);}
}
