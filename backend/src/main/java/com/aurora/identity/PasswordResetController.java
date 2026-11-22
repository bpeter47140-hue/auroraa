package com.aurora.identity;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth/password")
public class PasswordResetController {
    private final PasswordResetService resets;

    public PasswordResetController(PasswordResetService resets) { this.resets = resets; }

    public record ResetRequest(@NotBlank @Email @Size(max = 254) String email) {}
    public record CompleteRequest(@NotBlank @Size(max = 100) String token,
                                  @NotBlank @Size(min = 12, max = 72) String newPassword) {}
    public record GenericResponse(String message) {}

    @PostMapping("/reset-request")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public GenericResponse request(@Valid @RequestBody ResetRequest request) {
        resets.requestReset(request.email());
        return new GenericResponse("If an active account exists for that address, a password reset link will be sent.");
    }

    @PostMapping("/reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void complete(@Valid @RequestBody CompleteRequest request) {
        resets.completeReset(request.token(), request.newPassword());
    }
}
