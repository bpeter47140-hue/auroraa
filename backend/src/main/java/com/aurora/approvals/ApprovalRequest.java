package com.aurora.approvals;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.aurora.identity.TenantWorkspace;
import java.time.Instant;

@Entity
@Table(name="approval_requests")
public class ApprovalRequest {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", foreignKey = @ForeignKey(name = "fk_approval_requests_workspace"))
    private TenantWorkspace workspace;
    @Column(nullable=false,length=160) private String title;
    @Column(nullable=false,length=2000) private String description;
    @Column(nullable=false,length=40) private String actionType;
    @Column(nullable=false,length=24) private String status="PENDING";
    @Column(length=1000) private String decisionNote;
    @Column(nullable=false,updatable=false) private Instant createdAt;
    private Instant decidedAt;
    protected ApprovalRequest(){}
    public ApprovalRequest(String title,String description,String actionType){
        this.title=title;this.description=description;this.actionType=actionType;
    }
    @PrePersist void onCreate(){createdAt=Instant.now();}
    public Long getId(){return id;} public String getTitle(){return title;}
    public String getDescription(){return description;} public String getActionType(){return actionType;}
    public String getStatus(){return status;} public String getDecisionNote(){return decisionNote;}
    public Instant getCreatedAt(){return createdAt;} public Instant getDecidedAt(){return decidedAt;}
    public void decide(String status,String note){this.status=status;this.decisionNote=note;this.decidedAt=Instant.now();}

    @JsonIgnore
    public TenantWorkspace getWorkspace() { return workspace; }
    public void assignWorkspace(TenantWorkspace workspace) { if (workspace == null) throw new IllegalArgumentException("Workspace is required."); this.workspace = workspace; }
}
