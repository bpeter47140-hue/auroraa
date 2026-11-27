package com.aurora.agents;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/agents")
public class AgentCatalogController {
    @Value("${aurora.ai.api-key:}") private String apiKey;
    @Value("${aurora.ai.model:openai/gpt-4.1-mini}") private String model;

    public record AgentProfile(String id,String name,String role,String description,
        List<String> capabilities,String status,boolean modelConfigured,String model){}
    @GetMapping
    public List<AgentProfile> list(){
        boolean configured=apiKey!=null&&!apiKey.isBlank()&&model!=null&&!model.isBlank()&& !"not-configured".equals(model);
        String status=configured?"READY":"MODEL_REQUIRED";
        return List.of(
            new AgentProfile("research-agent","Research Agent","Market intelligence",
                "Researches supplied context, evaluates competitors and develops validation experiments. Provider web search is configured; source and citation quality still require end-to-end verification.",
                List.of("Market research","Competitor analysis","Opportunity scoring","Validation experiments"),status,configured,model),
            new AgentProfile("strategy-agent","Strategy Agent","Business planning",
                "Turns supplied research into business plans, measurable experiments and strategic priorities.",
                List.of("Business plans","Experiment design","Goal setting","Risk analysis"),status,configured,model),
            new AgentProfile("content-agent","Content Agent","Content production",
                "Creates content plans and drafts for review; does not publish to external channels.",
                List.of("Content calendars","Draft writing","Repurposing","Campaign planning"),status,configured,model),
            new AgentProfile("operations-agent","Operations Agent","Business operations",
                "Turns operational context into prioritised tasks, dependencies and performance measures.",
                List.of("Task coordination","Workflow monitoring","Operations reporting"),status,configured,model),
            new AgentProfile("developer-agent","Product Developer Agent","Frontend development",
                "Generates restricted React and TypeScript source files for a project workspace. Generated code is not executed or deployed automatically.",
                List.of("React page generation","Component writing","Responsive CSS","Source artifacts"),status,configured,model),
            new AgentProfile("qa-agent","Quality Assurance Agent","Website QA",
                "Reviews supplied website source for functionality, accessibility, responsive design and release risks. Produces findings only; never edits code.",
                List.of("Accessibility review","User journey review","Responsive source review","Release checklist"),status,configured,model),
            new AgentProfile("security-agent","Security Review Agent","Application security",
                "Reviews supplied frontend source for security and privacy risks. Does not execute code or replace a full penetration test.",
                List.of("XSS and unsafe code review","Secret exposure review","Privacy and tracking review","Remediation guidance"),status,configured,model)
        );
    }
}
