package com.aurora.businesses;
import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.aurora.identity.TenantWorkspace;
import java.time.Instant;
@Entity @Table(name="business_workspaces")
public class BusinessWorkspace {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", foreignKey = @ForeignKey(name = "fk_business_workspaces_workspace"))
    private TenantWorkspace workspace;
 @Column(nullable=false,length=140) private String name;
 @Column(length=2500) private String description;
 @Column(nullable=false,length=24) private String status="IDEA";
 @Column(length=1000) private String goal;
 @Column(nullable=false,updatable=false) private Instant createdAt;
 @Column(nullable=false) private Instant updatedAt;
 protected BusinessWorkspace(){}
 public BusinessWorkspace(String name,String description,String goal){this.name=name;this.description=description;this.goal=goal;}
 @PrePersist void onCreate(){createdAt=Instant.now();updatedAt=createdAt;}
 @PreUpdate void onUpdate(){updatedAt=Instant.now();}
 public Long getId(){return id;} public String getName(){return name;} public String getDescription(){return description;}
 public String getStatus(){return status;} public String getGoal(){return goal;} public Instant getCreatedAt(){return createdAt;} public Instant getUpdatedAt(){return updatedAt;}
 public void update(String name,String description,String goal,String status){this.name=name;this.description=description;this.goal=goal;this.status=status;}

    @JsonIgnore
    public TenantWorkspace getWorkspace() { return workspace; }
    public void assignWorkspace(TenantWorkspace workspace) { if (workspace == null) throw new IllegalArgumentException("Workspace is required."); this.workspace = workspace; }
}
