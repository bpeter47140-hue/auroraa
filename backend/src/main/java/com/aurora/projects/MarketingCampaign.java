package com.aurora.projects;

import com.aurora.identity.TenantWorkspace;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "marketing_campaigns")
public class MarketingCampaign {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "workspace_id", nullable = false) private TenantWorkspace workspace;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "project_id", nullable = false) private VentureProject project;
    @Column(nullable = false, length = 180) private String title;
    @Column(nullable = false, length = 24) private String channel;
    @Column(nullable = false, length = 24) private String format;
    @Column(nullable = false, columnDefinition = "TEXT") private String content;
    @Column(length = 500) private String callToAction;
    @Column(nullable = false, length = 24) private String status = "DRAFT";
    @Column(length = 254, nullable = false) private String createdBy;
    @Column(length = 254) private String reviewedBy;
    @Column(length = 4000) private String feedback;
    private Instant scheduledAt;
    private Instant approvedAt;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;

    protected MarketingCampaign() {}
    public MarketingCampaign(TenantWorkspace workspace, VentureProject project, String title, String channel,
        String format, String content, String callToAction, String createdBy) {
        this.workspace = workspace; this.project = project;
        edit(title, channel, format, content, callToAction);
        this.createdBy = createdBy;
    }
    @PrePersist void onCreate() { createdAt = Instant.now(); updatedAt = createdAt; }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }
    public void edit(String title, String channel, String format, String content, String callToAction) {
        if (!"DRAFT".equals(status) && !"CHANGES_REQUESTED".equals(status)) throw new IllegalStateException("Only drafts or campaigns returned for changes can be edited.");
        this.title = required(title, 180, "Title");
        this.channel = oneOf(channel, "INSTAGRAM", "FACEBOOK", "LINKEDIN", "TIKTOK", "YOUTUBE", "X", "EMAIL", "OTHER");
        this.format = oneOf(format, "POST", "STORY", "REEL", "VIDEO", "CAROUSEL", "EMAIL", "OTHER");
        this.content = required(content, 20000, "Content");
        this.callToAction = callToAction == null ? null : callToAction.trim();
        if (this.callToAction != null && this.callToAction.length() > 500) throw new IllegalArgumentException("Call to action is too long.");
        this.status = "DRAFT"; this.feedback = null;
    }
    public void submit() {
        if (!"DRAFT".equals(status) && !"CHANGES_REQUESTED".equals(status)) throw new IllegalStateException("Only a draft can be submitted for review.");
        status = "PENDING_REVIEW"; feedback = null;
    }
    public void decide(String decision, String actor, String note) {
        if (!"PENDING_REVIEW".equals(status)) throw new IllegalStateException("Campaign is not awaiting review.");
        if (!"APPROVED".equals(decision) && !"CHANGES_REQUESTED".equals(decision) && !"REJECTED".equals(decision)) throw new IllegalArgumentException("Invalid review decision.");
        if (!"APPROVED".equals(decision) && (note == null || note.isBlank())) throw new IllegalArgumentException("Feedback is required for changes or rejection.");
        status = decision; reviewedBy = actor; feedback = note == null ? null : note.trim();
        if ("APPROVED".equals(decision)) approvedAt = Instant.now();
    }
    public void schedule(Instant when) {
        if (!"APPROVED".equals(status)) throw new IllegalStateException("Only approved campaigns can be scheduled.");
        if (when == null || !when.isAfter(Instant.now())) throw new IllegalArgumentException("Choose a future schedule time.");
        scheduledAt = when; status = "SCHEDULED";
    }
    private static String required(String value, int max, String label) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label + " is required.");
        if (value.trim().length() > max) throw new IllegalArgumentException(label + " is too long.");
        return value.trim();
    }
    private static String oneOf(String value, String... allowed) {
        String normal = value == null ? "" : value.trim().toUpperCase();
        for (String candidate : allowed) if (candidate.equals(normal)) return normal;
        throw new IllegalArgumentException("Unsupported campaign channel or format.");
    }
    public Long getId(){return id;} public String getTitle(){return title;} public String getChannel(){return channel;}
    public String getFormat(){return format;} public String getContent(){return content;} public String getCallToAction(){return callToAction;}
    public String getStatus(){return status;} public String getCreatedBy(){return createdBy;} public String getReviewedBy(){return reviewedBy;}
    public String getFeedback(){return feedback;} public Instant getScheduledAt(){return scheduledAt;} public Instant getApprovedAt(){return approvedAt;}
    public Instant getCreatedAt(){return createdAt;} public Instant getUpdatedAt(){return updatedAt;} public Long getProjectId(){return project.getId();}
    @JsonIgnore public TenantWorkspace getWorkspace(){return workspace;} @JsonIgnore public VentureProject getProject(){return project;}
}
