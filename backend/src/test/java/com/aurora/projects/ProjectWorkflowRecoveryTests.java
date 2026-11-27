package com.aurora.projects;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class ProjectWorkflowRecoveryTests {
    private final ProjectWorkflowRunRepository runs = mock(ProjectWorkflowRunRepository.class);
    private final ProjectWorkflowStepRepository steps = mock(ProjectWorkflowStepRepository.class);
    private final ProjectWorkflowService service = mock(ProjectWorkflowService.class);
    private final ProjectWorkflowRecovery recovery = new ProjectWorkflowRecovery(runs, steps, service);

    @Test
    void liveWorkerLeasePreventsAnotherInstanceFromRecoveringPauseRequest() {
        ProjectWorkflowRun run = mock(ProjectWorkflowRun.class);
        when(run.getLeaseExpiresAt()).thenReturn(Instant.now().plusSeconds(60));
        when(runs.findAllByStatusOrderByCreatedAtAsc("PAUSE_REQUESTED")).thenReturn(List.of(run));

        recovery.periodicRecovery();

        verify(steps, never()).findAllByWorkflowRun_IdOrderByStepOrderAsc(anyLong());
        verify(runs, never()).save(any(ProjectWorkflowRun.class));
    }

    @Test
    void expiredCancelRequestCancelsWithoutReplayingUnknownActiveStage() {
        ProjectWorkflowRun run = mock(ProjectWorkflowRun.class);
        when(run.getId()).thenReturn(42L);
        when(run.getLeaseExpiresAt()).thenReturn(Instant.now().minusSeconds(1));
        when(runs.findAllByStatusOrderByCreatedAtAsc("CANCEL_REQUESTED")).thenReturn(List.of(run));

        ProjectWorkflowStep active = mock(ProjectWorkflowStep.class);
        when(active.getStatus()).thenReturn("RUNNING");
        ProjectWorkflowStep pending = mock(ProjectWorkflowStep.class);
        when(pending.getStatus()).thenReturn("PENDING");
        when(steps.findAllByWorkflowRun_IdOrderByStepOrderAsc(42L)).thenReturn(List.of(active, pending));

        recovery.periodicRecovery();

        verify(active).fail(contains("Provider outcome may be unknown"));
        verify(pending).cancel();
        verify(run).cancel();
        verify(run, never()).fail(anyString());
        verify(steps).saveAll(List.of(active, pending));
        verify(runs).save(run);
    }
}
