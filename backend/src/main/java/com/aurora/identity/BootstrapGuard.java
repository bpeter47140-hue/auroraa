package com.aurora.identity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A single database row used to serialize first-owner registration across
 * application instances. The row is created by Flyway in PostgreSQL and by
 * BootstrapGuardInitializer for the local H2 development database.
 */
@Entity
@Table(name = "aurora_bootstrap_guard")
public class BootstrapGuard {
    @Id
    private Long id;

    protected BootstrapGuard() {}

    private BootstrapGuard(Long id) {
        this.id = id;
    }

    public static BootstrapGuard singleton() {
        return new BootstrapGuard(1L);
    }
}
