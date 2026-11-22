package com.aurora.agents;

import com.aurora.audit.AuditService;
import com.aurora.security.WorkspaceContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/ai-budget/alerts")
public class AiBudgetAlertController {
    private final BusinessAiBudgetAlertRepository alerts;
    private final AuditService audit;
    public AiBudgetAlertController(BusinessAiBudgetAlertRepository alerts, AuditService audit) {
        this.alerts = alerts; this.audit = audit;
    }
    public record AlertView(Long id, Long businessId, String businessName, LocalDate periodStart,
                            String alertLevel, BigDecimal observedUsd, BigDecimal monthlyLimitUsd,
                            Instant createdAt, Instant acknowledgedAt) {}
    @GetMapping
    public List<AlertView> list() {
        WorkspaceContextHolder.requireAnyRole(com.aurora.identity.WorkspaceRole.OWNER, com.aurora.identity.WorkspaceRole.ADMIN);
        return alerts.findTop50ByWorkspace_IdOrderByCreatedAtDesc(WorkspaceContextHolder.id()).stream()
            .map(a -> new AlertView(a.getId(), a.getBusinessId(), a.getBusinessName(), a.getPeriodStart(),
                a.getAlertLevel(), a.getObservedUsd(), a.getMonthlyLimitUsd(), a.getCreatedAt(), a.getAcknowledgedAt()))
            .toList();
    }
    @PostMapping("/{alertId}/acknowledge")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void acknowledge(@PathVariable Long alertId) {
        WorkspaceContextHolder.requireAnyRole(com.aurora.identity.WorkspaceRole.OWNER, com.aurora.identity.WorkspaceRole.ADMIN);
        BusinessAiBudgetAlert alert = alerts.findByIdAndWorkspace_Id(alertId, WorkspaceContextHolder.id())
            .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND, "Alert not found."));
        if (alert.getAcknowledgedAt() == null) {
            alert.acknowledge();
            audit.record("AI_BUDGET_ALERT_ACKNOWLEDGED", "AI_BUDGET_ALERT", alertId.toString(),
                "Acknowledged " + alert.getAlertLevel() + " AI budget alert for business " + alert.getBusinessId() + ".");
        }
    }
}
