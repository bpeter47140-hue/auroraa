package com.aurora.research;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.aurora.identity.TenantWorkspace;
import java.time.Instant;

@Entity
@Table(name = "customer_insights")
public class CustomerInsight {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", foreignKey = @ForeignKey(name = "fk_customer_insights_workspace"))
    private TenantWorkspace workspace;
    @Column(nullable=false, length=40) private String insightType;
    @Column(nullable=false, length=100) private String participantLabel;
    @Column(nullable=false, length=2000) private String problem;
    @Column(length=1500) private String currentSolution;
    @Column(length=1000) private String frequency;
    @Column(length=1000) private String willingness;
    @Column(length=2000) private String evidence;
    @Column(nullable=false, length=30) private String validationStatus;
    @Column(nullable=false, updatable=false) private Instant createdAt;
    protected CustomerInsight() {}
    public CustomerInsight(String insightType,String participantLabel,String problem,String currentSolution,String frequency,String willingness,String evidence,String validationStatus) {
        this.insightType=insightType; this.participantLabel=participantLabel; this.problem=problem; this.currentSolution=currentSolution;
        this.frequency=frequency; this.willingness=willingness; this.evidence=evidence; this.validationStatus=validationStatus; this.createdAt=Instant.now();
    }
    public Long getId(){return id;} public String getInsightType(){return insightType;} public String getParticipantLabel(){return participantLabel;}
    public String getProblem(){return problem;} public String getCurrentSolution(){return currentSolution;} public String getFrequency(){return frequency;}
    public String getWillingness(){return willingness;} public String getEvidence(){return evidence;} public String getValidationStatus(){return validationStatus;} public Instant getCreatedAt(){return createdAt;}

    @JsonIgnore
    public TenantWorkspace getWorkspace() { return workspace; }
    public void assignWorkspace(TenantWorkspace workspace) { if (workspace == null) throw new IllegalArgumentException("Workspace is required."); this.workspace = workspace; }
}
