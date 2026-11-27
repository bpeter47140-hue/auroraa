package com.aurora.projects;

import com.aurora.identity.TenantWorkspace;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "campaign_performance_metrics")
public class CampaignPerformanceMetric {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "workspace_id", nullable = false) private TenantWorkspace workspace;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "campaign_id", nullable = false) private MarketingCampaign campaign;
    @Column(name = "reported_date", nullable = false) private LocalDate reportedDate;
    @Column(nullable = false) private long impressions;
    @Column(nullable = false) private long reach;
    @Column(nullable = false) private long clicks;
    @Column(nullable = false) private long likes;
    @Column(nullable = false) private long comments;
    @Column(nullable = false) private long shares;
    @Column(nullable = false) private long conversions;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal spend;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal revenue;
    @Column(length = 1000) private String notes;
    @Column(nullable = false, length = 254) private String recordedBy;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;

    protected CampaignPerformanceMetric() {}
    public CampaignPerformanceMetric(TenantWorkspace workspace, MarketingCampaign campaign, LocalDate date,
        PerformanceRequest request, String actor) {
        this.workspace = workspace; this.campaign = campaign; this.reportedDate = date; this.recordedBy = actor;
        update(request);
    }
    public void update(PerformanceRequest r) {
        if (r.impressions()<0 || r.reach()<0 || r.clicks()<0 || r.likes()<0 || r.comments()<0 || r.shares()<0 || r.conversions()<0)
            throw new IllegalArgumentException("Performance counts cannot be negative.");
        if (r.reach()>r.impressions() || r.clicks()>r.impressions() || r.conversions()>r.clicks())
            throw new IllegalArgumentException("Reach and clicks cannot exceed impressions, and conversions cannot exceed clicks.");
        if (r.spend()==null || r.revenue()==null || r.spend().signum()<0 || r.revenue().signum()<0 || r.spend().scale()>2 || r.revenue().scale()>2)
            throw new IllegalArgumentException("Spend and revenue must be non-negative amounts with at most two decimal places.");
        if (r.notes()!=null && r.notes().length()>1000) throw new IllegalArgumentException("Notes are too long.");
        impressions=r.impressions(); reach=r.reach(); clicks=r.clicks(); likes=r.likes(); comments=r.comments(); shares=r.shares(); conversions=r.conversions();
        spend=r.spend(); revenue=r.revenue(); notes=r.notes()==null?null:r.notes().trim();
        updatedAt=Instant.now();
    }
    @PrePersist void onCreate(){createdAt=Instant.now();if(updatedAt==null)updatedAt=createdAt;}
    public Long getId(){return id;} public LocalDate getReportedDate(){return reportedDate;} public long getImpressions(){return impressions;}
    public long getReach(){return reach;} public long getClicks(){return clicks;} public long getLikes(){return likes;} public long getComments(){return comments;}
    public long getShares(){return shares;} public long getConversions(){return conversions;} public BigDecimal getSpend(){return spend;} public BigDecimal getRevenue(){return revenue;}
    public String getNotes(){return notes;} public String getRecordedBy(){return recordedBy;} public Instant getCreatedAt(){return createdAt;} public Instant getUpdatedAt(){return updatedAt;}
    @JsonIgnore public TenantWorkspace getWorkspace(){return workspace;} @JsonIgnore public MarketingCampaign getCampaign(){return campaign;}
    public record PerformanceRequest(long impressions,long reach,long clicks,long likes,long comments,long shares,long conversions,BigDecimal spend,BigDecimal revenue,String notes) {}
}
