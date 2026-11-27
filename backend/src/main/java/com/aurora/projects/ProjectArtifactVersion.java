package com.aurora.projects;

import com.aurora.identity.TenantWorkspace;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="project_artifact_versions", uniqueConstraints=@UniqueConstraint(name="uq_project_artifact_version",columnNames={"workspace_id","artifact_id","version_number"}))
public class ProjectArtifactVersion {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="workspace_id",nullable=false) private TenantWorkspace workspace;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="project_id",nullable=false) private VentureProject project;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="artifact_id",nullable=false) private ProjectArtifact artifact;
 @Column(name="version_number",nullable=false) private int versionNumber;
 @Column(nullable=false,columnDefinition="TEXT") private String content;
 @Column(nullable=false,length=64) private String sha256;
 @Column(nullable=false,length=254) private String createdBy;
 @Column(nullable=false,updatable=false) private Instant createdAt;
 protected ProjectArtifactVersion(){}
 public ProjectArtifactVersion(TenantWorkspace workspace,VentureProject project,ProjectArtifact artifact,int versionNumber,String content,String sha256,String createdBy){this.workspace=workspace;this.project=project;this.artifact=artifact;this.versionNumber=versionNumber;this.content=content;this.sha256=sha256;this.createdBy=createdBy;}
 @PrePersist void onCreate(){createdAt=Instant.now();}
 public Long getId(){return id;} public Long getArtifactId(){return artifact.getId();} public int getVersionNumber(){return versionNumber;} public String getContent(){return content;} public String getSha256(){return sha256;} public String getCreatedBy(){return createdBy;} public Instant getCreatedAt(){return createdAt;}
}
