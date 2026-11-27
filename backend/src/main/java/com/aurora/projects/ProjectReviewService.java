package com.aurora.projects;

import com.aurora.audit.AuditService;
import com.aurora.identity.WorkspaceRole;
import com.aurora.security.WorkspaceContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

@Service
public class ProjectReviewService {
    private final VentureProjectRepository projects; private final ProjectArtifactRepository artifacts;
    private final ProjectReviewRepository reviews; private final ProjectArtifactVersionRepository versions; private final AuditService audit;
    @org.springframework.beans.factory.annotation.Value("${aurora.projects.storage-path:./data/project-workspaces}") private String storagePath;
    public ProjectReviewService(VentureProjectRepository projects, ProjectArtifactRepository artifacts, ProjectReviewRepository reviews, ProjectArtifactVersionRepository versions, AuditService audit) {
        this.projects=projects; this.artifacts=artifacts; this.reviews=reviews; this.versions=versions; this.audit=audit;
    }
    private VentureProject project(Long id) { return projects.findByIdAndWorkspace_Id(id, WorkspaceContextHolder.id()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Project not found.")); }
    private ProjectArtifact artifact(Long projectId, Long artifactId) { return artifacts.findById(artifactId).filter(a -> a.getProjectId().equals(projectId)).filter(a -> projects.existsByIdAndWorkspace_Id(projectId,WorkspaceContextHolder.id())).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Project artifact not found.")); }
    private Path safeFile(Long projectId, String relative) {
        Path root=Paths.get(storagePath).toAbsolutePath().normalize().resolve("ws-"+WorkspaceContextHolder.id()).resolve("project-"+projectId).normalize();
        Path file=root.resolve(relative).normalize(); if(!file.startsWith(root)||Files.isSymbolicLink(root)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Unsafe artifact path.");
        Path cursor=root; for(Path part:root.relativize(file)){cursor=cursor.resolve(part);if(Files.isSymbolicLink(cursor))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Symbolic links are not permitted.");}
        return file;
    }
    private String hash(Path file) throws IOException { try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file))); } catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);} }
    @Transactional(readOnly=true) public List<ProjectReview> list(Long projectId) { project(projectId); return reviews.findAllByProject_IdAndWorkspace_IdOrderByCreatedAtDesc(projectId,WorkspaceContextHolder.id()); }
    @Transactional public ProjectReview request(Long projectId, Long artifactId, String actorEmail) {
        WorkspaceContextHolder.requireAnyRole(WorkspaceRole.OWNER,WorkspaceRole.ADMIN,WorkspaceRole.MEMBER);
        VentureProject p=project(projectId); ProjectArtifact a=artifact(projectId,artifactId); Path file=safeFile(projectId,a.getRelativePath());
        try { if(!Files.isRegularFile(file)) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Artifact file is missing."); String hash=hash(file);
            if(reviews.existsByArtifact_IdAndArtifactSha256AndStatus(artifactId,hash,"PENDING_REVIEW")) throw new ResponseStatusException(HttpStatus.CONFLICT,"This exact artifact version is already awaiting review.");
            String content=Files.readString(file,StandardCharsets.UTF_8);
            ProjectArtifactVersion pinned=versions.findFirstByArtifact_IdAndWorkspace_IdAndSha256OrderByVersionNumberDesc(a.getId(),WorkspaceContextHolder.id(),hash).orElseGet(()->{
                int next=versions.countByArtifact_IdAndWorkspace_Id(a.getId(),WorkspaceContextHolder.id())+1;
                return versions.save(new ProjectArtifactVersion(WorkspaceContextHolder.require(),p,a,next,content,hash,actorEmail));
            });
            String requester=actorEmail; ProjectReview saved=reviews.save(new ProjectReview(WorkspaceContextHolder.require(),p,a,pinned,hash,requester));
            audit.record("PROJECT_REVIEW_REQUESTED","PROJECT_REVIEW",saved.getId().toString(),"Review requested for "+a.getName()+" version "+pinned.getVersionNumber()+" (SHA-256 "+hash+")."); return saved;
        } catch(IOException e){throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"Could not read artifact for review.");}
    }
    @Transactional public ProjectReview decide(Long projectId, Long reviewId, DecisionRequest request, String actorEmail) {
        WorkspaceContextHolder.requireAnyRole(WorkspaceRole.OWNER,WorkspaceRole.ADMIN);
        project(projectId);
        String decision=request.decision().trim().toUpperCase(); if(!List.of("APPROVED","CHANGES_REQUESTED","REJECTED").contains(decision)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Decision must be APPROVED, CHANGES_REQUESTED or REJECTED.");
        String note=request.feedback()==null?"":request.feedback().trim(); if(!decision.equals("APPROVED")&&note.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Feedback is required when requesting changes or rejecting work.");
        if(note.length()>4000) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Feedback is too long.");
        ProjectReview review=reviews.findByIdAndProject_IdAndWorkspace_Id(reviewId,projectId,WorkspaceContextHolder.id()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Review not found."));
        try { Path file=safeFile(projectId,review.getArtifactPath()); if(!Files.isRegularFile(file)||!hash(file).equals(review.getArtifactSha256())) throw new ResponseStatusException(HttpStatus.CONFLICT,"This artifact changed after review was requested. Request a new review of the latest version."); }
        catch(IOException e){throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"Could not verify reviewed artifact version.");}
        try { review.decide(decision,actorEmail,note); } catch(IllegalStateException e){throw new ResponseStatusException(HttpStatus.CONFLICT,e.getMessage());}
        ProjectReview saved=reviews.save(review); audit.record("PROJECT_REVIEW_"+decision,"PROJECT_REVIEW",reviewId.toString(),"Review "+decision+" for "+review.getArtifactName()+". "+note); return saved;
    }
    public record DecisionRequest(String decision,String feedback) {}
}
