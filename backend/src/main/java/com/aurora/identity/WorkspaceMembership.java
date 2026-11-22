package com.aurora.identity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "workspace_memberships",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_workspace_membership_user_workspace",
        columnNames = {"workspace_id", "user_id"}
    ),
    indexes = {
        @Index(name = "idx_membership_user", columnList = "user_id"),
        @Index(name = "idx_membership_workspace", columnList = "workspace_id")
    }
)
public class WorkspaceMembership {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false, foreignKey = @ForeignKey(name = "fk_membership_workspace"))
    private TenantWorkspace workspace;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_membership_user"))
    private UserAccount user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private WorkspaceRole role;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected WorkspaceMembership() {}

    public WorkspaceMembership(TenantWorkspace workspace, UserAccount user, WorkspaceRole role) {
        this.workspace = workspace;
        this.user = user;
        this.role = role == null ? WorkspaceRole.MEMBER : role;
    }

    @PrePersist
    void onCreate() { createdAt = Instant.now(); }

    public Long getId() { return id; }
    public TenantWorkspace getWorkspace() { return workspace; }
    public UserAccount getUser() { return user; }
    public WorkspaceRole getRole() { return role; }
    public Instant getCreatedAt() { return createdAt; }
    public void changeRole(WorkspaceRole role) {
        if (role == null) throw new IllegalArgumentException("A workspace role is required.");
        this.role = role;
    }
}
