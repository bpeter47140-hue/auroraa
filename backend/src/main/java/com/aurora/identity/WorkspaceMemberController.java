package com.aurora.identity;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/v1/workspace")
public class WorkspaceMemberController {
 private final WorkspaceMemberService service;
 private final InvitationEmailService emailService;
 public WorkspaceMemberController(WorkspaceMemberService service, InvitationEmailService emailService){this.service=service;this.emailService=emailService;}
 public record InviteRequest(@Email @NotBlank @Size(max=254) String email,@NotNull WorkspaceRole role){}
 public record RoleRequest(@NotNull WorkspaceRole role){}
 public record AcceptRequest(@NotBlank String token){}
 public record RegisterAcceptRequest(@NotBlank String token,@NotBlank String displayName,@NotBlank String password){}
 @GetMapping("/members") public List<WorkspaceMemberService.MemberView> members(){return service.members();}
 @GetMapping("/invitations") public List<WorkspaceMemberService.InvitationView> invitations(){return service.invitations();}
 public record InvitationCreatedResponse(WorkspaceMemberService.InvitationView invitation,String invitationToken,boolean emailSent){}
 @PostMapping("/invitations") @ResponseStatus(HttpStatus.CREATED)
 public InvitationCreatedResponse invite(@Valid @RequestBody InviteRequest r){
  WorkspaceMemberService.CreatedInvitation created=service.invite(r.email(),r.role());
  boolean sent=emailService.sendInvitation(created.invitation().email(),r.role(),created.invitationToken(),created.invitation().expiresAt());
  return new InvitationCreatedResponse(created.invitation(),sent?null:created.invitationToken(),sent);
 }
 @PostMapping("/invitations/accept") public WorkspaceMemberService.MemberView accept(@Valid @RequestBody AcceptRequest r,HttpServletRequest request){Object user=request.getAttribute("aurora.authenticatedUser");if(!(user instanceof UserAccount account))throw new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED);return service.accept(r.token(),account);}
 @PostMapping("/invitations/register") @ResponseStatus(HttpStatus.CREATED) public WorkspaceMemberService.MemberView acceptAndRegister(@Valid @RequestBody RegisterAcceptRequest r){return service.acceptAndRegister(r.token(),r.displayName(),r.password());}
 @PostMapping("/invitations/{id}/revoke") @ResponseStatus(HttpStatus.NO_CONTENT) public void revoke(@PathVariable Long id){service.revoke(id);}
 @PatchMapping("/members/{id}/role") @ResponseStatus(HttpStatus.NO_CONTENT) public void role(@PathVariable Long id,@Valid @RequestBody RoleRequest r){service.changeRole(id,r.role());}
 @DeleteMapping("/members/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void remove(@PathVariable Long id){service.remove(id);}
}
