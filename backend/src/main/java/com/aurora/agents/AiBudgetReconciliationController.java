package com.aurora.agents;

import com.aurora.audit.AuditService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/ai-budget/reconciliation")
public class AiBudgetReconciliationController {
    private final AiBudgetGuardService budgets;
    private final AuditService audit;
    public AiBudgetReconciliationController(AiBudgetGuardService budgets, AuditService audit) {
        this.budgets = budgets; this.audit = audit;
    }
    public record ReconciliationRequest(
        @jakarta.validation.constraints.NotNull @DecimalMin("0") @Digits(integer = 8, fraction = 10) BigDecimal actualCostUsd,
        @NotBlank @Size(min = 3, max = 160) String providerReference,
        @NotBlank @Size(min = 10, max = 1000) String evidenceNote) {}
    @GetMapping
    public List<AiBudgetGuardService.ReconciliationItem> list() { return budgets.unresolvedReservations(); }
    @PostMapping("/{reservationId}")
    @org.springframework.web.bind.annotation.ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void reconcile(@PathVariable Long reservationId, @Valid @RequestBody ReconciliationRequest request) {
        budgets.reconcile(reservationId, request.actualCostUsd(), request.providerReference().trim(), request.evidenceNote().trim());
        audit.record("AI_BUDGET_RESERVATION_RECONCILED", "AI_BUDGET_RESERVATION", reservationId.toString(),
            "Manually reconciled against provider billing reference " + request.providerReference().trim()
                + "; actual USD cost " + request.actualCostUsd().toPlainString()
                + ". Evidence note is stored on the reservation.");
    }
}
