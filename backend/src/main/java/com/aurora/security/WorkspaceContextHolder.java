package com.aurora.security;

import com.aurora.identity.TenantWorkspace;
import com.aurora.identity.WorkspaceRole;
import java.util.Arrays;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class WorkspaceContextHolder {
    private static final ThreadLocal<TenantWorkspace> CURRENT = new ThreadLocal<>();
    private static final ThreadLocal<WorkspaceRole> ROLE = new ThreadLocal<>();
    private WorkspaceContextHolder() {}
    public static void set(TenantWorkspace workspace, WorkspaceRole role) { CURRENT.set(workspace); ROLE.set(role); }
    public static WorkspaceRole role() {
        WorkspaceRole role = ROLE.get();
        if (role == null) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No workspace role is available.");
        return role;
    }
    public static void requireAnyRole(WorkspaceRole... allowed) {
        WorkspaceRole current = role();
        if (Arrays.stream(allowed).noneMatch(current::equals))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Your workspace role does not allow this action.");
    }
    public static TenantWorkspace require() {
        TenantWorkspace workspace = CURRENT.get();
        if (workspace == null) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No active workspace selected.");
        return workspace;
    }
    public static Long id() { return require().getId(); }
    public static void clear() { CURRENT.remove(); ROLE.remove(); }
}
