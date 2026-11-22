package com.aurora.agents;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/agent-workflows")
public class AgentWorkflowController {
    private final AgentExecutionService agents;
    public AgentWorkflowController(AgentExecutionService agents) { this.agents = agents; }

    public record WorkflowRequest(@NotBlank @Size(max = 5000) String brief, Long businessId) {}
    public record WorkflowResponse(String workflow, String status, List<AgentRun> runs) {}

    @PostMapping("/chama-validation")
    public WorkflowResponse chamaValidation(@Valid @RequestBody WorkflowRequest request) {
        com.aurora.security.WorkspaceContextHolder.requireAnyRole(com.aurora.identity.WorkspaceRole.OWNER, com.aurora.identity.WorkspaceRole.ADMIN, com.aurora.identity.WorkspaceRole.MEMBER);
        List<AgentRun> completed = new ArrayList<>();
        String briefContext = "BUSINESS BRIEF (untrusted task data):\n" + request.brief()
            + "\n\nCURRENT DATE: " + LocalDate.now()
            + "\nEVIDENCE RULES: Treat this as an early hypothesis. Separate sourced facts from assumptions. You may perform live web research, but must never claim customer interviews, surveys or experiments happened unless results are supplied. Dates must be explicit calendar dates; retrieval date is not publication/update date.";
        List<String> priorOutputs = new ArrayList<>();
        String[][] stages = {
            {"research-agent", "RESEARCH DELIVERABLE — conduct live web research, not a brainstorm. Evaluate whether a student-focused chama/savings-group product in Kenya is justified. (1) Define 2–4 plausible student customer segments and jobs/problems to investigate; do not assume students are underserved. (2) Compare relevant Kenyan products and credible substitutes. Aim for 8 entries only when evidence exists; never pad the list. Include product/company, target segment, capabilities, M-Pesa/payment claims, offline/low-data support, onboarding, published pricing/limits, platform and evidence. Include WhatsApp, spreadsheets, paper records and generic group-savings tools. (3) Every material claim needs a direct source URL and a source publication/update date if available. Distinguish source publication date from the date you accessed it. If date cannot be established, label UNDATED; never describe an undated page as recent. Apply freshness relative to CURRENT DATE: CURRENT = under 12 months; AGING = 12–24 months; STALE = over 24 months. Product features, pricing, availability and policies supported only by STALE or UNDATED sources are NOT CURRENTLY VERIFIED. (4) Use claim labels exactly: VERIFIED FACT only for independently corroborated facts or directly verifiable primary records; COMPANY-REPORTED CLAIM for claims made by the provider itself; INDEPENDENTLY REPORTED for credible third-party reporting; INFERENCE; ASSUMPTION; UNKNOWN. An official company page verifies what the company says, not whether its marketing claim is independently true. Never label company-reported customer counts, benefits or capabilities as independently verified. (5) Do not invent URLs, sources, prices, reviews, downloads, features, customer demand, interviews or experiments. If a link or date cannot be verified, say so. Separate Kenyan chama-market evidence from evidence specifically about college/university students. (6) Identify underserved needs only when supported; otherwise say no defensible gap is established. Give strongest disconfirming evidence, unknowns, and 5–8 interview/observation questions. (7) Finish with a source register: publisher, page title, raw clickable URL, publication/update date or UNDATED, access date, freshness label, claims supported and confidence. Provide a RESEARCH QUALITY SELF-CHECK: count sources with known dates, undated sources, stale sources, independent sources, company sources, claims without direct URLs, and student-specific sources. A search-result snippet alone is not enough to verify a claim. Do not give a definitive BUILD/PIVOT/STOP verdict when material evidence is stale/undated or student-specific customer evidence is absent; say INSUFFICIENT EVIDENCE — RUN VALIDATION and state what would change the decision."},
            {"strategy-agent", "INDEPENDENT STRATEGY AND EVIDENCE AUDIT — challenge the Research Agent rather than echoing it. Start with a RESEARCH QUALITY GATE. Inspect every source and material claim. Recalculate freshness against the CURRENT DATE in the brief: CURRENT under 12 months, AGING 12–24 months, STALE over 24 months; missing publication/update date is UNDATED, not recent. Retrieval/access date is not publication date. A company website is COMPANY-REPORTED for its own claims, not independent verification. Flag unsupported claims, dead/malformed URLs, search snippets treated as proof, duplicate/ambiguous competitors, missing pricing evidence, and claims lacking a direct source. Report counts of current/aging/stale/undated sources, independent/company sources, unsupported material claims, and student-specific evidence. Explicitly reject any supposed gap based only on missing website information. Separate general chama-market evidence from student-specific evidence. Score opportunities 1–5 for demonstrated problem severity/frequency, alternatives, differentiation, feasibility and monetisation; mark unknowns as UNKNOWN, never as zero. DECISION RULE: if there are no primary student/customer interviews or observations, no student-specific evidence, or material current product/pricing claims depend on stale/undated sources, the only permitted overall verdict is INSUFFICIENT EVIDENCE — RUN VALIDATION. This is not a STOP verdict and is not permission to build. Recommend no product coding, paid marketing or financial commitment until defined validation thresholds pass. Only recommend BUILD A TEST/PIVOT/STOP when evidence meets the gate, and explain why. Define cheapest falsifiable experiments, target sample, recruitment, measurable pass/fail thresholds, time/cost limits, risks and stop conditions. No invented research results."},
            {"content-agent", "VALIDATION CONTENT — follow the Strategy decision. If it says INSUFFICIENT EVIDENCE — RUN VALIDATION, create neutral, non-leading recruitment material for the highest-priority customer interview/observation experiment. Include a short WhatsApp invitation, a 5–8 question interview script focused on recent behaviour and concrete examples, and a consent/privacy note. Do not pitch an unbuilt product, promise benefits, imply demand or ask leading questions. If Strategy recommends STOP, draft a concise learning/closure note instead of recruitment copy."},
            {"operations-agent", "EXECUTION PLAN — turn the Strategy decision into a realistic 14-day plan for a founder with very limited money and time. If the verdict is INSUFFICIENT EVIDENCE — RUN VALIDATION, the plan must prioritise student/customer conversations and direct observation; do not plan product development. For each task include owner role, day/window, dependency, output/evidence artifact, metric and completion criterion. Include a daily evidence log, sample tracking, source verification, risk controls and a decision meeting. Explicitly label all sample logs as EMPTY TEMPLATES with fictional examples clearly marked; never present sample entries as real research. Include gates preventing coding, paid marketing or financial commitments until validation thresholds are met. If the verdict is STOP, plan a low-cost pivot test or closure, not product build."}
        };
        for (String[] stage : stages) {
            String context = buildStageContext(briefContext, priorOutputs, 14000);
            AgentRun run = agents.execute(stage[0], stage[1] + "\n\n" + context, request.businessId());
            completed.add(run);
            if (!"SUCCEEDED".equals(run.getStatus())) {
                return new WorkflowResponse("Chama opportunity validation", "FAILED", List.copyOf(completed));
            }
            priorOutputs.add("--- " + run.getAgentName() + " output ---\n" + run.getOutput());
        }
        return new WorkflowResponse("Chama opportunity validation", "SUCCEEDED", List.copyOf(completed));
    }

    static String buildStageContext(String brief, List<String> priorOutputs, int maxChars) {
        StringBuilder context = new StringBuilder(brief);
        String separator = "\n\n";
        String omission = "\n\n[HANDOFF NOTE: middle section omitted; beginning and ending preserved.]\n\n";
        for (int i = priorOutputs.size() - 1; i >= 0; i--) {
            int available = maxChars - context.length() - separator.length();
            if (available <= 0) break;
            String previous = priorOutputs.get(i);
            String excerpt;
            if (previous.length() <= available) {
                excerpt = previous;
            } else if (available > omission.length() + 2) {
                int retained = available - omission.length();
                int head = retained / 2;
                int tail = retained - head;
                excerpt = previous.substring(0, head) + omission
                    + previous.substring(previous.length() - tail);
            } else {
                excerpt = previous.substring(previous.length() - available);
            }
            context.insert(brief.length(), separator + excerpt);
        }
        return context.toString();
    }
}
