package com.aurora.projects;
import com.aurora.identity.WorkspaceRole;
import com.aurora.security.WorkspaceContextHolder;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import com.aurora.identity.UserAccount;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
@RestController
@RequestMapping("/api/v1/projects/{projectId}/reviews")
public class ProjectReviewController {
 private final ProjectReviewService service;
 public ProjectReviewController(ProjectReviewService service){this.service=service;}
 public record RequestReview(@jakarta.validation.constraints.NotNull Long artifactId){}
 public record Decision(@NotBlank String decision,@Size(max=4000) String feedback){}
 @GetMapping public List<ProjectReview> list(@PathVariable Long projectId){WorkspaceContextHolder.requireAnyRole(WorkspaceRole.OWNER,WorkspaceRole.ADMIN,WorkspaceRole.MEMBER,WorkspaceRole.VIEWER);return service.list(projectId);}
 @PostMapping @ResponseStatus(HttpStatus.CREATED) public ProjectReview request(@PathVariable Long projectId,@Valid @RequestBody RequestReview body,HttpServletRequest request){return service.request(projectId,body.artifactId(),actor(request));}
 @PostMapping("/{reviewId}/decision") public ProjectReview decide(@PathVariable Long projectId,@PathVariable Long reviewId,@Valid @RequestBody Decision body,HttpServletRequest request){return service.decide(projectId,reviewId,new ProjectReviewService.DecisionRequest(body.decision(),body.feedback()),actor(request));}
 private String actor(HttpServletRequest request){Object value=request.getAttribute("aurora.authenticatedUser");if(!(value instanceof UserAccount user))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Authentication required.");return user.getEmail();}
}
