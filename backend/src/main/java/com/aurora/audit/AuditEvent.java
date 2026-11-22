package com.aurora.audit;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.aurora.identity.TenantWorkspace;
import java.time.Instant;

@Entity
@Table(name="audit_events")
public class AuditEvent {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", foreignKey = @ForeignKey(name = "fk_audit_events_workspace"))
    private TenantWorkspace workspace;
    @Column(nullable=false, length=60) private String eventType;
    @Column(nullable=false, length=40) private String entityType;
    @Column(nullable=false, length=80) private String entityId;
    @Column(nullable=false, length=1000) private String details;
    @Column(nullable=false, updatable=false) private Instant createdAt;
    protected AuditEvent() {}
    public AuditEvent(String eventType, String entityType, String entityId, String details) {
        this.eventType=eventType; this.entityType=entityType; this.entityId=entityId; this.details=details;
    }
    @PrePersist void onCreate() { createdAt=Instant.now(); }
    public Long getId(){return id;} public String getEventType(){return eventType;}
    public String getEntityType(){return entityType;} public String getEntityId(){return entityId;}
    public String getDetails(){return details;} public Instant getCreatedAt(){return createdAt;}

    @JsonIgnore
    public TenantWorkspace getWorkspace() { return workspace; }
    public void assignWorkspace(TenantWorkspace workspace) { if (workspace == null) throw new IllegalArgumentException("Workspace is required."); this.workspace = workspace; }
}
