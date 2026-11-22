package com.aurora.identity;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

public final class AuthDtos {
    private AuthDtos() {}

    public record RegisterRequest(
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 12, max = 72) String password,
        @NotBlank @Size(min = 2, max = 100) String displayName,
        @NotBlank @Size(min = 2, max = 140) String workspaceName
    ) {}

    public record LoginRequest(
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(max = 72) String password
    ) {}

    public record ChangePasswordRequest(@NotBlank @Size(max = 72) String currentPassword, @NotBlank @Size(min = 12, max = 72) String newPassword) {}
    public record WorkspaceSummary(Long id, String name, WorkspaceRole role) {}
    public record CurrentUserResponse(UserSummary user, List<WorkspaceSummary> workspaces) {}
    public record UserSummary(Long id, String email, String displayName) {}
    public record AuthResponse(String token, Instant expiresAt, UserSummary user,
                               List<WorkspaceSummary> workspaces) {}
    public record BootstrapStatus(boolean registrationOpen) {}
}
