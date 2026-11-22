package com.aurora.system;

import java.time.Instant;

public record SystemStatusResponse(
        String application,
        String status,
        String version,
        Instant timestamp
) {
}
