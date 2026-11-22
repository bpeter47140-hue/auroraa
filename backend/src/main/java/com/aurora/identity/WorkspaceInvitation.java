package com.aurora.identity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="workspace_invitations", indexes={@Index(name="idx_invitation_workspace",columnList="workspace_id"),@Index(name="idx_invitation_email",columnList="email")}, uniqueConstraints=@UniqueConstraint(name="uk_invitation_token_hash",columnNames="tokenHash"))
public class WorkspaceInvitation {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="workspace_id",nullable=false) private TenantWorkspace workspace;
    @Column(nullable=false,length=254) private String email;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=16) private WorkspaceRole role;
    @Column(nullable=false,length=64) private String tokenHash;
    @Column(nullable=false) private Instant expiresAt;
    @Column(nullable=false,updatable=false) private Instant createdAt;
    private Instant acceptedAt;
    private Instant revokedAt;
    protected WorkspaceInvitation() {}
    public WorkspaceInvitation(TenantWorkspace workspace,String email,WorkspaceRole role,String tokenHash,Instant expiresAt){this.workspace=workspace;this.email=UserAccount.normalizeEmail(email);this.role=role;this.tokenHash=tokenHash;this.expiresAt=expiresAt;}
    @PrePersist void create(){createdAt=Instant.now();}
    public Long getId(){return id;} public TenantWorkspace getWorkspace(){return workspace;} public String getEmail(){return email;} public WorkspaceRole getRole(){return role;} public Instant getExpiresAt(){return expiresAt;} public Instant getCreatedAt(){return createdAt;} public Instant getAcceptedAt(){return acceptedAt;} public Instant getRevokedAt(){return revokedAt;}
    public boolean isPending(Instant now){return acceptedAt==null&&revokedAt==null&&expiresAt.isAfter(now);}
    public String getTokenHash(){return tokenHash;} public void accept(){acceptedAt=Instant.now();} public void revoke(){revokedAt=Instant.now();}
}
