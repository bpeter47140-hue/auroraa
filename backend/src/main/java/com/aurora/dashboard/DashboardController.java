package com.aurora.dashboard;
import com.aurora.tasks.TaskRepository;
import com.aurora.approvals.ApprovalRepository;
import com.aurora.businesses.BusinessRepository;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import com.aurora.security.WorkspaceContextHolder;
@RestController @RequestMapping("/api/v1/dashboard")
public class DashboardController {
 private final TaskRepository tasks; private final ApprovalRepository approvals; private final BusinessRepository businesses;
 public DashboardController(TaskRepository tasks,ApprovalRepository approvals,BusinessRepository businesses){this.tasks=tasks;this.approvals=approvals;this.businesses=businesses;}
 public record DashboardStats(long totalTasks,long pendingTasks,long inProgressTasks,long completedTasks,long pendingApprovals,long totalBusinesses,Instant generatedAt){}
 @GetMapping("/stats") public DashboardStats stats(){
  return new DashboardStats(tasks.countByWorkspace_Id(WorkspaceContextHolder.id()),tasks.countByWorkspace_IdAndStatus(WorkspaceContextHolder.id(),"PENDING"),tasks.countByWorkspace_IdAndStatus(WorkspaceContextHolder.id(),"IN_PROGRESS"),
   tasks.countByWorkspace_IdAndStatus(WorkspaceContextHolder.id(),"COMPLETED"),approvals.countByWorkspace_IdAndStatus(WorkspaceContextHolder.id(),"PENDING"),businesses.countByWorkspace_Id(WorkspaceContextHolder.id()),Instant.now());
 }
}
