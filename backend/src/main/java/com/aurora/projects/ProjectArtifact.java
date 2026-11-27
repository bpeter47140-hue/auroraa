package com.aurora.projects;
import com.aurora.identity.TenantWorkspace;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="project_artifacts")
public class ProjectArtifact {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="workspace_id",nullable=false) private TenantWorkspace workspace;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="project_id",nullable=false) private VentureProject project;
 @Column(nullable=false,length=40) private String artifactType;
 @Column(nullable=false,length=180) private String name;
 @Column(nullable=false,length=500) private String relativePath;
 @Column(length=1000) private String description;
 @Column(nullable=false) private long sizeBytes;
 @Column(nullable=false,updatable=false) private Instant createdAt;
 protected ProjectArtifact(){}
 public ProjectArtifact(TenantWorkspace w,VentureProject p,String type,String name,String path,String description,long size){workspace=w;project=p;artifactType=type;this.name=name;relativePath=path;this.description=description;sizeBytes=size;}
 @PrePersist void onCreate(){createdAt=Instant.now();}
 public void updateSize(long size){this.sizeBytes=size;}
 public Long getId(){return id;} public Long getProjectId(){return project.getId();} public String getArtifactType(){return artifactType;} public String getName(){return name;} public String getRelativePath(){return relativePath;} public String getDescription(){return description;} public long getSizeBytes(){return sizeBytes;} public Instant getCreatedAt(){return createdAt;}
}
