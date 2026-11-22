package com.aurora.system;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/system")
public class SystemStatusController {

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${info.app.version:0.1.0}")
    private String applicationVersion;

    @GetMapping("/status")
    public SystemStatusResponse getStatus() {
        return new SystemStatusResponse(
                applicationName,
                "OPERATIONAL",
                applicationVersion,
                Instant.now()
        );
    }
}
