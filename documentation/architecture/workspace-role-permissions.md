# Workspace role permissions

Roles are taken from the authenticated user's active workspace membership. A client cannot submit or override its role. The session authentication filter places the selected membership role into request-scoped workspace context.

| Capability | OWNER | ADMIN | MEMBER | VIEWER |
|---|---:|---:|---:|---:|
| Read workspace data, dashboard, tasks, approvals, memory, research and audit | Yes | Yes | Yes | Yes |
| Create/update tasks | Yes | Yes | Yes | No |
| Delete tasks | Yes | Yes | No | No |
| Create approval requests | Yes | Yes | Yes | No |
| Approve/reject requests | Yes | Yes | No | No |
| Create/delete shared memory | Yes | Yes | No | No |
| Add customer research | Yes | Yes | Yes | No |
| Delete customer research | Yes | Yes | No | No |
| Create/update business workspaces | Yes | Yes | No | No |
| Run AI agents and validation workflows | Yes | Yes | Yes | No |
| Manage workspace membership, roles, ownership or closure | Owner-only; not exposed yet | No | No | No |

## Enforcement notes

- Authorization is checked in controller methods for write/decision actions; list/read endpoints remain available to all active members.
- The role is loaded from the database membership selected by the authentication filter. `X-Aurora-Workspace` selects a workspace only when membership exists; it never supplies a role.
- Workspace membership management and invitations are intentionally not exposed yet.
- This is an initial RBAC layer. Before external users, add HTTP-level tests for every role/endpoint, review any future endpoints for explicit authorization, and implement owner-only membership management with audit events.
