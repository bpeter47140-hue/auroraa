package com.aurora.identity;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final IdentityAuthService auth;

    public AuthController(IdentityAuthService auth) { this.auth = auth; }

    @GetMapping("/bootstrap")
    public AuthDtos.BootstrapStatus bootstrap() {
        return new AuthDtos.BootstrapStatus(auth.isRegistrationOpen());
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody AuthDtos.RegisterRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(auth.registerFirstOwner(request));
        } catch (IdentityAuthService.AuthException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(ex.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthDtos.LoginRequest request) {
        try {
            return ResponseEntity.ok(auth.login(request));
        } catch (IdentityAuthService.AuthException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(ex.getMessage()));
        }
    }


    @GetMapping("/me")
    public ResponseEntity<AuthDtos.CurrentUserResponse> me(HttpServletRequest request) {
        Object value = request.getAttribute("aurora.authenticatedUser");
        if (!(value instanceof UserAccount user)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(new AuthDtos.CurrentUserResponse(
            new AuthDtos.UserSummary(user.getId(), user.getEmail(), user.getDisplayName()),
            auth.listWorkspaces(user.getId())
        ));
    }

    @PostMapping("/password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody AuthDtos.ChangePasswordRequest request,
                                            HttpServletRequest httpRequest) {
        try {
            auth.changePassword(bearerToken(httpRequest), request.currentPassword(), request.newPassword());
            return ResponseEntity.noContent().build();
        } catch (IdentityAuthService.AuthException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(ex.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        String token = bearerToken(request);
        auth.logout(token);
        return ResponseEntity.noContent().build();
    }

    private static String bearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        return header != null && header.startsWith("Bearer ") ? header.substring(7) : null;
    }

    public record ErrorResponse(String error) {}
}
