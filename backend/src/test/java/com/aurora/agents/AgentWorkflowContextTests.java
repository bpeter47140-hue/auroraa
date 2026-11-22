package com.aurora.agents;

import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AgentWorkflowContextTests {
    @Test
    void preservesBeginningAndEndingOfLongPriorOutput() {
        String prior = "RESEARCH SUMMARY " + "x".repeat(500) + " SOURCE REGISTER AND VERDICT";
        String context = AgentWorkflowController.buildStageContext("BRIEF", List.of(prior), 180);

        assertTrue(context.startsWith("BRIEF"));
        assertTrue(context.contains("RESEARCH SUMMARY"));
        assertTrue(context.contains("SOURCE REGISTER AND VERDICT"));
        assertTrue(context.contains("HANDOFF NOTE"));
        assertTrue(context.length() <= 180);
    }

    @Test
    void keepsMostRecentStageFirstAndRetainsOlderStageWhenSpaceAllows() {
        String context = AgentWorkflowController.buildStageContext(
            "BRIEF", List.of("RESEARCH OUTPUT", "STRATEGY OUTPUT"), 200);

        assertTrue(context.indexOf("RESEARCH OUTPUT") < context.indexOf("STRATEGY OUTPUT"));
    }

    @Test
    void neverExceedsBudgetWhenPriorOutputDoesNotFit() {
        String context = AgentWorkflowController.buildStageContext(
            "BRIEF", List.of("a".repeat(500), "b".repeat(500)), 100);

        assertTrue(context.length() <= 100);
        assertTrue(context.startsWith("BRIEF"));
    }

    @Test
    void returnsBriefUnchangedWhenThereIsNoRoomForHandoffs() {
        assertEquals("BRIEF", AgentWorkflowController.buildStageContext("BRIEF", List.of("OUTPUT"), 5));
    }
}
