package com.aurora.security;

import com.aurora.identity.IdentityAuthService;
import com.aurora.identity.UserAccount;
import com.aurora.identity.WorkspaceMembership;
import com.aurora.identity.WorkspaceMembershipRepository;
import com.aurora.identity.TenantWorkspace;
import com.aurora.identity.WorkspaceStatus;
import java.util.List;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Profile("!legacy-owner-token")
@ConditionalOnProperty(name = "aurora.security.sessions.enabled", havingValue = "true", matchIfMissing = true)
public class SessionAuthFilter extends OncePerRequestFilter {
    private final IdentityAuthService auth;
    private final WorkspaceMembershipRepository memberships;

    public SessionAuthFilter(IdentityAuthService auth, WorkspaceMembershipRepository memberships) { this.auth = auth; this.memberships = memberships; }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return "/api/v1/auth/bootstrap".equals(path)
            || "/api/v1/auth/register".equals(path)
            || "/api/v1/auth/login".equals(path)
            || "/api/v1/auth/logout".equals(path)
            || "/api/v1/workspace/invitations/register".equals(path)
            || "/api/v1/system/status".equals(path)
            || "/actuator/health".equals(path)
            || path.startsWith("/actuator/health/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        securityHeaders(response);
        String header = request.getHeader("Authorization");
        String token = header != null && header.startsWith("Bearer ") ? header.substring(7) : null;
        UserAccount user = auth.authenticate(token);
        if (user == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"error\":\"Authentication required\"}");
            return;
        }
        request.setAttribute("aurora.authenticatedUser", user);
        if ("/api/v1/workspace/invitations/accept".equals(request.getRequestURI())) {
            WorkspaceContextHolder.clear();
            try { chain.doFilter(request, response); }
            finally { WorkspaceContextHolder.clear(); }
            return;
        }
        List<WorkspaceMembership> activeMemberships = memberships.findAllByUserIdOrderByCreatedAtAsc(user.getId()).stream()
            .filter(m -> m.getWorkspace().getStatus() == WorkspaceStatus.ACTIVE).toList();
        String requestedWorkspace = request.getHeader("X-Aurora-Workspace");
        WorkspaceMembership selected = null;
        if (requestedWorkspace != null && !requestedWorkspace.isBlank()) {
            try {
                Long workspaceId = Long.valueOf(requestedWorkspace);
                selected = memberships.findByUserIdAndWorkspaceId(user.getId(), workspaceId)
                    .filter(m -> m.getWorkspace().getStatus() == WorkspaceStatus.ACTIVE).orElse(null);
            } catch (NumberFormatException ignored) { }
        } else if (!activeMemberships.isEmpty()) { selected = activeMemberships.get(0); }
        if (selected == null) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"error\":\"No access to the selected workspace\"}");
            return;
        }
        request.setAttribute("aurora.authenticatedUser", user);
        request.setAttribute("aurora.workspace", selected.getWorkspace());
        WorkspaceContextHolder.set(selected.getWorkspace(), selected.getRole());
        try { chain.doFilter(request, response); }
        finally { WorkspaceContextHolder.clear(); }
    }

    private static void securityHeaders(HttpServletResponse response) {
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Referrer-Policy", "no-referrer");
        response.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=()");
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Pragma", "no-cache");
    }
}
