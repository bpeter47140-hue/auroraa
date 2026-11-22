package com.aurora.memory;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.aurora.identity.TenantWorkspace;
import java.time.Instant;

@Entity
@Table(name = "memory_entries")
public class MemoryEntry {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", foreignKey = @ForeignKey(name = "fk_memory_entries_workspace"))
    private TenantWorkspace workspace;
    @Column(nullable=false, length=100) private String category;
    @Column(nullable=false, length=180) private String title;
    @Column(nullable=false, length=3000) private String content;
    @Column(length=180) private String source;
    @Column(nullable=false, updatable=false) private Instant createdAt;
    protected MemoryEntry() {}
    public MemoryEntry(String category, String title, String content, String source) {
        this.category=category; this.title=title; this.content=content; this.source=source;
        this.createdAt=Instant.now();
    }
    public Long getId(){return id;}
    public String getCategory(){return category;}
    public String getTitle(){return title;}
    public String getContent(){return content;}
    public String getSource(){return source;}
    public Instant getCreatedAt(){return createdAt;}

    @JsonIgnore
    public TenantWorkspace getWorkspace() { return workspace; }
    public void assignWorkspace(TenantWorkspace workspace) { if (workspace == null) throw new IllegalArgumentException("Workspace is required."); this.workspace = workspace; }
}
