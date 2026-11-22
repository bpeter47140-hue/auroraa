package com.aurora.agents;

import com.aurora.audit.AuditService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/businesses/{businessId}/ai-budget")
public class BusinessAiBudgetController {
    private final AiBudgetGuardService budgets;
    private final AuditService audit;
    public BusinessAiBudgetController(AiBudgetGuardService budgets, AuditService audit) {
        this.budgets = budgets; this.audit = audit;
    }
    public record BudgetRequest(@NotNull @DecimalMin(value = "0.0000000001") @Digits(integer = 8, fraction = 10) BigDecimal monthlyLimitUsd) {}
    @GetMapping
    public AiBudgetGuardService.BusinessBudgetView get(@PathVariable Long businessId) {
        return budgets.businessBudget(businessId);
    }
    @DeleteMapping
    @org.springframework.web.bind.annotation.ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void remove(@PathVariable Long businessId) {
        budgets.removeBusinessBudget(businessId);
        audit.record("BUSINESS_AI_BUDGET_REMOVED", "BUSINESS", businessId.toString(), "Removed monthly AI spending ceiling.");
    }
    @PutMapping
    public AiBudgetGuardService.BusinessBudgetView set(@PathVariable Long businessId, @Valid @RequestBody BudgetRequest request) {
        var result = budgets.setBusinessBudget(businessId, request.monthlyLimitUsd());
        audit.record("BUSINESS_AI_BUDGET_UPDATED", "BUSINESS", businessId.toString(),
            "Updated monthly AI spending ceiling in USD.");
        return result;
    }
}
