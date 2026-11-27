package com.aurora.projects;

import com.aurora.identity.TenantWorkspace;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "creative_assets")
public class CreativeAsset {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "workspace_id", nullable = false) private TenantWorkspace workspace;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "project_id", nullable = false) private VentureProject project;
    @Column(nullable = false, length = 180) private String name;
    @Column(nullable = false, length = 40) private String mediaType;
    @Column(nullable = false, length = 100) private String contentType;
    @Column(nullable = false, length = 500) private String storageKey;
    @Column(nullable = false) private long sizeBytes;
    @Column(length = 1000) private String notes;
    @Column(nullable = false, length = 254) private String uploadedBy;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    protected CreativeAsset() {}
    public CreativeAsset(TenantWorkspace workspace, VentureProject project, String name, String mediaType, String contentType, String storageKey, long sizeBytes, String notes, String uploadedBy) {
        this.workspace=workspace; this.project=project; this.name=name; this.mediaType=mediaType; this.contentType=contentType; this.storageKey=storageKey; this.sizeBytes=sizeBytes; this.notes=notes; this.uploadedBy=uploadedBy;
    }
    @PrePersist void onCreate(){createdAt=Instant.now();}
    public Long getId(){return id;} public Long getProjectId(){return project.getId();} public String getName(){return name;} public String getMediaType(){return mediaType;} public String getContentType(){return contentType;} public long getSizeBytes(){return sizeBytes;} public String getNotes(){return notes;} public String getUploadedBy(){return uploadedBy;} public Instant getCreatedAt(){return createdAt;}
    public String getStorageKey(){return storageKey;}
}