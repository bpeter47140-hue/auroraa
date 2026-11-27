package com.aurora.projects;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class ProjectWorkflowRecovery {
    private final ProjectWorkflowRunRepository runs;
    private final ProjectWorkflowStepRepository steps;
    private final ProjectWorkflowService service;

    public ProjectWorkflowRecovery(ProjectWorkflowRunRepository runs, ProjectWorkflowStepRepository steps,
                                   ProjectWorkflowService service) {
        this.runs = runs; this.steps = steps; this.service = service;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void recoverAfterRestart() {
        recoverInterruptedRuns();
        recoverExpiredLeases();
        dispatchQueuedRuns();
    }

    @Scheduled(fixedDelay = 30000)
    public void periodicRecovery() {
        recoverInterruptedRuns();
        recoverExpiredLeases();
        dispatchQueuedRuns();
    }

    private void recoverInterruptedRuns() {
        Instant now = Instant.now();
        for (String status : List.of("PAUSE_REQUESTED", "CANCEL_REQUESTED")) {
            for (ProjectWorkflowRun run : runs.findAllByStatusOrderByCreatedAtAsc(status)) {
                // A different application instance may still own this workflow.
                // Never recover a pause/cancel request while its worker lease is live.
                Instant leaseExpiry = run.getLeaseExpiresAt();
                if (leaseExpiry != null && leaseExpiry.isAfter(now)) continue;

                List<ProjectWorkflowStep> workflowSteps = steps.findAllByWorkflowRun_IdOrderByStepOrderAsc(run.getId());
                ProjectWorkflowStep active = workflowSteps.stream().filter(step -> "RUNNING".equals(step.getStatus())).findFirst().orElse(null);
                if (active != null) {
                    active.fail("Worker lease expired while the workflow was stopping. Provider outcome may be unknown; never replay automatically.");
                    if ("CANCEL_REQUESTED".equals(status)) {
                        workflowSteps.stream().filter(step -> "PENDING".equals(step.getStatus())).forEach(ProjectWorkflowStep::cancel);
                        run.cancel();
                    } else {
                        run.fail("Workflow interrupted while pausing. The active stage was not replayed; reconcile provider usage before manual retry.");
                    }
                } else if ("PAUSE_REQUESTED".equals(status)) run.pause();
                else if ("CANCEL_REQUESTED".equals(status)) {
                    workflowSteps.stream().filter(step -> "PENDING".equals(step.getStatus())).forEach(ProjectWorkflowStep::cancel);
                    run.cancel();
                }
                steps.saveAll(workflowSteps);
                runs.save(run);
            }
        }
    }

    private void recoverExpiredLeases() {
        for (ProjectWorkflowRun run : runs.findAllByStatusAndLeaseExpiresAtBeforeOrderByCreatedAtAsc("RUNNING", Instant.now())) {
            List<ProjectWorkflowStep> workflowSteps = steps.findAllByWorkflowRun_IdOrderByStepOrderAsc(run.getId());
            ProjectWorkflowStep active = workflowSteps.stream().filter(step -> "RUNNING".equals(step.getStatus())).findFirst().orElse(null);
            if (active != null) active.fail("Worker lease expired. The external AI request may have completed or been billed; no automatic retry.");
            run.fail("Worker lease expired. Run stopped safely; inspect the active stage and reconcile cost before retry.");
            steps.saveAll(workflowSteps); runs.save(run);
        }
    }

    private void dispatchQueuedRuns() {
        for (ProjectWorkflowRun run : runs.findAllByStatusOrderByCreatedAtAsc("QUEUED")) service.executeAsync(run.getId());
    }
}
