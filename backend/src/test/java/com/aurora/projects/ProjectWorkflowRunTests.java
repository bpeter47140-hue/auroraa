package com.aurora.projects;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProjectWorkflowRunTests {
    @Test
    void pauseAndResumeKeepWorkflowAtCheckpoint() {
        ProjectWorkflowRun run = new ProjectWorkflowRun(null, null, "website-planning");
        assertEquals("QUEUED", run.getStatus());
        run.start();
        assertEquals("RUNNING", run.getStatus());
        run.requestPause();
        assertEquals("PAUSE_REQUESTED", run.getStatus());
        run.pause();
        assertEquals("PAUSED", run.getStatus());
        run.resume();
        assertEquals("QUEUED", run.getStatus());
        run.start();
        assertEquals("RUNNING", run.getStatus());
    }

    @Test
    void approvalGateCanOnlyReleaseAWorkflowWaitingForApproval() {
        ProjectWorkflowRun run = new ProjectWorkflowRun(null, null, "website-build");
        run.awaitApproval();
        assertEquals("AWAITING_APPROVAL", run.getStatus());
        run.approvalGranted();
        assertEquals("QUEUED", run.getStatus());
        assertThrows(IllegalStateException.class, run::approvalGranted);
    }

    @Test
    void cannotResumeWorkflowThatWasNotPaused() {
        ProjectWorkflowRun run = new ProjectWorkflowRun(null, null, "website-planning");
        assertThrows(IllegalStateException.class, run::resume);
    }
}
