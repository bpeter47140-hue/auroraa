package com.aurora.identity;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ensures the singleton row exists before the local application accepts
 * requests. In PostgreSQL the same row is inserted by Flyway migration V3.
 */
@Component
public class BootstrapGuardInitializer implements ApplicationRunner {
    private final BootstrapGuardRepository guards;

    public BootstrapGuardInitializer(BootstrapGuardRepository guards) {
        this.guards = guards;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!guards.existsById(1L)) {
            guards.saveAndFlush(BootstrapGuard.singleton());
        }
    }
}
