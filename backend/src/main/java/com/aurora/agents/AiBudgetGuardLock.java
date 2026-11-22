package com.aurora.agents;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "ai_budget_guard_lock")
public class AiBudgetGuardLock {
    @Id private Long id;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    protected AiBudgetGuardLock() {}
    public AiBudgetGuardLock(Long id) { this.id = id; this.updatedAt = Instant.now(); }
    public void touch() { updatedAt = Instant.now(); }
}
