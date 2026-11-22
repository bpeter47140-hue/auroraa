package com.aurora.agents;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;

@Service
public class AiProviderUsageLedgerService {
    private final AiProviderUsageRepository ledger;

    public record BusinessSummary(Long businessId, String businessName, long providerRequests, long promptTokens,
                                  long completionTokens, long totalTokens, BigDecimal reportedCostUsd,
                                  long requestsWithoutReportedCost) {}

    public record MonthlySummary(long providerRequests, long reportedRequests, long requestsWithUnknownUsage,
                                 long promptTokens, long completionTokens, long totalTokens,
                                 BigDecimal reportedCostUsd, long requestsWithoutReportedCost,
                                 LocalDate periodStart, List<BusinessSummary> businessUsage) {}

    public AiProviderUsageLedgerService(AiProviderUsageRepository ledger) { this.ledger = ledger; }

    @Transactional
    public Long begin(AgentRun run, String model) {
        return ledger.save(new AiProviderUsage(run, "OPENROUTER", model)).getId();
    }

    @Transactional
    public void complete(Long ledgerId, Map<?, ?> response) {
        if (ledgerId == null) return;
        AiProviderUsage entry = ledger.findById(ledgerId).orElseThrow();
        Object usageValue = response == null ? null : response.get("usage");
        Map<?, ?> usage = usageValue instanceof Map<?, ?> map ? map : Map.of();
        Integer prompt = integer(usage.get("prompt_tokens"));
        Integer completion = integer(usage.get("completion_tokens"));
        Integer total = integer(usage.get("total_tokens"));
        BigDecimal cost = decimal(usage.get("cost"));
        String requestId = response != null && response.get("id") instanceof String id ? id : null;
        entry.recordProviderResponse(prompt, completion, total, cost, requestId, !usage.isEmpty());
    }

    @Transactional
    public void markUnknown(Long ledgerId) {
        if (ledgerId == null) return;
        ledger.findById(ledgerId).ifPresent(AiProviderUsage::markUnknown);
    }

    @Transactional(readOnly = true)
    public MonthlySummary currentMonth(Long workspaceId) {
        LocalDate period = LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1);
        Instant from = period.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant until = period.plusMonths(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        var entries = ledger.findByWorkspace_IdAndRecordedAtGreaterThanEqualAndRecordedAtLessThan(workspaceId, from, until);
        long reported = entries.stream().filter(e -> "REPORTED".equals(e.getStatus()) || "MANUALLY_RECONCILED".equals(e.getStatus())).count();
        long unknown = entries.stream().filter(e -> "UNKNOWN".equals(e.getStatus()) || "UNAVAILABLE".equals(e.getStatus())).count();
        long prompt = entries.stream().map(AiProviderUsage::getPromptTokens).filter(v -> v != null).mapToLong(Integer::longValue).sum();
        long completion = entries.stream().map(AiProviderUsage::getCompletionTokens).filter(v -> v != null).mapToLong(Integer::longValue).sum();
        long total = entries.stream().map(AiProviderUsage::getTotalTokens).filter(v -> v != null).mapToLong(Integer::longValue).sum();
        BigDecimal cost = entries.stream().map(AiProviderUsage::getProviderCostUsd).filter(v -> v != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        long withoutCost = entries.stream().filter(e -> e.getProviderCostUsd() == null).count();
        Map<Long, List<AiProviderUsage>> grouped = new LinkedHashMap<>();
        for (AiProviderUsage entry : entries) {
            grouped.computeIfAbsent(entry.getAgentRun().getBusinessId(), ignored -> new ArrayList<>()).add(entry);
        }
        List<BusinessSummary> businessUsage = grouped.entrySet().stream().map(group -> {
            List<AiProviderUsage> items = group.getValue();
            AiProviderUsage first = items.get(0);
            long businessPrompt = items.stream().map(AiProviderUsage::getPromptTokens).filter(v -> v != null).mapToLong(Integer::longValue).sum();
            long businessCompletion = items.stream().map(AiProviderUsage::getCompletionTokens).filter(v -> v != null).mapToLong(Integer::longValue).sum();
            long businessTotal = items.stream().map(AiProviderUsage::getTotalTokens).filter(v -> v != null).mapToLong(Integer::longValue).sum();
            BigDecimal businessCost = items.stream().map(AiProviderUsage::getProviderCostUsd).filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            long missing = items.stream().filter(e -> e.getProviderCostUsd() == null).count();
            return new BusinessSummary(group.getKey(), first.getAgentRun().getBusinessName(), items.size(),
                businessPrompt, businessCompletion, businessTotal, businessCost, missing);
        }).toList();
        return new MonthlySummary(entries.size(), reported, unknown, prompt, completion, total,
            cost, withoutCost, period, businessUsage);
    }

    private static Integer integer(Object value) {
        if (value instanceof Number number) {
            long parsed = number.longValue();
            return parsed >= 0 && parsed <= Integer.MAX_VALUE ? (int) parsed : null;
        }
        return null;
    }

    private static BigDecimal decimal(Object value) {
        try {
            BigDecimal parsed = value instanceof BigDecimal decimal ? decimal
                : value instanceof Number number ? new BigDecimal(number.toString())
                : value instanceof String string ? new BigDecimal(string) : null;
            return parsed != null && parsed.signum() >= 0 ? parsed : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
