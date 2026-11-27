package com.aurora.projects;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProjectWorkflowStepApprovalTests {
    @Test
    void protectedStepCannotRunUntilExplicitlyApproved() {
        ProjectWorkflowStep step = new ProjectWorkflowStep(null, 4, "developer-agent", "Generate website source", "Build the approved MVP.");
        step.requireApproval();
        assertTrue(step.isApprovalRequired());
        assertEquals("PENDING", step.getApprovalStatus());
        step.awaitApproval();
        assertEquals("AWAITING_APPROVAL", step.getStatus());
        assertThrows(IllegalStateException.class, step::awaitApproval);
        step.approve();
        assertEquals("APPROVED", step.getApprovalStatus());
        assertEquals("PENDING", step.getStatus());
        assertNotNull(step.getApprovalDecidedAt());
        step.start();
        assertEquals("RUNNING", step.getStatus());
    }

    @Test
    void unprotectedStepDoesNotAcquireAnApprovalGate() {
        ProjectWorkflowStep step = new ProjectWorkflowStep(null, 1, "research-agent", "Research", "Research the market.");
        assertFalse(step.isApprovalRequired());
        assertNull(step.getApprovalStatus());
    }
}
