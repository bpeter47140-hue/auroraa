package com.aurora.projects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.aurora.identity.TenantWorkspace;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "project_reviews")
public class ProjectReview {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "workspace_id", nullable = false) private TenantWorkspace workspace;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "project_id", nullable = false) private VentureProject project;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "artifact_id", nullable = false) private ProjectArtifact artifact;
    @Column(nullable = false, length = 180) private String artifactName;
    @Column(nullable = false, length = 500) private String artifactPath;
    @Column(nullable = false, length = 64) private String artifactSha256;
    @Column(name="artifact_version_id") private Long artifactVersionId;
    @Column(name="artifact_version_number") private Integer artifactVersionNumber;
    @Column(nullable = false, length = 24) private String status = "PENDING_REVIEW";
    @Column(nullable = false, length = 254) private String requestedBy;
    @Column(length = 254) private String reviewedBy;
    @Column(length = 4000) private String feedback;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    private Instant reviewedAt;
    protected ProjectReview() {}
    public ProjectReview(TenantWorkspace workspace, VentureProject project, ProjectArtifact artifact, ProjectArtifactVersion version, String hash, String requester) {
        this.workspace = workspace; this.project = project; this.artifact = artifact;
        this.artifactName = artifact.getName(); this.artifactPath = artifact.getRelativePath();
        this.artifactSha256 = hash; this.artifactVersionId = version.getId(); this.artifactVersionNumber = version.getVersionNumber(); this.requestedBy = requester;
    }
    @PrePersist void onCreate() { createdAt = Instant.now(); }
    public void decide(String decision, String reviewer, String note) {
        if (!"PENDING_REVIEW".equals(status)) throw new IllegalStateException("This review has already been decided.");
        status = decision; reviewedBy = reviewer; feedback = note; reviewedAt = Instant.now();
    }
    public Long getId(){return id;} public Long getProjectId(){return project.getId();}
    public Long getArtifactId(){return artifact.getId();} public String getArtifactName(){return artifactName;}
    public String getArtifactPath(){return artifactPath;} public String getArtifactSha256(){return artifactSha256;} public Long getArtifactVersionId(){return artifactVersionId;} public Integer getArtifactVersionNumber(){return artifactVersionNumber;}
    public String getStatus(){return status;} public String getRequestedBy(){return requestedBy;}
    public String getReviewedBy(){return reviewedBy;} public String getFeedback(){return feedback;}
    public Instant getCreatedAt(){return createdAt;} public Instant getReviewedAt(){return reviewedAt;}
    @JsonIgnore public TenantWorkspace getWorkspace(){return workspace;}
}
