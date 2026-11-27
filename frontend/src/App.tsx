import { useCallback, useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import './App.css'

type Task = { id: number; title: string; description: string | null; priority: string; status: string; createdAt: string }
type Approval = { id: number; title: string; description: string; actionType: string; status: string; createdAt: string; decisionNote?: string }
type AuditEvent = { id: number; eventType: string; entityType: string; entityId: string; details: string; createdAt: string }
type Business = { id: number; name: string; description: string | null; goal: string | null; status: string; createdAt: string }
type BusinessBudget = { businessId: number; monthlyLimitUsd: number; reportedSpendUsd: number; heldUsd: number; availableUsd: number; enabled: boolean; alertLevel: string; reservationConfigured: boolean }
type BudgetAlert = { id:number; businessId:number; businessName:string; periodStart:string; alertLevel:string; observedUsd:number; monthlyLimitUsd:number; createdAt:string; acknowledgedAt:string|null }
type BudgetReconciliation = { id: number; businessId: number | null; businessName: string | null; agentRunId: number; agentName: string; model: string; providerRequestId: string | null; usageStatus: string | null; reservedUsd: number; createdAt: string }
type Agent = { id: string; name: string; role: string; description: string; capabilities: string[]; status: string; modelConfigured: boolean; model: string }
type WorkflowResult = { workflow: string; status: string; runs: AgentRun[] }
type AgentRun = { id: number; businessId?: number | null; agentId: string; agentName: string; status: string; input: string; output: string | null; error: string | null; model: string | null; startedAt: string; finishedAt: string | null }
type AiUsage = { used: number; limit: number; remaining: number; periodStart: string; exempt: boolean; providerRequests?: number; reportedRequests?: number; requestsWithUnknownUsage?: number; promptTokens?: number; completionTokens?: number; totalTokens?: number; reportedCostUsd?: number; requestsWithoutReportedCost?: number; businessUsage?: { businessId: number | null; businessName: string | null; providerRequests: number; promptTokens: number; completionTokens: number; totalTokens: number; reportedCostUsd: number; requestsWithoutReportedCost: number }[] }
type Stats = { totalTasks: number; pendingTasks: number; inProgressTasks: number; completedTasks: number; pendingApprovals: number; totalBusinesses: number }
type Memory = { id: number; category: string; title: string; content: string; source: string | null; createdAt: string }
type CustomerInsight = { id: number; insightType: string; participantLabel: string; problem: string; currentSolution: string | null; frequency: string | null; willingness: string | null; evidence: string | null; validationStatus: string; createdAt: string }
type Member = { membershipId:number; userId:number; workspaceId:number; email:string; displayName:string; role:string; joinedAt:string }
type WorkspaceSummary = { id:number; name:string; role:string }
type CurrentUser = { user:{id:number;email:string;displayName:string;platformAdmin:boolean}; workspaces:WorkspaceSummary[] }
type Invitation = { id:number; email:string; role:string; createdAt:string; expiresAt:string; status:string }
type AdminAccess = { registeredAccounts:number; activeAccounts:number; disabledAccounts:number; accountsEverLoggedIn:number; pendingInvitations:number; onlineUsers:number; activeSessions:number; sessionsStartedLast30Days:number; sessions:{id:number;email:string;displayName:string;createdAt:string;lastSeenAt:string;expiresAt:string;online:boolean}[]; generatedAt:string; onlineWindowMinutes:number }
type VentureProject = { id:number; name:string; goal:string; projectType:string; status:string; createdAt:string; updatedAt:string }
type WorkflowStep = { id:number; stepOrder:number; dependsOnStepOrders:number[]; agentId:string; title:string; status:string; approvalRequired:boolean; approvalStatus:string|null; approvalDecidedAt:string|null; output:string|null; error:string|null; agentRunId:number|null; previousAgentRunId:number|null; retryCount:number; usageStatus:string|null; promptTokens:number|null; completionTokens:number|null; totalTokens:number|null; providerCostUsd:number|string|null; createdAt:string; startedAt:string|null; finishedAt:string|null }
type ProjectWorkflow = { run:{id:number; projectId:number; projectName:string; workflowType:string; status:string; createdAt:string; startedAt:string|null; finishedAt:string|null; error:string|null}; steps:WorkflowStep[] }
type WorkspaceFile = {path:string;content:string;sizeBytes:number}
type ProjectArtifact = {id:number;projectId:number;artifactType:string;name:string;relativePath:string;description:string;sizeBytes:number;createdAt:string}
type ProjectGeneration = {agentRunId:number;filesWritten:number;files:WorkspaceFile[]}
type PreviewBuild = {status:string;logs:string;builtAt:string}
type PreviewAccess = {url:string;expiresAt:string}
type ProjectReview = {id:number;projectId:number;artifactId:number;artifactName:string;artifactPath:string;artifactSha256:string;artifactVersionId:number|null;artifactVersionNumber:number|null;status:string;requestedBy:string;reviewedBy:string|null;feedback:string|null;createdAt:string;reviewedAt:string|null}
type ArtifactVersion = {id:number;artifactId:number;versionNumber:number;sha256:string;createdBy:string;createdAt:string}
type ArtifactVersionDetail = ArtifactVersion & {content:string}
type ProjectQualityResult = {agentId:string;agentRunId:number;reportArtifactId:number|null;status:string;report:string;notice:string}
type BrowserQAResult = {status:string;url:string;issues:{severity:string;type:string;message?:string;href?:string;status?:number;count?:number}[];checks:{name:string;passed?:boolean;count?:number;buttons?:number;forms?:number;fields?:number;scrollWidth?:number;innerWidth?:number}[];screenshots:{desktop?:{data:string};mobile?:{data:string}}}
type MarketingCampaign = {id:number;projectId:number;title:string;channel:string;format:string;content:string;callToAction:string|null;status:string;createdBy:string;reviewedBy:string|null;feedback:string|null;scheduledAt:string|null;approvedAt:string|null;createdAt:string;updatedAt:string}
type CampaignDraft = {title:string;channel:string;format:string;brief:string;content:string;callToAction:string}
type Tab = 'overview' | 'tasks' | 'approvals' | 'activity' | 'businesses' | 'projects' | 'agents' | 'memory' | 'research' | 'members' | 'settings' | 'admin'

// Keep the bearer token only for this browser tab. It survives refreshes but is
// cleared when the tab/session ends; it is never written to localStorage.
let sessionToken: string | null = sessionStorage.getItem('aurora.session')
let selectedWorkspaceId: number | null = Number(sessionStorage.getItem('aurora.workspace')) || null

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/+$/, '')
const apiUrl = (path: string) => `${API_BASE_URL}${path}`

const api = async <T,>(path: string, options?: RequestInit): Promise<T> => {
  const response = await fetch(apiUrl(path), { ...options, headers: { 'Content-Type': 'application/json', ...(sessionToken ? { Authorization: `Bearer ${sessionToken}` } : {}), ...(selectedWorkspaceId ? { 'X-Aurora-Workspace': String(selectedWorkspaceId) } : {}), ...options?.headers } })
  if (!response.ok) {
    const message = await response.text()
    throw new Error(`HTTP ${response.status}: ${message || 'Request failed'}`)
  }
  if (response.status === 204) return undefined as T
  return response.json() as Promise<T>
}

function App() {
  const [tab, setTab] = useState<Tab>('overview')
  const [dateLabel] = useState(() => new Intl.DateTimeFormat('en-GB', { weekday: 'long', month: 'long', day: 'numeric', year: 'numeric' }).format(new Date()).toUpperCase())
  const [tasks, setTasks] = useState<Task[]>([])
  const [approvals, setApprovals] = useState<Approval[]>([])
  const [activity, setActivity] = useState<AuditEvent[]>([])
  const [businesses, setBusinesses] = useState<Business[]>([])
  const [projects, setProjects] = useState<VentureProject[]>([])
  const [projectWorkflows, setProjectWorkflows] = useState<Record<number,ProjectWorkflow[]>>({})
  const [marketingCampaigns, setMarketingCampaigns] = useState<Record<number,MarketingCampaign[]>>({})
  const [campaignDrafts, setCampaignDrafts] = useState<Record<number,CampaignDraft>>({})
  const [campaignScheduleDrafts, setCampaignScheduleDrafts] = useState<Record<number,string>>({})
  const [campaignFeedback, setCampaignFeedback] = useState<Record<number,string>>({})
  const [campaignBusy, setCampaignBusy] = useState<number|null>(null)
  const [campaignGenerating, setCampaignGenerating] = useState<number|null>(null)
  const [campaignEditing, setCampaignEditing] = useState<Record<number,number|null>>({})
  const [showProjectForm, setShowProjectForm] = useState(false)
  const [projectName, setProjectName] = useState('')
  const [projectGoal, setProjectGoal] = useState('')
  const [projectType, setProjectType] = useState('WEBSITE')
  const [projectBusy, setProjectBusy] = useState(false)
  const [workflowBusyProject, setWorkflowBusyProject] = useState<number|null>(null)
  const [workflowTemplate, setWorkflowTemplate] = useState<Record<number,string>>({})
  const [workspaceFiles, setWorkspaceFiles] = useState<Record<number,WorkspaceFile[]>>({})
  const [sourceDrafts,setSourceDrafts] = useState<Record<string,string>>({})
  const [sourceBusy,setSourceBusy] = useState<string|null>(null)
  const [sourceVersions,setSourceVersions] = useState<Record<number,ArtifactVersion[]>>({})
  const [sourceVersionContent,setSourceVersionContent] = useState<Record<number,ArtifactVersionDetail>>({})
  const [qualityBusy,setQualityBusy] = useState<number|null>(null)
  const [qualityResults,setQualityResults] = useState<Record<number,ProjectQualityResult>>({})
  const [browserQAResults,setBrowserQAResults]=useState<Record<number,BrowserQAResult>>({})
  const [projectReviews, setProjectReviews] = useState<Record<number,ProjectReview[]>>({})
  const [projectArtifacts, setProjectArtifacts] = useState<Record<number,ProjectArtifact[]>>({})
  const [reviewFeedback, setReviewFeedback] = useState<Record<number,string>>({})
  const [reviewBusy, setReviewBusy] = useState<number|null>(null)
  const [workspaceBusy, setWorkspaceBusy] = useState<number|null>(null)
  const [previewBusyProject, setPreviewBusyProject] = useState<number|null>(null)
  const [previewBuilds, setPreviewBuilds] = useState<Record<number,PreviewBuild>>({})
  const [previewUrls, setPreviewUrls] = useState<Record<number,string>>({})
  const [previewModes, setPreviewModes] = useState<Record<number,'desktop'|'mobile'>>({})
  const [businessBudgets, setBusinessBudgets] = useState<Record<number, BusinessBudget>>({})
  const [budgetDrafts, setBudgetDrafts] = useState<Record<number, string>>({})
  const [budgetBusy, setBudgetBusy] = useState<number | null>(null)
  const [budgetReconciliations, setBudgetReconciliations] = useState<BudgetReconciliation[]>([])
  const [budgetAlerts, setBudgetAlerts] = useState<BudgetAlert[]>([])
  const [alertBusy, setAlertBusy] = useState<number | null>(null)
  const [reconciliationDrafts, setReconciliationDrafts] = useState<Record<number, { cost: string; reference: string; note: string }>>({})
  const [reconciliationBusy, setReconciliationBusy] = useState<number | null>(null)
  const [agents, setAgents] = useState<Agent[]>([])
  const [aiUsage, setAiUsage] = useState<AiUsage | null>(null)
  const [stats, setStats] = useState<Stats>({ totalTasks: 0, pendingTasks: 0, inProgressTasks: 0, completedTasks: 0, pendingApprovals: 0, totalBusinesses: 0 })
  const [online, setOnline] = useState(false)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)
  const [showTaskForm, setShowTaskForm] = useState(false)
  const [taskTitle, setTaskTitle] = useState('')
  const [taskDescription, setTaskDescription] = useState('')
  const [taskPriority, setTaskPriority] = useState('NORMAL')
  const [showBusinessForm, setShowBusinessForm] = useState(false)
  const [businessName, setBusinessName] = useState('')
  const [businessDescription, setBusinessDescription] = useState('')
  const [businessGoal, setBusinessGoal] = useState('')
  const [researchQuestion, setResearchQuestion] = useState('')
  const [researchReport, setResearchReport] = useState<AgentRun | null>(null)
  const [agentRuns, setAgentRuns] = useState<AgentRun[]>([])
  const [selectedAgentId, setSelectedAgentId] = useState('research-agent')
  const [selectedBusinessId, setSelectedBusinessId] = useState('')
  const [researchBusy, setResearchBusy] = useState(false)
  const [workflowBusy, setWorkflowBusy] = useState(false)
  const [workflowResult, setWorkflowResult] = useState<WorkflowResult | null>(null)
  const [memories, setMemories] = useState<Memory[]>([])
  const [memoryCategory, setMemoryCategory] = useState('FINDING')
  const [memoryTitle, setMemoryTitle] = useState('')
  const [memoryContent, setMemoryContent] = useState('')
  const [memorySource, setMemorySource] = useState('')
  const [memoryBusy, setMemoryBusy] = useState(false)
  const [customerInsights, setCustomerInsights] = useState<CustomerInsight[]>([])
  const [insightType, setInsightType] = useState('PROBLEM')
  const [participantLabel, setParticipantLabel] = useState('')
  const [insightProblem, setInsightProblem] = useState('')
  const [currentSolution, setCurrentSolution] = useState('')
  const [insightFrequency, setInsightFrequency] = useState('')
  const [insightWillingness, setInsightWillingness] = useState('')
  const [insightEvidence, setInsightEvidence] = useState('')
  const [validationStatus, setValidationStatus] = useState('REPORTED')
  const [insightBusy, setInsightBusy] = useState(false)
  const [analysisBusy, setAnalysisBusy] = useState(false)
  const [analysisResult, setAnalysisResult] = useState<AgentRun | null>(null)
  const [busy, setBusy] = useState(false)
  const [authenticated, setAuthenticated] = useState(false)
  const [workspaces, setWorkspaces] = useState<WorkspaceSummary[]>([])
  const [activeWorkspaceId, setActiveWorkspaceId] = useState<number | null>(null)
  const [currentRole, setCurrentRole] = useState('VIEWER')
  const [platformAdmin, setPlatformAdmin] = useState(false)
  const [adminAccess, setAdminAccess] = useState<AdminAccess | null>(null)
  const [adminAccessBusy, setAdminAccessBusy] = useState<number | null>(null)
  const [invitationToken, setInvitationToken] = useState<string | null>(() => new URLSearchParams(window.location.search).get('invite'))
  const [inviteAccountMode, setInviteAccountMode] = useState<'existing' | 'new'>('new')
  const [inviteNotice, setInviteNotice] = useState('')
  const [pendingInviteWorkspaceId, setPendingInviteWorkspaceId] = useState<number | null>(null)
  const [authMode, setAuthMode] = useState<'login' | 'register'>('login')
  const [registrationOpen, setRegistrationOpen] = useState(false)
  const [resetToken, setResetToken] = useState<string | null>(() => new URLSearchParams(window.location.search).get('reset'))
  const [resetRequestMode, setResetRequestMode] = useState(false)
  const [resetNotice, setResetNotice] = useState('')
  const [resetNewPassword, setResetNewPassword] = useState('')
  const [resetConfirmPassword, setResetConfirmPassword] = useState('')
  const [resetBusy, setResetBusy] = useState(false)
  const [authEmail, setAuthEmail] = useState('')
  const [authPassword, setAuthPassword] = useState('')
  const [displayName, setDisplayName] = useState('')
  const [workspaceName, setWorkspaceName] = useState('My workspace')
  const [authBusy, setAuthBusy] = useState(false)
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmNewPassword, setConfirmNewPassword] = useState('')
  const [passwordBusy, setPasswordBusy] = useState(false)
  const [passwordNotice, setPasswordNotice] = useState('')
  const [members, setMembers] = useState<Member[]>([])
  const [invitations, setInvitations] = useState<Invitation[]>([])
  const [inviteEmail, setInviteEmail] = useState('')
  const [inviteRole, setInviteRole] = useState('MEMBER')
  const [inviteBusy, setInviteBusy] = useState(false)
  const [memberBusy, setMemberBusy] = useState(false)
  const [createdInvite, setCreatedInvite] = useState<{email:string;token:string|null;expiresAt:string;emailSent:boolean}|null>(null)

  const refresh = useCallback(async () => {
    try {
      const [taskData, approvalData, activityData, statData, businessData, agentData, runData, memoryData, insightData, memberData, invitationData, currentUser, usageData] = await Promise.all([
        api<Task[]>('/api/v1/tasks'), api<Approval[]>('/api/v1/approvals'),
        api<AuditEvent[]>('/api/v1/audit'), api<Stats>('/api/v1/dashboard/stats'),
        api<Business[]>('/api/v1/businesses'), api<Agent[]>('/api/v1/agents'), api<AgentRun[]>('/api/v1/agent-runs'), api<Memory[]>('/api/v1/memory'), api<CustomerInsight[]>('/api/v1/customer-insights'), api<Member[]>('/api/v1/workspace/members'), api<Invitation[]>('/api/v1/workspace/invitations'), api<CurrentUser>('/api/v1/auth/me'), api<AiUsage>('/api/v1/ai-usage'),
      ])
      setTasks(taskData); setApprovals(approvalData); setActivity(activityData); setStats(statData)
      const budgetEntries = await Promise.all(businessData.map(async business => {
        try { return [business.id, await api<BusinessBudget>(`/api/v1/businesses/${business.id}/ai-budget`)] as const }
        catch { return [business.id, null] as const }
      }))
      const budgetMap = Object.fromEntries(budgetEntries.filter((entry): entry is readonly [number, BusinessBudget] => entry[1] !== null))
      setBusinessBudgets(budgetMap)
      setBudgetDrafts(current => Object.fromEntries(businessData.map(business => [business.id, current[business.id] ?? (budgetMap[business.id]?.enabled ? String(budgetMap[business.id].monthlyLimitUsd) : '')])))
      setBusinesses(businessData); setAgents(agentData); setAiUsage(usageData); setAgentRuns(runData); setMemories(memoryData); setCustomerInsights(insightData); setMembers(memberData); setInvitations(invitationData)
      setWorkspaces(currentUser.workspaces)
      setPlatformAdmin(currentUser.user.platformAdmin)
      if (currentUser.user.platformAdmin) {
        try { setAdminAccess(await api<AdminAccess>('/api/v1/admin/access')) }
        catch { setAdminAccess(null) }
      } else { setAdminAccess(null) }
      const activeMembership = currentUser.workspaces.find(workspace => workspace.id === selectedWorkspaceId) || currentUser.workspaces[0]
      if (activeMembership) {
        selectedWorkspaceId = activeMembership.id
        sessionStorage.setItem('aurora.workspace', String(activeMembership.id))
        setActiveWorkspaceId(activeMembership.id)
        setCurrentRole(activeMembership.role)
        if (activeMembership.role === 'OWNER' || activeMembership.role === 'ADMIN') {
          try {
            const [reconciliations, alerts] = await Promise.all([
              api<BudgetReconciliation[]>('/api/v1/ai-budget/reconciliation'),
              api<BudgetAlert[]>('/api/v1/ai-budget/alerts'),
            ])
            setBudgetReconciliations(reconciliations); setBudgetAlerts(alerts)
          } catch { setBudgetReconciliations([]); setBudgetAlerts([]) }
        } else { setBudgetReconciliations([]); setBudgetAlerts([]) }
      }
      setAuthenticated(true); setOnline(true); setError('')
    } catch (e) {
      const message = e instanceof Error ? e.message : 'Unable to connect to AURORA'
      if (message.includes('401')) {
        sessionToken = null
        selectedWorkspaceId = null
        sessionStorage.removeItem('aurora.session')
        sessionStorage.removeItem('aurora.workspace')
        setAuthenticated(false)
        setActiveWorkspaceId(null)
      }
      setOnline(false); setError(message)
    } finally { setLoading(false) }
  }, [])

  const submitAuth = async (event: FormEvent) => {
    event.preventDefault()
    setAuthBusy(true)
    setError('')
    try {
      const path = authMode === 'register' ? '/api/v1/auth/register' : '/api/v1/auth/login'
      const body = authMode === 'register'
        ? { email: authEmail, password: authPassword, displayName, workspaceName }
        : { email: authEmail, password: authPassword }
      const response = await fetch(apiUrl(path), {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
      })
      const responseText = await response.text()
      let result: { error?: string; token?: string; workspaces?: WorkspaceSummary[] }
      try {
        result = responseText ? JSON.parse(responseText) as typeof result : {}
      } catch {
        const readableBody = responseText.replace(/<[^>]*>/g, ' ').replace(/\s+/g, ' ').trim().slice(0, 180)
        const detail = response.status === 429
          ? 'Too many attempts. Please wait a minute before trying again.'
          : readableBody
            ? `HTTP ${response.status}: ${readableBody}`
            : `The server returned an empty response (HTTP ${response.status}). Please try again.`
        throw new Error(detail)
      }
      if (!response.ok) throw new Error(result.error || (response.status === 429 ? 'Too many attempts. Please wait a minute before trying again.' : `Authentication failed (HTTP ${response.status})`))
      if (typeof result.token !== 'string' || result.token.length < 40) {
        throw new Error('The server returned an invalid session. Please try again.')
      }
      sessionToken = result.token
      sessionStorage.setItem('aurora.session', result.token)
      setWorkspaces(result.workspaces || [])
      if (invitationToken && authMode === 'login') {
        const joined = await api<{workspaceId:number}>('/api/v1/workspace/invitations/accept', { method: 'POST', body: JSON.stringify({ token: invitationToken }) })
        selectedWorkspaceId = joined.workspaceId
        sessionStorage.setItem('aurora.workspace', String(joined.workspaceId))
        setActiveWorkspaceId(joined.workspaceId)
        setInviteNotice('Invitation accepted. You have joined the workspace.')
        setInvitationToken(null)
        window.history.replaceState({}, '', window.location.pathname)
      } else if (pendingInviteWorkspaceId) {
        selectedWorkspaceId = pendingInviteWorkspaceId
        sessionStorage.setItem('aurora.workspace', String(pendingInviteWorkspaceId))
        setActiveWorkspaceId(pendingInviteWorkspaceId)
        setPendingInviteWorkspaceId(null)
      } else if (result.workspaces?.length) {
        selectedWorkspaceId = result.workspaces[0].id
        sessionStorage.setItem('aurora.workspace', String(result.workspaces[0].id))
        setActiveWorkspaceId(result.workspaces[0].id)
        setCurrentRole(result.workspaces[0].role)
      }
      setAuthenticated(true)
      setAuthPassword('')
      await refresh()
    } catch (e) {
      sessionToken = null
      sessionStorage.removeItem('aurora.session')
      sessionStorage.removeItem('aurora.workspace')
      selectedWorkspaceId = null
      setAuthenticated(false)
      setError(e instanceof Error ? e.message : 'Authentication failed')
    } finally {
      setAuthBusy(false)
    }
  }

  const requestPasswordReset = async (event: FormEvent) => {
    event.preventDefault()
    setResetBusy(true); setResetNotice(''); setError('')
    try {
      const response = await fetch(apiUrl('/api/v1/auth/password/reset-request'), {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: authEmail }),
      })
      const result = await response.json().catch(() => ({}))
      if (!response.ok) throw new Error(result.message || result.error || 'Could not request a password reset.')
      setResetNotice('If an active account exists for that address, a reset link will be sent. Check your inbox and spam folder.')
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Could not request a password reset.')
    } finally { setResetBusy(false) }
  }

  const completePasswordReset = async (event: FormEvent) => {
    event.preventDefault()
    setError(''); setResetNotice('')
    if (resetNewPassword !== resetConfirmPassword) { setError('The new passwords do not match.'); return }
    if (resetNewPassword.length < 12) { setError('Use at least 12 characters.'); return }
    if (!resetToken) return
    setResetBusy(true)
    try {
      const response = await fetch(apiUrl('/api/v1/auth/password/reset'), {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ token: resetToken, newPassword: resetNewPassword }),
      })
      if (!response.ok) {
        const result = await response.json().catch(() => ({}))
        throw new Error(result.message || result.error || 'This reset link is invalid or expired.')
      }
      setResetToken(null); setResetNewPassword(''); setResetConfirmPassword('')
      sessionToken = null; selectedWorkspaceId = null
      sessionStorage.removeItem('aurora.session'); sessionStorage.removeItem('aurora.workspace')
      setResetNotice('Password updated. Sign in with your new password.')
      window.history.replaceState({}, '', window.location.pathname)
      setAuthMode('login')
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Could not reset password.')
    } finally { setResetBusy(false) }
  }

  const registerFromInvitation = async (event: FormEvent) => {
    event.preventDefault()
    if (!invitationToken) return
    setAuthBusy(true); setError(''); setInviteNotice('')
    try {
      const response = await fetch(apiUrl('/api/v1/workspace/invitations/register'), {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ token: invitationToken, displayName, password: authPassword }),
      })
      const joined = await response.json().catch(() => ({}))
      if (!response.ok) throw new Error(joined.message || joined.error || `Invitation registration failed (HTTP ${response.status})`)
      setAuthEmail(joined.email || '')
      setPendingInviteWorkspaceId(joined.workspaceId || null)
      setInvitationToken(null)
      window.history.replaceState({}, '', window.location.pathname)
      setAuthMode('login'); setAuthPassword('')
      setInviteNotice('Your account was created and the invitation accepted. Sign in with the email address the invitation was sent to.')
    } catch (e) { setError(e instanceof Error ? e.message : 'Could not accept invitation') }
    finally { setAuthBusy(false) }
  }

  const changePassword = async (event: FormEvent) => {
    event.preventDefault(); setPasswordNotice(''); setError('')
    if (newPassword !== confirmNewPassword) { setError('The new passwords do not match.'); return }
    if (newPassword.length < 12) { setError('Use at least 12 characters.'); return }
    setPasswordBusy(true)
    try { await api<void>('/api/v1/auth/password', { method: 'POST', body: JSON.stringify({ currentPassword, newPassword }) }); setCurrentPassword(''); setNewPassword(''); setConfirmNewPassword(''); setPasswordNotice('Password updated. Other active sessions have been signed out.') }
    catch (e) { setError(e instanceof Error ? e.message : 'Could not update password') }
    finally { setPasswordBusy(false) }
  }

  const logout = async () => {
    try { await api<void>('/api/v1/auth/logout', { method: 'POST' }) }
    catch { /* Clear the local session even if the server is unreachable. */ }
    sessionToken = null
    selectedWorkspaceId = null
    sessionStorage.removeItem('aurora.session')
    sessionStorage.removeItem('aurora.workspace')
    setActiveWorkspaceId(null); setWorkspaces([]); setCurrentRole('VIEWER')
    setAuthenticated(false)
    setOnline(false)
    setTasks([]); setApprovals([]); setActivity([]); setBusinesses([])
    setAgents([]); setAgentRuns([]); setMemories([]); setCustomerInsights([])
    setAuthEmail(''); setAuthPassword(''); setError('')
  }

  useEffect(() => {
    let active = true
    fetch(apiUrl('/api/v1/auth/bootstrap'))
      .then(response => {
        if (!response.ok) throw new Error('Backend returned HTTP ' + response.status)
        return response.json()
      })
      .then(data => {
        if (!active) return
        const open = Boolean(data.registrationOpen)
        setRegistrationOpen(open)
        setAuthMode('login')
        if (sessionToken && !invitationToken && !resetToken) setAuthenticated(true)
        else setLoading(false)
      })
      .catch(() => {
        if (active) {
          setError('Cannot reach AURORA. Check that the backend is running.')
          setOnline(false)
          setLoading(false)
        }
      })
    return () => { active = false }
  }, [invitationToken, resetToken])

  useEffect(() => {
    if (!authenticated) return
    let active = true
    void Promise.resolve().then(() => { if (active) void refresh() })
    const timer = window.setInterval(() => void refresh(), 15000)
    return () => { active = false; window.clearInterval(timer) }
  }, [authenticated, refresh])

  const createTask = async (event: FormEvent) => {
    event.preventDefault()
    if (!taskTitle.trim()) return
    setBusy(true)
    try {
      await api<Task>('/api/v1/tasks', { method: 'POST', body: JSON.stringify({ title: taskTitle, description: taskDescription, priority: taskPriority }) })
      setTaskTitle(''); setTaskDescription(''); setTaskPriority('NORMAL'); setShowTaskForm(false)
      await refresh()
    } catch (e) { setError(e instanceof Error ? e.message : 'Could not create task') }
    finally { setBusy(false) }
  }

  const removeBusinessBudget = async (businessId: number) => {
    setBudgetBusy(businessId); setError('')
    try { await api<void>(`/api/v1/businesses/${businessId}/ai-budget`, { method: 'DELETE' }); setBusinessBudgets(current => ({ ...current, [businessId]: { businessId, monthlyLimitUsd: 0, reportedSpendUsd: 0, heldUsd: 0, availableUsd: 0, enabled: false, alertLevel: 'DISABLED', reservationConfigured: true } })); setBudgetDrafts(current => ({ ...current, [businessId]: '' })) }
    catch (e) { setError(e instanceof Error ? e.message : 'Could not remove business AI budget.') }
    finally { setBudgetBusy(null) }
  }

  const acknowledgeBudgetAlert = async (alert: BudgetAlert) => {
    setAlertBusy(alert.id); setError('')
    try {
      await api<void>(`/api/v1/ai-budget/alerts/${alert.id}/acknowledge`, { method: 'POST' })
      setBudgetAlerts(current => current.map(item => item.id === alert.id ? { ...item, acknowledgedAt: new Date().toISOString() } : item))
    } catch (e) { setError(e instanceof Error ? e.message : 'Could not acknowledge this alert.') }
    finally { setAlertBusy(null) }
  }

  const reconcileBudgetReservation = async (item: BudgetReconciliation) => {
    const draft = reconciliationDrafts[item.id]
    if (!draft || !draft.cost.trim() || !draft.reference.trim() || draft.note.trim().length < 10) {
      setError('Enter the confirmed USD cost, provider billing reference and at least 10 characters explaining the evidence.')
      return
    }
    const amount = Number(draft.cost)
    if (!Number.isFinite(amount) || amount < 0 || amount > 99999999) { setError('Enter a valid non-negative USD amount.'); return }
    setReconciliationBusy(item.id); setError('')
    try {
      await api<void>(`/api/v1/ai-budget/reconciliation/${item.id}`, { method: 'POST', body: JSON.stringify({ actualCostUsd: draft.cost, providerReference: draft.reference.trim(), evidenceNote: draft.note.trim() }) })
      setBudgetReconciliations(current => current.filter(entry => entry.id !== item.id))
      setReconciliationDrafts(current => { const next = { ...current }; delete next[item.id]; return next })
      await refresh()
    } catch (e) { setError(e instanceof Error ? e.message : 'Could not reconcile this reservation.') }
    finally { setReconciliationBusy(null) }
  }

  const saveBusinessBudget = async (businessId: number) => {
    const value = Number(budgetDrafts[businessId])
    if (!Number.isFinite(value) || value <= 0) { setError('Enter a positive monthly budget in USD.'); return }
    setBudgetBusy(businessId); setError('')
    try {
      const result = await api<BusinessBudget>(`/api/v1/businesses/${businessId}/ai-budget`, {
        method: 'PUT', body: JSON.stringify({ monthlyLimitUsd: value.toFixed(10) }),
      })
      setBusinessBudgets(current => ({ ...current, [businessId]: result }))
      setBudgetDrafts(current => ({ ...current, [businessId]: String(result.monthlyLimitUsd) }))
    } catch (e) { setError(e instanceof Error ? e.message : 'Could not save business AI budget.') }
    finally { setBudgetBusy(null) }
  }

  const createBusiness = async (event: FormEvent) => {
    event.preventDefault()
    if (!businessName.trim()) return
    setBusy(true)
    try {
      await api<Business>('/api/v1/businesses', { method: 'POST', body: JSON.stringify({ name: businessName, description: businessDescription, goal: businessGoal, status: 'IDEA' }) })
      setBusinessName(''); setBusinessDescription(''); setBusinessGoal(''); setShowBusinessForm(false)
      await refresh()
    } catch (e) { setError(e instanceof Error ? e.message : 'Could not create business') }
    finally { setBusy(false) }
  }

  const refreshProjects = useCallback(async () => {
    const data = await api<VentureProject[]>('/api/v1/projects')
    setProjects(data)
    const pairs = await Promise.all(data.map(async project => {
      try { return [project.id, await api<ProjectWorkflow[]>(`/api/v1/projects/${project.id}/workflows`)] as const }
      catch { return [project.id, []] as const }
    }))
    setProjectWorkflows(Object.fromEntries(pairs))
    const campaignPairs = await Promise.all(data.map(async project => {
      try { return [project.id, await api<MarketingCampaign[]>(`/api/v1/projects/${project.id}/campaigns`)] as const }
      catch { return [project.id, []] as const }
    }))
    setMarketingCampaigns(Object.fromEntries(campaignPairs))
    const workspaces = await Promise.all(data.map(async project => {
      try {
        const artifacts = await api<ProjectArtifact[]>(`/api/v1/projects/${project.id}/workspace/artifacts`)
        if (!artifacts.length) return [project.id, []] as const
        const files = await api<WorkspaceFile[]>(`/api/v1/projects/${project.id}/workspace/files`)
        return [project.id, files] as const
      } catch { return [project.id, []] as const }
    }))
    setWorkspaceFiles(Object.fromEntries(workspaces))
    const artifactPairs = await Promise.all(data.map(async project => {
      try { return [project.id, await api<ProjectArtifact[]>(`/api/v1/projects/${project.id}/workspace/artifacts`)] as const }
      catch { return [project.id, []] as const }
    }))
    setProjectArtifacts(Object.fromEntries(artifactPairs))
    const reviewPairs = await Promise.all(data.map(async project => {
      try { return [project.id, await api<ProjectReview[]>(`/api/v1/projects/${project.id}/reviews`)] as const }
      catch { return [project.id, []] as const }
    }))
    setProjectReviews(Object.fromEntries(reviewPairs))
  }, [])

  useEffect(() => {
    if (!authenticated || tab !== 'projects') return
    let active = true
    void Promise.resolve().then(() => {
      if (active) void refreshProjects().catch(e => setError(e instanceof Error ? e.message : 'Could not load projects'))
    })
    const timer = window.setInterval(() => {
      if (active) void refreshProjects().catch(e => setError(e instanceof Error ? e.message : 'Could not refresh project progress'))
    }, 10000)
    return () => { active = false; window.clearInterval(timer) }
  }, [authenticated, tab, activeWorkspaceId, refreshProjects])

  const createProject = async (event: FormEvent) => {
    event.preventDefault()
    if (!projectName.trim() || !projectGoal.trim()) return
    setProjectBusy(true); setError('')
    try {
      await api<VentureProject>('/api/v1/projects', { method:'POST', body:JSON.stringify({name:projectName.trim(),goal:projectGoal.trim(),projectType}) })
      setProjectName(''); setProjectGoal(''); setProjectType('WEBSITE'); setShowProjectForm(false)
      await refreshProjects()
    } catch(e) { setError(e instanceof Error ? e.message : 'Could not create project') }
    finally { setProjectBusy(false) }
  }

  const updateProjectStatus = async (project:VentureProject, status:string) => {
    try {
      await api<VentureProject>(`/api/v1/projects/${project.id}/status`, {method:'PATCH',body:JSON.stringify({status})})
      await refreshProjects()
    } catch(e) { setError(e instanceof Error ? e.message : 'Could not update project status') }
  }

  const createProjectWorkspace = async (projectId:number) => {
    setWorkspaceBusy(projectId); setError('')
    try {
      await api<ProjectArtifact[]>('/api/v1/projects/' + projectId + '/workspace/scaffold', {method:'POST'})
      const files = await api<WorkspaceFile[]>('/api/v1/projects/' + projectId + '/workspace/files')
      setWorkspaceFiles(current=>({...current,[projectId]:files}))
    } catch(e) { setError(e instanceof Error ? e.message : 'Could not scaffold workspace') }
    finally { setWorkspaceBusy(null) }
  }

  const exportProjectWorkspace = async (projectId:number) => {
    try {
      const response = await fetch(apiUrl('/api/v1/projects/' + projectId + '/workspace/export.zip'), {headers:{...(sessionToken?{Authorization:'Bearer '+sessionToken}:{}),...(selectedWorkspaceId?{'X-Aurora-Workspace':String(selectedWorkspaceId)}:{})}})
      if(!response.ok) throw new Error('Could not export project workspace')
      const blob=await response.blob();const url=URL.createObjectURL(blob);const anchor=document.createElement('a');anchor.href=url;anchor.download='aurora-project-'+projectId+'.zip';document.body.appendChild(anchor);anchor.click();anchor.remove();window.setTimeout(()=>URL.revokeObjectURL(url),1000)
    } catch(e) { setError(e instanceof Error ? e.message : 'Export failed') }
  }

  const generateProjectWebsite = async (project:VentureProject) => {
    setWorkspaceBusy(project.id);setError('')
    try {
      const result=await api<ProjectGeneration>('/api/v1/projects/'+project.id+'/workspace/generate',{method:'POST',body:JSON.stringify({brief:'Design and implement a complete responsive website for '+project.name+'. Goal: '+project.goal+'. Create a polished, professional, modern frontend with a cohesive visual system, real page sections, mobile responsiveness and interactive navigation where useful. Avoid placeholder-only content.'})})
      setWorkspaceFiles(current=>({...current,[project.id]:result.files}))
      setSourceDrafts(current=>Object.fromEntries(Object.entries(current).filter(([key])=>!key.startsWith(project.id+":"))))
      setPreviewUrls(current=>({...current,[project.id]:''}))
      setPreviewBuilds(current=>({...current,[project.id]:{status:'NOT_BUILT',logs:'New source generated. Build to inspect this version.',builtAt:new Date().toISOString()}}))
      await refreshProjects()
    }catch(e){setError(e instanceof Error?e.message:'Website generation failed')}
    finally{setWorkspaceBusy(null)}
  }

  const buildProjectPreview = async (projectId:number) => {
    setPreviewBusyProject(projectId);setError('')
    try {
      const result=await api<PreviewBuild>(`/api/v1/projects/${projectId}/workspace/build`,{method:'POST'})
      setPreviewBuilds(current=>({...current,[projectId]:result}))
      if(result.status==='SUCCEEDED'){
        const access=await api<PreviewAccess>(`/api/v1/projects/${projectId}/workspace/preview-access`,{method:'POST'})
        setPreviewUrls(current=>({...current,[projectId]:access.url}))
      } else setPreviewUrls(current=>({...current,[projectId]:''}))
    } catch(e) { setError(e instanceof Error?e.message:'Website build failed') }
    finally { setPreviewBusyProject(null) }
  }

  const refreshProjectPreviewAccess = async (projectId:number) => {
    try {
      const access=await api<PreviewAccess>(`/api/v1/projects/${projectId}/workspace/preview-access`,{method:'POST'})
      setPreviewUrls(current=>({...current,[projectId]:access.url}))
    } catch(e) { setError(e instanceof Error?e.message:'Could not refresh preview link') }
  }

  const saveProjectSource = async (projectId:number,file:WorkspaceFile) => {
    const key=projectId+":"+file.path;const content=sourceDrafts[key]??file.content
    setSourceBusy(key);setError('')
    try {
      await api<WorkspaceFile>(`/api/v1/projects/${projectId}/workspace/file`,{method:'PUT',body:JSON.stringify({path:file.path,content})})
      setSourceDrafts(current=>{const next={...current};delete next[key];return next})
      const files=await api<WorkspaceFile[]>(`/api/v1/projects/${projectId}/workspace/files`);setWorkspaceFiles(current=>({...current,[projectId]:files}))
      const artifact=projectArtifacts[projectId]?.find(a=>a.relativePath===file.path)
      if(artifact){const versions=await api<ArtifactVersion[]>(`/api/v1/projects/${projectId}/workspace/artifacts/${artifact.id}/versions`);setSourceVersions(current=>({...current,[artifact.id]:versions}))}
      setPreviewUrls(current=>({...current,[projectId]:''}));setPreviewBuilds(current=>({...current,[projectId]:{status:'NOT_BUILT',logs:'Source changed. Build again to inspect the latest version.',builtAt:new Date().toISOString()}}))
    } catch(e) {setError(e instanceof Error?e.message:'Could not save source file')}
    finally {setSourceBusy(null)}
  }
  const runProjectQualityReview = async (projectId:number,agentId:'qa-agent'|'security-agent') => {
    setQualityBusy(projectId);setError('')
    try {
      const result=await api<ProjectQualityResult>(`/api/v1/projects/${projectId}/quality/${agentId}`,{method:'POST',body:JSON.stringify({})})
      setQualityResults(current=>({...current,[projectId]:result}))
      await refreshProjects()
    } catch(e) {setError(e instanceof Error?e.message:'Quality review failed')}
    finally {setQualityBusy(null)}
  }
  const runBrowserQA=async(projectId:number)=>{setQualityBusy(projectId);setError('');try{const result=await api<BrowserQAResult>(`/api/v1/projects/${projectId}/quality/browser`,{method:'POST',body:'{}'});setBrowserQAResults(current=>({...current,[projectId]:result}));await refreshProjects()}catch(e){setError(e instanceof Error?e.message:'Browser QA failed')}finally{setQualityBusy(null)}}
  const loadSourceVersions = async (projectId:number,artifactId:number) => {
    try {const versions=await api<ArtifactVersion[]>(`/api/v1/projects/${projectId}/workspace/artifacts/${artifactId}/versions`);setSourceVersions(current=>({...current,[artifactId]:versions}))}
    catch(e){setError(e instanceof Error?e.message:'Could not load version history')}
  }
  const inspectSourceVersion = async (projectId:number,artifactId:number,versionId:number) => {
    try {const version=await api<ArtifactVersionDetail>(`/api/v1/projects/${projectId}/workspace/artifacts/${artifactId}/versions/${versionId}`);setSourceVersionContent(current=>({...current,[artifactId]:version}))}
    catch(e){setError(e instanceof Error?e.message:'Could not open saved version')}
  }
  const requestProjectReview = async (projectId:number, artifactId:number) => {
    setReviewBusy(artifactId); setError('')
    try {
      await api<ProjectReview>(`/api/v1/projects/${projectId}/reviews`, {method:'POST',body:JSON.stringify({artifactId})})
      await refreshProjects()
    } catch(e) { setError(e instanceof Error ? e.message : 'Could not request review') }
    finally { setReviewBusy(null) }
  }

  const decideProjectReview = async (projectId:number, review:ProjectReview, decision:string) => {
    const feedback=reviewFeedback[review.id]||''
    if(decision!=='APPROVED'&&!feedback.trim()){setError('Please add feedback when requesting changes or rejecting work.');return}
    setReviewBusy(review.id); setError('')
    try {
      await api<ProjectReview>(`/api/v1/projects/${projectId}/reviews/${review.id}/decision`,{method:'POST',body:JSON.stringify({decision,feedback})})
      setReviewFeedback(current=>({...current,[review.id]:''}))
      await refreshProjects()
    } catch(e) { setError(e instanceof Error ? e.message : 'Could not save review decision') }
    finally { setReviewBusy(null) }
  }

  const startProjectWorkflow = async (projectId:number) => {
    setWorkflowBusyProject(projectId); setError('')
    try {
      await api<ProjectWorkflow>(`/api/v1/projects/${projectId}/workflows`, {method:'POST',body:JSON.stringify({template:workflowTemplate[projectId]||'website-planning'})})
      await refreshProjects()
    } catch(e) { setError(e instanceof Error ? e.message : 'Could not start agent workflow') }
    finally { setWorkflowBusyProject(null) }
  }

  const campaignDraftFor = (projectId:number):CampaignDraft => campaignDrafts[projectId] || {title:'',channel:'INSTAGRAM',format:'POST',brief:'',content:'',callToAction:''}
  const updateCampaignDraft = (projectId:number, patch:Partial<CampaignDraft>) => setCampaignDrafts(current=>{
    const existing=current[projectId]??{title:'',channel:'INSTAGRAM',format:'POST',brief:'',content:'',callToAction:''}
    return {...current,[projectId]:{...existing,...patch}}
  })

  const generateCampaignCopy = async (project:VentureProject) => {
    const draft=campaignDraftFor(project.id)
    if(!draft.brief.trim()){setError('Add a short creative brief before asking the Content Agent to generate copy.');return}
    setCampaignGenerating(project.id); setError('')
    try {
      const result=await api<{agentRunId:number;content:string}>(`/api/v1/projects/${project.id}/campaigns/generate-copy`,{method:'POST',body:JSON.stringify({channel:draft.channel,format:draft.format,brief:draft.brief})})
      updateCampaignDraft(project.id,{content:result.content})
    } catch(e) { setError(e instanceof Error?e.message:'Could not generate campaign copy') }
    finally { setCampaignGenerating(null) }
  }

  const createCampaign = async (event:FormEvent, projectId:number) => {
    event.preventDefault(); const draft=campaignDraftFor(projectId)
    if(!draft.title.trim()||!draft.content.trim()) return
    setCampaignBusy(projectId); setError('')
    try {
      const editingId=campaignEditing[projectId]
      const payload={title:draft.title,channel:draft.channel,format:draft.format,content:draft.content,callToAction:draft.callToAction}
      await api<MarketingCampaign>(editingId?`/api/v1/projects/${projectId}/campaigns/${editingId}`:`/api/v1/projects/${projectId}/campaigns`,{method:editingId?'PUT':'POST',body:JSON.stringify(payload)})
      setCampaignDrafts(current=>({...current,[projectId]:{title:'',channel:'INSTAGRAM',format:'POST',brief:'',content:'',callToAction:''}}))
      setCampaignEditing(current=>({...current,[projectId]:null}))
      await refreshProjects()
    } catch(e) { setError(e instanceof Error?e.message:'Could not save campaign draft') }
    finally { setCampaignBusy(null) }
  }

  const editCampaignDraft = (projectId:number, campaign:MarketingCampaign) => {
    setCampaignEditing(current=>({...current,[projectId]:campaign.id}))
    setCampaignDrafts(current=>({...current,[projectId]:{title:campaign.title,channel:campaign.channel,format:campaign.format,brief:'',content:campaign.content,callToAction:campaign.callToAction||''}}))
  }

  const submitCampaign = async (projectId:number, campaignId:number) => {
    setCampaignBusy(campaignId); setError('')
    try { await api<MarketingCampaign>(`/api/v1/projects/${projectId}/campaigns/${campaignId}/submit`,{method:'POST'}); await refreshProjects() }
    catch(e) { setError(e instanceof Error?e.message:'Could not submit campaign for review') }
    finally { setCampaignBusy(null) }
  }

  const decideCampaign = async (projectId:number, campaignId:number, decision:'APPROVED'|'CHANGES_REQUESTED'|'REJECTED') => {
    const feedback=campaignFeedback[campaignId]||''
    if(decision!=='APPROVED'&&!feedback.trim()){setError('Add feedback when requesting changes or rejecting a campaign.');return}
    setCampaignBusy(campaignId); setError('')
    try {
      await api<MarketingCampaign>(`/api/v1/projects/${projectId}/campaigns/${campaignId}/decision`,{method:'POST',body:JSON.stringify({decision,feedback})})
      setCampaignFeedback(current=>({...current,[campaignId]:''})); await refreshProjects()
    } catch(e) { setError(e instanceof Error?e.message:'Could not save campaign decision') }
    finally { setCampaignBusy(null) }
  }

  const scheduleCampaign = async (projectId:number, campaignId:number) => {
    const value=campaignScheduleDrafts[campaignId]
    if(!value){setError('Choose a future date and time first.');return}
    setCampaignBusy(campaignId); setError('')
    try {
      await api<MarketingCampaign>(`/api/v1/projects/${projectId}/campaigns/${campaignId}/schedule`,{method:'POST',body:JSON.stringify({scheduledAt:new Date(value).toISOString()})})
      await refreshProjects()
    } catch(e) { setError(e instanceof Error?e.message:'Could not queue campaign') }
    finally { setCampaignBusy(null) }
  }

  const approveWorkflowStep = async (projectId:number, workflowId:number, stepId:number) => {
    try {
      await api<ProjectWorkflow>(`/api/v1/projects/${projectId}/workflows/${workflowId}/steps/${stepId}/approve`, {method:'POST'})
      await refreshProjects()
    } catch(e) { setError(e instanceof Error ? e.message : 'Could not approve workflow step') }
  }

  const retryWorkflowStep = async (projectId:number, workflowId:number, stepId:number) => {
    if (!window.confirm('Retry this failed stage? A new AI run may incur provider charges. The previous run and its usage record will be retained.')) return
    try {
      await api<ProjectWorkflow>(`/api/v1/projects/${projectId}/workflows/${workflowId}/steps/${stepId}/retry`, {method:'POST'})
      await refreshProjects()
    } catch(e) { setError(e instanceof Error ? e.message : 'Could not safely retry workflow step') }
  }

  const cancelProjectWorkflow = async (projectId:number, workflowId:number) => {
    try {
      await api<ProjectWorkflow>(`/api/v1/projects/${projectId}/workflows/${workflowId}/cancel`, {method:'POST'})
      await refreshProjects()
    } catch(e) { setError(e instanceof Error ? e.message : 'Could not cancel workflow') }
  }

  const controlProjectWorkflow = async (projectId:number, workflowId:number, action:'pause'|'resume') => {
    try {
      await api<ProjectWorkflow>(`/api/v1/projects/${projectId}/workflows/${workflowId}/${action}`, {method:'POST'})
      await refreshProjects()
    } catch(e) { setError(e instanceof Error ? e.message : `Could not ${action} workflow`) }
  }

  const runResearch = async (event: FormEvent) => {
    event.preventDefault()
    if (!researchQuestion.trim()) return
    setResearchBusy(true); setResearchReport(null)
    try {
      const result = await api<AgentRun>('/api/v1/agent-runs', { method: 'POST', body: JSON.stringify({ agentId: selectedAgentId, input: researchQuestion, businessId: selectedBusinessId ? Number(selectedBusinessId) : null }) })
      setResearchReport(result); await refresh()
      if (result.status === 'FAILED') setError(result.error || 'Agent execution failed')
    } catch (e) { setError(e instanceof Error ? e.message : 'Research Agent could not run') }
    finally { setResearchBusy(false) }
  }

  const saveInsight = async (event: FormEvent) => {
    event.preventDefault()
    if (!participantLabel.trim() || !insightProblem.trim()) return
    setInsightBusy(true)
    try {
      await api<CustomerInsight>('/api/v1/customer-insights', { method: 'POST', body: JSON.stringify({ insightType, participantLabel, problem: insightProblem, currentSolution, frequency: insightFrequency, willingness: insightWillingness, evidence: insightEvidence, validationStatus }) })
      setParticipantLabel(''); setInsightProblem(''); setCurrentSolution(''); setInsightFrequency(''); setInsightWillingness(''); setInsightEvidence('')
      await refresh()
    } catch (e) { setError(e instanceof Error ? e.message : 'Could not save customer research') }
    finally { setInsightBusy(false) }
  }

  const exportCustomerInsights = () => {
    const columns: (keyof CustomerInsight)[] = ['id','insightType','participantLabel','problem','currentSolution','frequency','willingness','evidence','validationStatus','createdAt']
    const escapeCsv = (value: unknown) => '"' + String(value ?? '').replaceAll('"', '""') + '"'
    const csv = [columns.join(','), ...customerInsights.map(item => columns.map(column => escapeCsv(item[column])).join(','))].join('\r\n')
    const url = URL.createObjectURL(new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8' }))
    const link = document.createElement('a')
    link.href = url; link.download = 'aurora-customer-research.csv'; link.click()
    URL.revokeObjectURL(url)
  }

  const analyseCustomerEvidence = async () => {
    if (customerInsights.length === 0) return
    setAnalysisBusy(true); setAnalysisResult(null); setError('')
    try {
      const result = await api<AgentRun>('/api/v1/agent-runs', { method: 'POST', body: JSON.stringify({
        agentId: 'research-agent',
        businessId: selectedBusinessId ? Number(selectedBusinessId) : null,
        input: 'Analyse the customer research records currently available in AURORA shared research context for our student-focused chama contribution tracker hypothesis in Kenya. Produce: (1) number and types of records and participants, (2) recurring problems with counts only where supported, (3) current workarounds, (4) evidence of frequency and willingness to pay/try, (5) contradictions and negative evidence, (6) what remains unknown, (7) confidence level with reasons, (8) the next five highest-value interview questions, and (9) a go / continue discovery / pivot recommendation with explicit stop conditions. Separate direct observations from participant reports and unverified notes. Do not invent counts, market size, competitor research or demand. The current sample is small and non-random; say so.'
      }) })
      setAnalysisResult(result); await refresh()
    } catch (e) { setError(e instanceof Error ? e.message : 'Could not analyse research') }
    finally { setAnalysisBusy(false) }
  }

  const deleteInsight = async (id: number) => {
    if (!window.confirm('Delete this customer research record?')) return
    try { await api<void>('/api/v1/customer-insights/' + id, { method: 'DELETE' }); await refresh() }
    catch (e) { setError(e instanceof Error ? e.message : 'Could not delete research record') }
  }

  const saveMemory = async (event: FormEvent) => {
    event.preventDefault()
    if (!memoryTitle.trim() || !memoryContent.trim()) return
    setMemoryBusy(true)
    try {
      await api<Memory>('/api/v1/memory', { method: 'POST', body: JSON.stringify({ category: memoryCategory, title: memoryTitle, content: memoryContent, source: memorySource || null }) })
      setMemoryTitle(''); setMemoryContent(''); setMemorySource('')
      await refresh()
    } catch (e) { setError(e instanceof Error ? e.message : 'Could not save memory') }
    finally { setMemoryBusy(false) }
  }

  const deleteMemory = async (id: number) => {
    if (!window.confirm('Delete this shared memory? Agents will no longer receive it as context.')) return
    try { await api<void>('/api/v1/memory/' + id, { method: 'DELETE' }); await refresh() }
    catch (e) { setError(e instanceof Error ? e.message : 'Could not delete memory') }
  }

  const runChamaWorkflow = async () => {
    setWorkflowBusy(true); setWorkflowResult(null); setError('')
    try {
      const result = await api<WorkflowResult>('/api/v1/agent-workflows/chama-validation', {
        method: 'POST',
        body: JSON.stringify({ businessId: selectedBusinessId ? Number(selectedBusinessId) : null, brief: 'Evaluate whether a new student-focused chama or savings-group product in Kenya is justified. Compare at least 8 relevant Kenyan competitors and substitutes where evidence exists, including actual published pricing and limits, verified features, onboarding, platform availability, and student-specific suitability. Separate general chama-market evidence from evidence about college and university students. Identify only evidence-supported unmet needs, rank three opportunities, show disconfirming evidence, and recommend BUILD A TEST, PIVOT, or STOP. We have a very limited budget. Do not assume demand, M-Pesa integration, regulatory clearance, or competitor capabilities. Prioritise direct customer validation before software development.' })
      })
      setWorkflowResult(result)
      await refresh()
      if (result.status !== 'SUCCEEDED') setError('The workflow stopped early. Review the agent run results below.')
    } catch (e) { setError(e instanceof Error ? e.message : 'Could not run the workflow') }
    finally { setWorkflowBusy(false) }
  }

  const changeBusinessStatus = async (business: Business, status: string) => {
    try {
      await api<Business>(`/api/v1/businesses/${business.id}`, { method: 'PUT', body: JSON.stringify({ name: business.name, description: business.description, goal: business.goal, status }) })
      await refresh()
    } catch (e) { setError(e instanceof Error ? e.message : 'Could not update business') }
  }

  const changeTaskStatus = async (task: Task, status: string) => {
    try {
      await api<Task>(`/api/v1/tasks/${task.id}/status`, { method: 'PATCH', body: JSON.stringify({ status }) })
      await refresh()
    } catch (e) { setError(e instanceof Error ? e.message : 'Could not update task') }
  }

  const decideApproval = async (approval: Approval, decision: 'approve' | 'reject') => {
    const confirmed = window.confirm(`${decision === 'approve' ? 'Approve' : 'Reject'} this request?\n\n${approval.title}\n\nThis records your decision. It does not automatically transfer money or execute an external action.`)
    if (!confirmed) return
    try {
      await api<Approval>(`/api/v1/approvals/${approval.id}/${decision}`, { method: 'POST', body: JSON.stringify({ note: 'Owner decision from AURORA dashboard' }) })
      await refresh()
    } catch (e) { setError(e instanceof Error ? e.message : 'Could not record decision') }
  }

  const createInvitation = async (event: FormEvent) => {
    event.preventDefault(); setInviteBusy(true); setCreatedInvite(null); setError('')
    try {
      const result = await api<{invitation:Invitation;invitationToken:string;emailSent:boolean}>('/api/v1/workspace/invitations', {method:'POST',body:JSON.stringify({email:inviteEmail,role:inviteRole})})
      setCreatedInvite({email:result.invitation.email,token:result.invitationToken ?? null,expiresAt:result.invitation.expiresAt,emailSent:result.emailSent}); setInviteEmail(''); await refresh()
    } catch(e) { setError(e instanceof Error ? e.message : 'Could not create invitation') }
    finally { setInviteBusy(false) }
  }
  const updateMemberRole = async (member:Member, role:string) => {
    setMemberBusy(true); setError('')
    try { await api<void>(`/api/v1/workspace/members/${member.membershipId}/role`,{method:'PATCH',body:JSON.stringify({role})}); await refresh() }
    catch(e) { setError(e instanceof Error ? e.message : 'Could not change member role') }
    finally { setMemberBusy(false) }
  }
  const removeMember = async (member:Member) => {
    if(!window.confirm(`Remove ${member.email} from this workspace?`)) return
    setMemberBusy(true); setError('')
    try { await api<void>(`/api/v1/workspace/members/${member.membershipId}`,{method:'DELETE'}); await refresh() }
    catch(e) { setError(e instanceof Error ? e.message : 'Could not remove member') }
    finally { setMemberBusy(false) }
  }
  const revokeInvitation = async (invitation:Invitation) => {
    if(!window.confirm(`Revoke the invitation for ${invitation.email}?`)) return
    try { await api<void>(`/api/v1/workspace/invitations/${invitation.id}/revoke`,{method:'POST'}); await refresh() }
    catch(e) { setError(e instanceof Error ? e.message : 'Could not revoke invitation') }
  }

  const switchWorkspace = async (workspaceId: number) => {
    selectedWorkspaceId = workspaceId
    sessionStorage.setItem('aurora.workspace', String(workspaceId))
    setActiveWorkspaceId(workspaceId)
    setLoading(true)
    setTab('overview')
    await refresh()
  }

  useEffect(() => {
    if (!authenticated) return
    const heartbeat = () => { void api<void>('/api/v1/auth/activity', { method: 'POST' }).catch(() => undefined) }
    heartbeat()
    const timer = window.setInterval(heartbeat, 60_000)
    return () => window.clearInterval(timer)
  }, [authenticated])

  const refreshAdminAccess = useCallback(async () => {
    try { setAdminAccess(await api<AdminAccess>('/api/v1/admin/access')) }
    catch (e) { setError(e instanceof Error ? e.message : 'Unable to load access monitor') }
  }, [])

  const revokeAdminSession = async (sessionId: number) => {
    setAdminAccessBusy(sessionId)
    try { await api<void>('/api/v1/admin/access/sessions/' + sessionId + '/revoke', { method: 'POST' }); await refreshAdminAccess() }
    catch (e) { setError(e instanceof Error ? e.message : 'Unable to revoke session') }
    finally { setAdminAccessBusy(null) }
  }

  useEffect(() => {
    if (!authenticated || !platformAdmin || tab !== 'admin') return
    const timer = window.setInterval(() => { void refreshAdminAccess() }, 30_000)
    return () => window.clearInterval(timer)
  }, [authenticated, platformAdmin, tab, refreshAdminAccess])

  const canManageMembers = currentRole === 'OWNER' || currentRole === 'ADMIN'
  const canInviteAdmins = currentRole === 'OWNER'
  const canManageProjects = currentRole === 'OWNER' || currentRole === 'ADMIN' || currentRole === 'MEMBER'

  const title = ({ members: 'Members & invitations', overview: 'Overview', tasks: 'Task centre', approvals: 'Approval centre', activity: 'Activity log', businesses: 'Businesses', projects: 'Project factory', agents: 'AI employees', memory: 'Shared memory', research: 'Customer research', settings: 'Account settings', admin: 'Platform access monitor' } as Record<Tab, string>)[tab]
  const nav = (key: Tab, icon: string, label: string) => <button className={`nav-item ${tab === key ? 'active' : ''}`} onClick={() => setTab(key)}><span>{icon}</span>{label}{key === 'approvals' && stats.pendingApprovals > 0 && <b className="nav-count">{stats.pendingApprovals}</b>}</button>

  return (
    <div className="app-shell">
      {!authenticated && <div className="auth-overlay"><form className="auth-panel" onSubmit={resetToken ? completePasswordReset : invitationToken && inviteAccountMode === 'new' ? registerFromInvitation : resetRequestMode ? requestPasswordReset : submitAuth}>
        <div className="brand-mark">A</div>
        <div className="eyebrow">{resetToken ? 'ACCOUNT RECOVERY' : resetRequestMode ? 'ACCOUNT RECOVERY' : invitationToken ? 'WORKSPACE INVITATION' : authMode === 'register' ? 'CREATE YOUR AURORA ACCOUNT' : 'SECURE WORKSPACE ACCESS'}</div>
        <h2>{resetToken ? 'Choose a new password.' : resetRequestMode ? 'Reset your password.' : invitationToken ? (inviteAccountMode === 'new' ? 'Join the workspace.' : 'Sign in to join.') : authMode === 'register' ? 'Create your AURORA account.' : 'Welcome back.'}</h2>
        <p>{resetToken ? 'Choose a new password of at least 12 characters. This one-time reset link expires after 30 minutes.' : resetRequestMode ? 'Enter the email address associated with your account. If an active account exists, we will send a one-time reset link.' : invitationToken ? 'You have been invited to AURORA. Use the email address the invitation was sent to. Invitations are single-use and expire after seven days.' : authMode === 'register' ? 'Create your account and a private workspace. You will be its owner and can invite your own team. Other users cannot access your workspace unless you invite them.' : 'Sign in with your AURORA account to continue to your workspace.'}</p>
        {inviteNotice && <div className="notice-panel">{inviteNotice}</div>}
        {resetNotice && <div className="notice-panel">{resetNotice}</div>}
        {resetToken ? <>
          <label>New password<input type="password" value={resetNewPassword} onChange={e=>setResetNewPassword(e.target.value)} autoComplete="new-password" minLength={12} maxLength={72} required/><small>Use at least 12 characters.</small></label>
          <label>Confirm new password<input type="password" value={resetConfirmPassword} onChange={e=>setResetConfirmPassword(e.target.value)} autoComplete="new-password" minLength={12} maxLength={72} required/></label>
          <button className="primary-button" disabled={resetBusy}>{resetBusy ? 'Updating…' : 'Set new password'}</button>
        </> : resetRequestMode ? <>
          <label>Email address<input type="email" value={authEmail} onChange={e=>setAuthEmail(e.target.value)} autoComplete="email" maxLength={254} required/></label>
          <button className="primary-button" disabled={resetBusy}>{resetBusy ? 'Sending…' : 'Send reset link'}</button>
          <button type="button" className="text-button" onClick={()=>{setResetRequestMode(false);setResetNotice('');setError('')}}>Back to sign in</button>
        </> : invitationToken && inviteAccountMode === 'new' ? <>
          <label>Your name<input value={displayName} onChange={e=>setDisplayName(e.target.value)} autoComplete="name" minLength={2} maxLength={100} required/></label>
          <label>Create password<input type="password" value={authPassword} onChange={e=>setAuthPassword(e.target.value)} autoComplete="new-password" minLength={12} maxLength={72} required/><small>Use at least 12 characters.</small></label>
          <button className="primary-button" disabled={authBusy}>{authBusy ? 'Please wait…' : 'Create account & join'}</button>
          <button type="button" className="text-button" onClick={()=>{setInviteAccountMode('existing');setAuthPassword('');setError('')}}>Already have an account? Sign in</button>
        </> : <>
          {authMode === 'register' && !invitationToken && <>
            <label>Your name<input value={displayName} onChange={e=>setDisplayName(e.target.value)} autoComplete="name" minLength={2} maxLength={100} required/></label>
            <label>Workspace name<input value={workspaceName} onChange={e=>setWorkspaceName(e.target.value)} maxLength={140} required/></label>
          </>}
          <label>Email address<input type="email" value={authEmail} onChange={e=>setAuthEmail(e.target.value)} autoComplete="email" maxLength={254} required/></label>
          <label>Password<input type="password" value={authPassword} onChange={e=>setAuthPassword(e.target.value)} autoComplete={authMode === 'register' ? 'new-password' : 'current-password'} minLength={authMode === 'register' ? 12 : 1} maxLength={72} required/>{authMode === 'register' && <small>Use at least 12 characters.</small>}</label>
          <button className="primary-button" disabled={authBusy}>{authBusy ? 'Please wait…' : invitationToken ? 'Sign in & accept invitation' : authMode === 'register' ? 'Create account & workspace' : 'Sign in'}</button>
          {!invitationToken && authMode === 'login' && <button type="button" className="text-button" onClick={()=>{setResetRequestMode(true);setResetNotice('');setError('')}}>Forgot password?</button>}
          {invitationToken && <button type="button" className="text-button" onClick={()=>{setInviteAccountMode('new');setAuthPassword('');setError('')}}>Create a new account instead</button>}
          {!invitationToken && registrationOpen && <button type="button" className="text-button" onClick={()=>{setAuthMode(authMode==='register'?'login':'register');setError('')}}>{authMode==='register'?'Already have an account? Sign in':'New to AURORA? Create an account'}</button>}
        </>}
        {error && <small>{error}</small>}
      </form></div>}
      <aside className="sidebar">
        <div className="brand"><div className="brand-mark">A</div><div><strong>AURORA</strong><span>BUSINESS OS</span></div></div>
        <div className="nav-label">WORKSPACE</div>
        <nav>
          {nav('overview', '◫', 'Overview')}
          {nav('businesses', '▦', 'Businesses')}
          {nav('projects', '⌘', 'Projects')}
          {nav('agents', '✳', 'AI employees')}
          {nav('memory', '▤', 'Shared memory')}
          {nav('research', '⌕', 'Customer research')}
          {nav('tasks', '☷', 'Tasks')}
          {nav('approvals', '◇', 'Approvals')}
          {nav('activity', '↻', 'Activity log')}
          {nav('members', '♙', 'Members & invitations')}
          {nav('settings', '⚙', 'Account settings')}
          {platformAdmin && nav('admin', '◉', 'Access monitor')}
        </nav>
        <div className="sidebar-bottom"><div className="owner-avatar">AU</div><div><strong>{currentRole}</strong><span>Signed in</span></div><button className="text-button" onClick={()=>void logout()}>Sign out</button></div>
      </aside>

      <main className="main-content">
        <header className="topbar"><div className="breadcrumb">Workspace <span>/</span> {title}</div><div className="topbar-right">{workspaces.length > 1 && <select className="workspace-switcher" aria-label="Switch workspace" value={activeWorkspaceId ?? ''} onChange={e=>void switchWorkspace(Number(e.target.value))}>{workspaces.map(workspace=><option key={workspace.id} value={workspace.id}>{workspace.name} · {workspace.role}</option>)}</select>}<span className="environment">LOCAL DEVELOPMENT</span><button className="text-button" onClick={()=>void logout()}>Sign out ↗</button></div></header>
        <section className="page">
          <div className="page-heading"><div><div className="eyebrow">{dateLabel}</div><h1>{tab === 'overview' ? 'Good evening.' : title}</h1><p>{tab === 'overview' ? "Here's the current state of your business operating system." : ({ tasks: 'Create and track work across AURORA.', approvals: 'Important actions stay under your control.', activity: 'A traceable record of activity across the platform.', businesses: 'Business workspaces will live here.', projects: 'Create projects and launch coordinated agent workflows.', agents: 'Specialised AI workers will be managed here.', memory: 'A shared knowledge store used as context by all AI employees.', research: 'Capture customer interviews and distinguish evidence from assumptions.', settings: 'Manage your account security.', members: 'Manage workspace access and delegate administration.', admin: 'Global account activity, login sessions and access controls.' } as Record<Tab, string>)[tab]}</p></div><div className="system-pill"><span className={online ? 'pulse-dot' : 'pulse-dot offline'} />{online ? 'System operational' : 'Backend disconnected'}</div></div>

          {error && <div className="error-banner"><span>{error}</span><button onClick={() => void refresh()}>Retry</button></div>}

          {tab === 'overview' && <>
            <div className="welcome-panel"><div className="welcome-copy"><div className="eyebrow light">YOUR AUTONOMOUS BUSINESS PLATFORM</div><h2>Build something<br/><em>that works for you.</em></h2><p>AURORA is growing into a coordinated business operating system. Your work, decisions and system activity are now tracked in persistent storage.</p><div className="welcome-meta">◈ &nbsp; MILESTONE 02 · OPERATIONS CORE</div></div><div className="orbital-art" aria-hidden="true"><div className="orbit orbit-one"/><div className="orbit orbit-two"/><div className="orbit orbit-three"/><div className="orb-core">A</div><div className="orb-satellite satellite-one"/><div className="orb-satellite satellite-two"/></div></div>
            <div className="section-title"><div><h3>System overview</h3><p>Live operational data from the backend</p></div><span className="live-label">LIVE CHECKS</span></div>
            <div className="stats-grid">
              <article className="stat-card"><div className="stat-top">TOTAL TASKS <span className="stat-icon">☷</span></div><div className="stat-value">{stats.totalTasks}</div><div className="stat-foot">Work items recorded</div></article>
              <article className="stat-card"><div className="stat-top">IN PROGRESS <span className="stat-icon">↻</span></div><div className="stat-value">{stats.inProgressTasks}</div><div className="stat-foot">Currently being worked on</div></article>
              <article className="stat-card"><div className="stat-top">COMPLETED <span className="stat-icon">✓</span></div><div className="stat-value">{stats.completedTasks}</div><div className="stat-foot">Finished tasks</div></article>
              <article className="stat-card"><div className="stat-top">PENDING APPROVALS <span className="stat-icon">◇</span></div><div className="stat-value">{stats.pendingApprovals}</div><div className="stat-foot">Waiting for your decision</div></article>
            </div>
            <div className="lower-grid">
              <section className="content-card"><div className="card-heading"><div><h3>Operations snapshot</h3><p>Current workload</p></div><button className="text-button" onClick={() => setTab('tasks')}>View tasks →</button></div><div className="snapshot-row"><span>Pending tasks</span><strong>{stats.pendingTasks}</strong></div><div className="snapshot-row"><span>In progress</span><strong>{stats.inProgressTasks}</strong></div><div className="snapshot-row"><span>Completed</span><strong>{stats.completedTasks}</strong></div><div className="snapshot-row"><span>Waiting for approval</span><strong>{stats.pendingApprovals}</strong></div></section>
              <section className="content-card"><div className="card-heading"><div><h3>Recent activity</h3><p>Latest recorded events</p></div><button className="text-button" onClick={() => setTab('activity')}>View log →</button></div>{activity.slice(0,4).map(item => <div className="activity-item" key={item.id}><span className="activity-symbol success">✓</span><div><strong>{item.eventType.replaceAll('_',' ')}</strong><small>{item.details}</small></div><time>{new Date(item.createdAt).toLocaleDateString()}</time></div>)}{activity.length===0 && <p className="empty-note">No activity recorded yet. Create your first task to begin.</p>}</section>
            </div>
          </>}

          {tab === 'projects' && <section className="content-card module-card"><div className="module-toolbar"><div><h3>Project factory</h3><p>{projects.length} project{projects.length===1?'':'s'} · agent workflows are saved and progress refreshes automatically</p></div>{canManageProjects && <button className="primary-button" onClick={()=>setShowProjectForm(v=>!v)}>{showProjectForm?'Close form':'+ New project'}</button>}</div>
            {canManageProjects && showProjectForm && <form className="project-form" onSubmit={createProject}><label>Project name<input value={projectName} onChange={e=>setProjectName(e.target.value)} maxLength={160} placeholder="e.g. Build a coffee shop website" required/></label><label>Project goal<textarea value={projectGoal} onChange={e=>setProjectGoal(e.target.value)} maxLength={4000} rows={3} placeholder="Describe who it is for, the problem and what success looks like." required/></label><label>Project type<select value={projectType} onChange={e=>setProjectType(e.target.value)}><option value="WEBSITE">Website</option><option value="APPLICATION">Application</option><option value="MARKETING">Marketing</option><option value="GENERAL">General project</option></select></label><button className="primary-button" disabled={projectBusy}>{projectBusy?'Creating…':'Create project'}</button></form>}
            <div className="project-list">{projects.map(project=><article className="project-card" key={project.id}><div className="project-head"><div><span className="project-type">{project.projectType}</span><h3>{project.name}</h3><p>{project.goal}</p></div>{canManageProjects ? <select className={`status-select status-${project.status.toLowerCase()}`} value={project.status} onChange={e=>void updateProjectStatus(project,e.target.value)} aria-label={`Project status for ${project.name}`}><option value="PLANNING">Planning</option><option value="ACTIVE">Active</option><option value="PAUSED">Paused</option><option value="COMPLETED">Completed</option><option value="ARCHIVED">Archived</option></select> : <span className={`project-status status-${project.status.toLowerCase()}`}>{project.status}</span>}</div>{canManageProjects && <div className="project-workflow-controls"><button className="secondary-button" disabled={workspaceBusy===project.id} onClick={()=>void createProjectWorkspace(project.id)}>{workspaceBusy===project.id?'Working…':'▣ Create website starter'}</button><button className="primary-button" disabled={workspaceBusy===project.id} onClick={()=>void generateProjectWebsite(project)}>{workspaceBusy===project.id?'Generating…':'✦ Generate website with AI'}</button>{workspaceFiles[project.id]?.length>0&&<><button className="secondary-button" onClick={()=>void exportProjectWorkspace(project.id)}>↓ Export ZIP</button><button className="preview-build-button" disabled={workspaceBusy===project.id||previewBusyProject===project.id} onClick={()=>void buildProjectPreview(project.id)}>{previewBusyProject===project.id?'Building safely…':'▣ Build & preview'}</button><button className="secondary-button" disabled={qualityBusy===project.id} onClick={()=>void runProjectQualityReview(project.id,'qa-agent')}>AI source review</button><button className="preview-build-button" disabled={qualityBusy===project.id} onClick={()=>void runBrowserQA(project.id)}>{qualityBusy===project.id?'Testing in browser…':'◉ Browser QA'}</button><button className="secondary-button" disabled={qualityBusy===project.id} onClick={()=>void runProjectQualityReview(project.id,'security-agent')}>⌑ Security review</button></>}<label>Start agent team<select value={workflowTemplate[project.id]||'website-planning'} onChange={e=>setWorkflowTemplate(current=>({...current,[project.id]:e.target.value}))}><option value="website-planning">Website / app planning</option><option value="website-build">Research, build & preview website</option><option value="marketing-plan">Marketing plan</option></select></label><button className="primary-button" disabled={workflowBusyProject===project.id} onClick={()=>void startProjectWorkflow(project.id)}>{workflowBusyProject===project.id?'Queueing…':'▶ Start workflow'}</button></div>}
              {(previewBuilds[project.id]||previewUrls[project.id])&&<section className="project-preview-panel">
                <div className="preview-toolbar"><div><strong>Website preview</strong><small>{previewBuilds[project.id]?.status==='SUCCEEDED'?'Build successful · isolated container':'Build did not complete'}</small></div>{previewUrls[project.id]&&<div className="preview-actions"><button className={previewModes[project.id]!=='mobile'?'selected':''} onClick={()=>setPreviewModes(current=>({...current,[project.id]:'desktop'}))}>Desktop</button><button className={previewModes[project.id]==='mobile'?'selected':''} onClick={()=>setPreviewModes(current=>({...current,[project.id]:'mobile'}))}>Mobile</button><button onClick={()=>void refreshProjectPreviewAccess(project.id)}>Refresh link</button></div>}</div>
                {previewUrls[project.id]&&<div className={`preview-frame-wrap ${previewModes[project.id]==='mobile'?'mobile':''}`}><iframe key={previewUrls[project.id]} title={`${project.name} website preview`} src={apiUrl(previewUrls[project.id])} sandbox="allow-scripts" referrerPolicy="no-referrer"/></div>}
                {previewBuilds[project.id]?.logs&&<details className="preview-build-logs"><summary>Build output</summary><pre>{previewBuilds[project.id].logs}</pre></details>}
              </section>}
              <div className="project-review-panel">
                {browserQAResults[project.id]&&<article className="quality-report-panel"><div className="quality-report-head"><div><strong>Browser QA · {browserQAResults[project.id].status}</strong><small>Real Chromium run · Build and preview verified first · No source changes</small></div><button className="text-button" onClick={()=>setBrowserQAResults(current=>{const next={...current};delete next[project.id];return next})}>Dismiss</button></div><p>{browserQAResults[project.id].issues.length} finding(s) · {browserQAResults[project.id].checks.length} checks</p><pre>{JSON.stringify({checks:browserQAResults[project.id].checks,issues:browserQAResults[project.id].issues},null,2)}</pre>{browserQAResults[project.id].screenshots?.desktop?.data&&<div className="browser-qa-shots"><h4>Desktop screenshot</h4><img src={`data:image/jpeg;base64,${browserQAResults[project.id].screenshots.desktop?.data}`} alt="Desktop website QA screenshot"/>{browserQAResults[project.id].screenshots.mobile?.data&&<><h4>Mobile screenshot</h4><img src={`data:image/jpeg;base64,${browserQAResults[project.id].screenshots.mobile?.data}`} alt="Mobile website QA screenshot"/></>}</div>}</article>}
                {qualityResults[project.id]&&<article className="quality-report-panel"><div className="quality-report-head"><div><strong>{qualityResults[project.id].agentId==='security-agent'?'Security review':'QA review'} · Run #{qualityResults[project.id].agentRunId}</strong><small>Saved as a project report · Advisory only · No source changes</small></div><button className="text-button" onClick={()=>setQualityResults(current=>{const next={...current};delete next[project.id];return next})}>Dismiss</button></div><pre>{qualityResults[project.id].report}</pre><small>{qualityResults[project.id].notice}</small></article>}
                <div className="section-title"><div><h3>Review & approval</h3><p>Inspect saved files, request review, and keep a decision history. Approval never deploys or publishes.</p></div><span className="pending-chip">{(projectReviews[project.id]||[]).filter(r=>r.status==='PENDING_REVIEW').length} awaiting</span></div>
                {(workspaceFiles[project.id]||[]).map(file=>{
                  const editableSource=file.path==='src/App.tsx'||file.path==='src/style.css'||file.path.startsWith('src/components/')||file.path.startsWith('src/pages/')
                  const reviewsForFile = (projectReviews[project.id]||[]).filter(r=>r.artifactPath===file.path)
                  return <article className="review-file" key={file.path}><div className="review-file-head"><div><strong>{file.path}</strong><small>{file.sizeBytes.toLocaleString()} bytes · {reviewsForFile.length?reviewsForFile[0].status.replaceAll('_',' '):'Not submitted for review'}</small></div>{canManageProjects && <button className="secondary-button" disabled={reviewBusy!==null||!projectArtifacts[project.id]?.some(a=>a.relativePath===file.path)} onClick={()=>{const id=projectArtifacts[project.id]?.find(a=>a.relativePath===file.path)?.id;if(id)void requestProjectReview(project.id,id);else setError('Artifact metadata is not available yet. Refresh the project.')}}>Request review</button>}</div><details className="review-source"><summary>Open source editor · {file.path}</summary><div className="source-editor"><div className="source-editor-toolbar"><span>Current working copy · {file.sizeBytes.toLocaleString()} bytes</span><button className="primary-button" disabled={sourceBusy===project.id+":"+file.path||!canManageProjects||!editableSource} onClick={()=>void saveProjectSource(project.id,file)}>{!editableSource?'Read-only file':sourceBusy===project.id+":"+file.path?'Saving…':'Save new version'}</button></div><textarea className="source-code-editor" spellCheck={false} aria-label={'Edit '+file.path} value={sourceDrafts[project.id+":"+file.path]??file.content} onChange={e=>setSourceDrafts(current=>({...current,[project.id+":"+file.path]:e.target.value}))} disabled={!canManageProjects||!editableSource}/><div className="source-editor-toolbar"><button className="text-button" onClick={()=>{const artifact=projectArtifacts[project.id]?.find(a=>a.relativePath===file.path);if(artifact)void loadSourceVersions(project.id,artifact.id)}}>Load version history</button><small>{editableSource?'Saving creates an immutable snapshot. Build again to preview edits.':'This project file is inspectable but not editable through the source editor.'}</small></div>{(()=>{const artifact=projectArtifacts[project.id]?.find(a=>a.relativePath===file.path);const history=artifact?sourceVersions[artifact.id]||[]:[];const selected=artifact?sourceVersionContent[artifact.id]:undefined;return <>{history.length>0&&<div className="source-version-list"><strong>Saved versions</strong>{history.map(version=><div className="source-version-row" key={version.id}><span>v{version.versionNumber} · {new Date(version.createdAt).toLocaleString()} · {version.sha256.slice(0,12)}</span><button className="text-button" onClick={()=>artifact&&void inspectSourceVersion(project.id,artifact.id,version.id)}>Inspect</button></div>)}</div>}{selected&&<div className="source-version-preview"><div className="source-editor-toolbar"><strong>Compare version {selected.versionNumber} with working copy</strong><button className="secondary-button" onClick={()=>setSourceDrafts(current=>({...current,[project.id+":"+file.path]:selected.content}))}>Use saved version as editor draft</button></div><div className="source-compare-grid"><section><strong>Saved version · v{selected.versionNumber}</strong><pre>{selected.content}</pre></section><section><strong>Current working copy</strong><pre>{sourceDrafts[project.id+":"+file.path]??file.content}</pre></section></div></div>}</>})()}</div></details>{reviewsForFile.map(review=><div className="review-history" key={review.id}><div className="review-history-head"><strong>Review #{review.id} · v{review.artifactVersionNumber??'unknown'} · {review.status.replaceAll('_',' ')}</strong><small>Requested {new Date(review.createdAt).toLocaleString()} by {review.requestedBy}</small></div>{review.feedback&&<p>{review.feedback}</p>}{review.reviewedBy&&<small>Decision by {review.reviewedBy} · {review.reviewedAt?new Date(review.reviewedAt).toLocaleString():''}</small>}{review.status==='PENDING_REVIEW'&&(currentRole==='OWNER'||currentRole==='ADMIN')&&<div className="review-decision"><textarea value={reviewFeedback[review.id]||''} onChange={e=>setReviewFeedback(current=>({...current,[review.id]:e.target.value}))} rows={2} maxLength={4000} placeholder="Feedback for the agent (required for changes or rejection)"/><div className="approval-actions"><button className="reject-button" disabled={reviewBusy===review.id} onClick={()=>void decideProjectReview(project.id,review,'REJECTED')}>Reject</button><button className="secondary-button" disabled={reviewBusy===review.id} onClick={()=>void decideProjectReview(project.id,review,'CHANGES_REQUESTED')}>Request changes</button><button className="approve-button" disabled={reviewBusy===review.id} onClick={()=>void decideProjectReview(project.id,review,'APPROVED')}>Approve version</button></div></div>}</div>)}</article>
                })}
                {(workspaceFiles[project.id]||[]).length===0&&<p className="empty-note">Create a starter workspace or generate a website to review project files here.</p>}
                {(projectReviews[project.id]||[]).length>0&&<details className="review-history-all"><summary>Full review history ({projectReviews[project.id].length})</summary>{projectReviews[project.id].map(review=><p key={review.id}>#{review.id} · {review.artifactName} · {review.status.replaceAll('_',' ')} · {new Date(review.createdAt).toLocaleString()}{review.feedback?' · '+review.feedback:''}</p>)}</details>}
              </div>
              <section className="campaign-manager">
                <div className="section-title"><div><h3>Campaign studio</h3><p>Draft channel-specific content, submit it for owner review, then queue approved work. Aurora does not publish to social accounts yet.</p></div><span className="pending-chip">{(marketingCampaigns[project.id]||[]).length} campaigns</span></div>
                {canManageProjects && <form className="project-form" onSubmit={event=>void createCampaign(event,project.id)}>
                  <label>Campaign title<input maxLength={180} required value={campaignDraftFor(project.id).title} onChange={e=>updateCampaignDraft(project.id,{title:e.target.value})} placeholder="e.g. Meet the new lunch menu"/></label>
                  <div className="project-workflow-controls"><label>Channel<select value={campaignDraftFor(project.id).channel} onChange={e=>updateCampaignDraft(project.id,{channel:e.target.value})}><option value="INSTAGRAM">Instagram</option><option value="FACEBOOK">Facebook</option><option value="TIKTOK">TikTok</option><option value="YOUTUBE">YouTube</option><option value="LINKEDIN">LinkedIn</option><option value="X">X</option><option value="EMAIL">Email</option><option value="OTHER">Other</option></select></label><label>Format<select value={campaignDraftFor(project.id).format} onChange={e=>updateCampaignDraft(project.id,{format:e.target.value})}><option value="POST">Post</option><option value="STORY">Story</option><option value="REEL">Reel</option><option value="VIDEO">Video</option><option value="CAROUSEL">Carousel</option><option value="EMAIL">Email</option><option value="OTHER">Other</option></select></label></div>
                  <label>Creative brief for AI<textarea rows={2} maxLength={4000} value={campaignDraftFor(project.id).brief} onChange={e=>updateCampaignDraft(project.id,{brief:e.target.value})} placeholder="Who should this reach, what should they understand, and what action should they take?"/></label>
                  <button type="button" className="secondary-button" disabled={campaignGenerating===project.id} onClick={()=>void generateCampaignCopy(project)}>{campaignGenerating===project.id?'Generating…':'✦ Generate copy with Content Agent · uses AI budget'}</button>
                  <label>Creative copy<textarea rows={4} maxLength={20000} required value={campaignDraftFor(project.id).content} onChange={e=>updateCampaignDraft(project.id,{content:e.target.value})} placeholder="Review and edit the generated caption, email or script before saving."/></label>
                  <label>Call to action<input maxLength={500} value={campaignDraftFor(project.id).callToAction} onChange={e=>updateCampaignDraft(project.id,{callToAction:e.target.value})} placeholder="e.g. Visit us this weekend"/></label>
                  <button className="primary-button" disabled={campaignBusy===project.id}>{campaignBusy===project.id?'Saving…':campaignEditing[project.id]?'Update campaign draft':'Save campaign draft'}</button>
                </form>}
                {(marketingCampaigns[project.id]||[]).map(campaign=><article className="approval-row" key={campaign.id}>
                  <div className="approval-heading"><div><span className="action-type">{campaign.channel} · {campaign.format}</span><h3>{campaign.title}</h3></div><span className={`approval-status ${campaign.status.toLowerCase()}`}>{campaign.status.replaceAll('_',' ')}</span></div>
                  <p className="campaign-copy">{campaign.content}</p>{campaign.callToAction&&<p><strong>CTA:</strong> {campaign.callToAction}</p>}
                  {campaign.feedback&&<p className="decision-note">Review feedback: {campaign.feedback}</p>}
                  {(campaign.status==='DRAFT'||campaign.status==='CHANGES_REQUESTED')&&<div className="approval-actions"><button className="secondary-button" onClick={()=>editCampaignDraft(project.id,campaign)}>Edit draft</button><button className="secondary-button" disabled={campaignBusy===campaign.id} onClick={()=>void submitCampaign(project.id,campaign.id)}>Submit for review</button></div>}
                  {campaign.status==='PENDING_REVIEW'&&(currentRole==='OWNER'||currentRole==='ADMIN')&&<div className="review-decision"><textarea rows={2} maxLength={4000} value={campaignFeedback[campaign.id]||''} onChange={e=>setCampaignFeedback(current=>({...current,[campaign.id]:e.target.value}))} placeholder="Feedback (required for changes or rejection)"/><div className="approval-actions"><button className="reject-button" disabled={campaignBusy===campaign.id} onClick={()=>void decideCampaign(project.id,campaign.id,'REJECTED')}>Reject</button><button className="secondary-button" disabled={campaignBusy===campaign.id} onClick={()=>void decideCampaign(project.id,campaign.id,'CHANGES_REQUESTED')}>Request changes</button><button className="approve-button" disabled={campaignBusy===campaign.id} onClick={()=>void decideCampaign(project.id,campaign.id,'APPROVED')}>Approve</button></div></div>}
                  {campaign.status==='APPROVED'&&(currentRole==='OWNER'||currentRole==='ADMIN')&&<div className="project-workflow-controls"><label>Queue date & time<input type="datetime-local" value={campaignScheduleDrafts[campaign.id]||''} onChange={e=>setCampaignScheduleDrafts(current=>({...current,[campaign.id]:e.target.value}))}/></label><button className="primary-button" disabled={campaignBusy===campaign.id} onClick={()=>void scheduleCampaign(project.id,campaign.id)}>Queue in Aurora</button></div>}
                  {campaign.status==='SCHEDULED'&&<small>Queued for {campaign.scheduledAt?new Date(campaign.scheduledAt).toLocaleString():''}. This is an Aurora planning queue only; no social post has been sent.</small>}
                  <small>Created {new Date(campaign.createdAt).toLocaleString()} · {campaign.createdBy}</small>
                </article>)}
                {(marketingCampaigns[project.id]||[]).length===0&&<p className="empty-note">No campaigns yet. Start a Marketing plan workflow for ideas, then save approved copy here.</p>}
              </section>
              {(projectWorkflows[project.id]||[]).map(wf=><div className="workflow-box" key={wf.run.id}><div className="workflow-title"><div><strong>{wf.run.workflowType.replaceAll('-',' ')}</strong><small>Run #{wf.run.id} · {new Date(wf.run.createdAt).toLocaleString()}</small>{(()=>{const tokenTotal=wf.steps.reduce((sum,step)=>sum+(step.totalTokens??0),0);const costSteps=wf.steps.filter(step=>step.providerCostUsd!==null);const reportedCost=costSteps.reduce((sum,step)=>sum+Number(step.providerCostUsd),0);return <small>{tokenTotal.toLocaleString()} tokens · {costSteps.length?`$${reportedCost.toFixed(6)} provider-reported across ${costSteps.length} steps`:'Provider cost not reported'}</small>})()}</div><span className={`workflow-status ${wf.run.status.toLowerCase()}`}>{wf.run.status.replaceAll('_',' ')}</span></div><div className="workflow-progress"><div className="workflow-progress-fill" style={{width:`${wf.steps.length?wf.steps.filter(s=>s.status==='SUCCEEDED').length/wf.steps.length*100:0}%`}}/></div><div className="workflow-steps">{wf.steps.map(step=><div className="workflow-step" key={step.id}><span className={`step-indicator ${step.status.toLowerCase()}`}>{step.status==='SUCCEEDED'?'✓':step.status==='RUNNING'?'↻':step.status==='FAILED'?'!':'·'}</span><div><strong>{step.stepOrder}. {step.title}</strong><small>{step.agentId} · {step.status}{step.dependsOnStepOrders?.length?` · waits for stage ${step.dependsOnStepOrders.join(', ')}`:' · no dependencies'}{step.approvalRequired?` · approval ${step.approvalStatus||'pending'}`:''}{step.agentRunId?` · Agent run #${step.agentRunId}`:''}{step.previousAgentRunId?` · Previous run #${step.previousAgentRunId}`:''}{step.retryCount?` · retry ${step.retryCount}/1`:''}</small>{step.usageStatus&&<small className="workflow-usage">{step.totalTokens!==null?`${step.totalTokens.toLocaleString()} tokens`: 'Token usage unavailable'} · {step.providerCostUsd!==null?`$${Number(step.providerCostUsd).toFixed(6)} provider-reported`:step.usageStatus==='UNAVAILABLE'?'Provider did not report cost':'Cost not reported'}{step.usageStatus==='UNKNOWN'?' · usage outcome unknown':''}</small>}{step.output && <details><summary>View agent result</summary><pre>{step.output}</pre></details>}{step.status==='AWAITING_APPROVAL'&&<small className="approval-pending-note">Website source generation is paused until an owner or administrator approves this stage. No source-generation request has been sent yet.</small>}{step.status==='AWAITING_APPROVAL'&&(currentRole==='OWNER'||currentRole==='ADMIN')&&<button className="approve-button workflow-approve-button" onClick={()=>void approveWorkflowStep(project.id,wf.run.id,step.id)}>Approve source generation</button>}{step.error&&<small className="error-note">{step.error}</small>}{step.status==='FAILED'&&step.retryCount===0&&(currentRole==='OWNER'||currentRole==='ADMIN')&&<button className="secondary-button workflow-retry-button" onClick={()=>void retryWorkflowStep(project.id,wf.run.id,step.id)}>Retry failed stage · may incur cost</button>}</div></div>)}</div>{wf.run.error&&<p className="error-note">{wf.run.error}</p>}{canManageProjects && <div className="workflow-controls">{['QUEUED','RUNNING'].includes(wf.run.status)&&<button className="secondary-button" onClick={()=>void controlProjectWorkflow(project.id,wf.run.id,'pause')}>Pause at next safe checkpoint</button>}{wf.run.status==='PAUSE_REQUESTED'&&<small>Pause requested · current stage will finish first</small>}{wf.run.status==='PAUSED'&&<button className="primary-button" onClick={()=>void controlProjectWorkflow(project.id,wf.run.id,'resume')}>Resume from checkpoint</button>}{['QUEUED','RUNNING','PAUSE_REQUESTED','PAUSED','AWAITING_APPROVAL','CANCEL_REQUESTED'].includes(wf.run.status)&&<button className="text-button cancel-workflow" onClick={()=>void cancelProjectWorkflow(project.id,wf.run.id)}>{wf.run.status==='CANCEL_REQUESTED'?'Cancellation requested':'Cancel workflow'}</button>}</div>}</div>)}
              {(projectWorkflows[project.id]||[]).length===0&&<p className="empty-note project-hint">No agent workflow has run for this project yet. Starting one uses your configured AI provider and counts toward usage.</p>}</article>)}{projects.length===0&&<div className="empty-state"><span>⌘</span><h3>Start your first project</h3><p>Give Aurora a goal. It will research, develop a strategy, draft messaging and make a project plan using its current agent team.</p>{canManageProjects && <button className="primary-button" onClick={()=>setShowProjectForm(true)}>Create a project</button>}</div>}</div>
          </section>}

          {tab === 'tasks' && <section className="content-card module-card"><div className="module-toolbar"><div><h3>Work queue</h3><p>{tasks.length} task{tasks.length===1?'':'s'} recorded</p></div><button className="primary-button" onClick={() => setShowTaskForm(v=>!v)}>{showTaskForm ? 'Close form' : '+ New task'}</button></div>
            {showTaskForm && <form className="task-form" onSubmit={createTask}><label>Task title<input value={taskTitle} onChange={e=>setTaskTitle(e.target.value)} maxLength={160} placeholder="e.g. Research online business opportunities" required/></label><label>Description<textarea value={taskDescription} onChange={e=>setTaskDescription(e.target.value)} maxLength={2000} rows={3} placeholder="What needs to be done?"/></label><label>Priority<select value={taskPriority} onChange={e=>setTaskPriority(e.target.value)}><option value="LOW">Low</option><option value="NORMAL">Normal</option><option value="HIGH">High</option><option value="URGENT">Urgent</option></select></label><button className="primary-button" disabled={busy}>{busy?'Saving…':'Create task'}</button></form>}
            <div className="task-list">{tasks.map(task=><article className="task-row" key={task.id}><div className="task-main"><div className="task-title">{task.title}</div>{task.description && <p>{task.description}</p>}<div className="task-meta"><span className={`priority priority-${task.priority.toLowerCase()}`}>{task.priority}</span><span>Created {new Date(task.createdAt).toLocaleDateString()}</span></div></div><select className={`status-select status-${task.status.toLowerCase()}`} value={task.status} onChange={e=>void changeTaskStatus(task,e.target.value)} aria-label={`Status for ${task.title}`}><option value="PENDING">Pending</option><option value="IN_PROGRESS">In progress</option><option value="COMPLETED">Completed</option><option value="CANCELLED">Cancelled</option></select></article>)}{tasks.length===0 && <div className="empty-state"><span>☷</span><h3>Your task queue is empty</h3><p>Create tasks for yourself or work that AURORA agents will handle in future.</p><button className="primary-button" onClick={()=>setShowTaskForm(true)}>Create first task</button></div>}</div>
          </section>}

          {tab === 'approvals' && <section className="content-card module-card"><div className="module-toolbar"><div><h3>Owner decisions</h3><p>Requests that require your explicit approval</p></div><span className="pending-chip">{stats.pendingApprovals} pending</span></div><div className="approval-list">{approvals.map(item=><article className="approval-row" key={item.id}><div className="approval-heading"><div><span className="action-type">{item.actionType.replaceAll('_',' ')}</span><h3>{item.title}</h3></div><span className={`approval-status ${item.status.toLowerCase()}`}>{item.status}</span></div><p>{item.description}</p><div className="approval-footer"><small>Requested {new Date(item.createdAt).toLocaleString()}</small>{item.status==='PENDING' && <div className="approval-actions"><button className="reject-button" onClick={()=>void decideApproval(item,'reject')}>Reject</button><button className="approve-button" onClick={()=>void decideApproval(item,'approve')}>Approve</button></div>}</div>{item.decisionNote && <small className="decision-note">Decision note: {item.decisionNote}</small>}</article>)}{approvals.length===0 && <div className="empty-state"><span>◇</span><h3>No approval requests</h3><p>When AURORA proposes an important action, it will appear here for your decision.</p></div>}</div></section>}

          {tab === 'activity' && <section className="content-card module-card"><div className="module-toolbar"><div><h3>Audit trail</h3><p>Recent system events, newest first</p></div><button className="text-button" onClick={()=>void refresh()}>Refresh ↻</button></div><div className="audit-list">{activity.map(item=><div className="audit-row" key={item.id}><div className="audit-marker"/><div><strong>{item.eventType.replaceAll('_',' ')}</strong><p>{item.details}</p><small>{item.entityType} #{item.entityId} · {new Date(item.createdAt).toLocaleString()}</small></div></div>)}{activity.length===0 && <div className="empty-state"><h3>No events yet</h3><p>Actions performed in AURORA will be recorded here.</p></div>}</div></section>}

          {tab==='businesses' && <section className="content-card module-card"><div className="module-toolbar"><div><h3>Business portfolio</h3><p>{businesses.length} workspace{businesses.length===1?'':'s'} · each business keeps its own goals and status</p></div><button className="primary-button" onClick={()=>setShowBusinessForm(v=>!v)}>{showBusinessForm?'Close form':'+ New business'}</button></div>{showBusinessForm && <form className="business-form" onSubmit={createBusiness}><label>Business name<input value={businessName} onChange={e=>setBusinessName(e.target.value)} maxLength={140} placeholder="e.g. Digital products studio" required/></label><label>Description<textarea value={businessDescription} onChange={e=>setBusinessDescription(e.target.value)} maxLength={2500} rows={2} placeholder="What will this business do?"/></label><label>Primary goal<input value={businessGoal} onChange={e=>setBusinessGoal(e.target.value)} maxLength={1000} placeholder="What outcome should it achieve?"/></label><button className="primary-button" disabled={busy}>{busy?'Saving…':'Create workspace'}</button></form>}<div className="business-list">{businesses.map(b=><article className="business-row" key={b.id}><div><h3>{b.name}</h3><p>{b.description||'No description added yet.'}</p>{b.goal&&<small>GOAL · {b.goal}</small>}{currentRole==='OWNER'||currentRole==='ADMIN' ? <div className="business-budget-control"><label>Monthly AI ceiling (USD)<input type="number" min="0.0000000001" max="99999999" step="0.01" disabled={!businessBudgets[b.id]?.reservationConfigured} value={budgetDrafts[b.id] ?? ''} onChange={e=>setBudgetDrafts(current=>({...current,[b.id]:e.target.value}))} placeholder="e.g. 5.00"/></label><button className="text-button" disabled={budgetBusy===b.id||!businessBudgets[b.id]?.reservationConfigured} onClick={()=>void saveBusinessBudget(b.id)}>{budgetBusy===b.id?'Saving…':'Save budget'}</button>{businessBudgets[b.id]?.enabled && <button className="text-button" disabled={budgetBusy===b.id} onClick={()=>void removeBusinessBudget(b.id)}>Remove cap</button>}{businessBudgets[b.id]?.enabled && <small>Reported: ${businessBudgets[b.id].reportedSpendUsd.toFixed(6)} · Reserved: ${businessBudgets[b.id].heldUsd.toFixed(6)} · Available: ${businessBudgets[b.id].availableUsd.toFixed(6)} · {businessBudgets[b.id].alertLevel}</small>}<small>{!businessBudgets[b.id]?.reservationConfigured ? 'Business budgets are locked until the server has a defensible AURORA_AI_MAX_REQUEST_RESERVATION_USD value configured.' : businessBudgets[b.id]?.enabled ? 'Budget guard active. Requests with unknown provider cost keep their reservation held.' : 'No business spending ceiling is active yet. Platform ceiling, if configured, still applies.'}</small></div> : businessBudgets[b.id]?.enabled && <small>Monthly AI ceiling: ${businessBudgets[b.id].monthlyLimitUsd.toFixed(2)} USD</small>}</div><select className="status-select" value={b.status} onChange={e=>void changeBusinessStatus(b,e.target.value)} aria-label={`Status for ${b.name}`}><option value="IDEA">Idea</option><option value="RESEARCH">Research</option><option value="VALIDATION">Validation</option><option value="ACTIVE">Active</option><option value="PAUSED">Paused</option><option value="ARCHIVED">Archived</option></select></article>)}{businesses.length===0&&<div className="empty-state"><span>▦</span><h3>No businesses registered</h3><p>Create a workspace when AURORA identifies an opportunity worth investigating.</p><button className="primary-button" onClick={()=>setShowBusinessForm(true)}>Create first workspace</button></div>}</div>{(currentRole==='OWNER'||currentRole==='ADMIN')&&<><div className="budget-reconciliation"><div className="section-title"><div><h3>Budget alerts</h3><p>Persistent monthly thresholds · 80% warning · 90% critical · 100% exhausted</p></div><button className="text-button" onClick={()=>void refresh()}>Refresh ↻</button></div>{budgetAlerts.map(alert=><article className="approval-row" key={alert.id}><div className="approval-heading"><div><strong>{alert.businessName} · {alert.alertLevel}</strong><p>{alert.periodStart} UTC budget period</p></div><span className={`role-chip ${alert.acknowledgedAt?'role-active':'role-pending'}`}>{alert.acknowledgedAt?'ACKNOWLEDGED':'NEEDS REVIEW'}</span></div><p>${alert.observedUsd.toFixed(6)} of ${alert.monthlyLimitUsd.toFixed(6)} USD · {Math.min(999, alert.observedUsd / alert.monthlyLimitUsd * 100).toFixed(1)}% observed (reported cost plus held reservations)</p><small>Created {new Date(alert.createdAt).toLocaleString()}</small>{!alert.acknowledgedAt&&<div><button className="text-button" disabled={alertBusy===alert.id} onClick={()=>void acknowledgeBudgetAlert(alert)}>{alertBusy===alert.id?'Saving…':'Acknowledge'}</button></div>}</article>)}{budgetAlerts.length===0&&<p className="empty-note">No budget threshold alerts recorded for this workspace.</p>}</div><div className="budget-reconciliation"><div className="section-title"><div><h3>Unresolved AI charges</h3><p>{budgetReconciliations.length} held reservation{budgetReconciliations.length===1?'':'s'} · oldest first · maximum 100 shown</p></div><button className="text-button" onClick={()=>void refresh()}>Refresh ↻</button></div><div className="agent-notice"><strong>Verify before reconciling</strong><p>Check the provider dashboard or invoice and match the request to its billing record. Enter the actual provider-reported USD cost, a billing reference and a short explanation. Never enter an estimate. Reconciliation releases the held reservation and records an auditable adjustment; it does not contact the provider.</p></div>{budgetReconciliations.map(item=><article className="approval-row" key={item.id}><div className="approval-heading"><div><strong>{item.agentName} · Run #{item.agentRunId}</strong><p>{item.businessName||'Workspace-level work'} · {item.model}</p></div><span className="role-chip role-pending">HELD</span></div><p>Reserved ${item.reservedUsd.toFixed(6)} USD · {new Date(item.createdAt).toLocaleString()} · Usage status: {item.usageStatus||'not recorded'}</p><p>{item.providerRequestId ? <>Provider request ID: <code>{item.providerRequestId}</code></> : 'Provider request ID was not returned; match using the run time, model and provider billing records.'}</p><div className="reconciliation-form"><label>Actual provider cost (USD)<input type="number" min="0" max="99999999" step="0.0000000001" value={reconciliationDrafts[item.id]?.cost??''} onChange={e=>setReconciliationDrafts(current=>({...current,[item.id]:{cost:e.target.value,reference:current[item.id]?.reference??'',note:current[item.id]?.note??''}}))} placeholder="Confirmed invoice amount"/></label><label>Provider billing reference<input maxLength={160} value={reconciliationDrafts[item.id]?.reference??''} onChange={e=>setReconciliationDrafts(current=>({...current,[item.id]:{cost:current[item.id]?.cost??'',reference:e.target.value,note:current[item.id]?.note??''}}))} placeholder="Invoice or provider request reference"/></label><label>Evidence note<textarea maxLength={1000} minLength={10} rows={2} value={reconciliationDrafts[item.id]?.note??''} onChange={e=>setReconciliationDrafts(current=>({...current,[item.id]:{cost:current[item.id]?.cost??'',reference:current[item.id]?.reference??'',note:e.target.value}}))} placeholder="How did you match this request to the provider billing record?"/></label><button className="primary-button" disabled={reconciliationBusy===item.id} onClick={()=>void reconcileBudgetReservation(item)}>{reconciliationBusy===item.id?'Reconciling…':'Confirm reconciliation'}</button></div></article>)}{budgetReconciliations.length===0&&<p className="empty-note">No unresolved reservations in this workspace.</p>}</div></>}</section>}

          {tab==='research' && <section className="content-card module-card"><div className="module-toolbar"><div><h3>Customer evidence</h3><p>{customerInsights.length} research record{customerInsights.length===1?'':'s'} saved · newest 200 shown</p></div><span className="building-label">AGENT CONTEXT ENABLED</span></div><div className="agent-notice"><strong>Capture what people actually say and do</strong><p>Use anonymous labels such as “Student treasurer 01” rather than names, phone numbers or sensitive personal information. These notes are user-entered and are not independently verified. All agents receive the latest 50 records and must distinguish reported claims from observed behaviour.</p></div>
<div className="content-card"><div className="card-heading"><div><h3>Interview guide · Student savings groups</h3><p>Ask open questions. Do not pitch the app or lead participants toward an answer.</p></div><span className="pending-chip">5–10 minutes</span></div>
<ol className="interview-guide">
<li>Tell me about the last time your group collected contributions. How did it work?</li>
<li>How do you record who has paid and who has not?</li>
<li>Can you walk me through the last time a contribution record was confusing or disputed?</li>
<li>What do you currently use to coordinate reminders and share records?</li>
<li>What takes the most time or causes the most frustration?</li>
<li>How do you handle mistakes, late payments or disagreements?</li>
<li>Have you tried a tool or process to solve this? What happened?</li>
<li>If you could change one thing about the process, what would it be and why?</li>
<li>Who decides whether the group adopts a new tool, and what concerns would they have?</li>
<li>What would make you stop using a tool like this?</li>
</ol><p className="empty-note">Optional follow-up: “Can you give me a recent example?” Avoid asking “Would you use our app?” Hypothetical enthusiasm is weaker evidence than actual behaviour.</p></div>
<div className="module-toolbar"><div><h3>Evidence analysis</h3><p>Ask the Research Agent to review saved records. It will not change them or automatically prove demand.</p></div><div className="research-actions"><button className="text-button" disabled={customerInsights.length===0} onClick={exportCustomerInsights}>Export CSV</button><button className="primary-button" disabled={analysisBusy||customerInsights.length===0} onClick={()=>void analyseCustomerEvidence()}>{analysisBusy?'Analysing…':'Analyse collected evidence'}</button></div></div>
{analysisResult&&<article className="research-report"><div className="report-heading"><strong>Research Agent · {analysisResult.status}</strong><small>{new Date(analysisResult.startedAt).toLocaleString()}</small></div><pre>{analysisResult.output||analysisResult.error||'No output returned.'}</pre></article>}
<form className="research-form" onSubmit={saveInsight}><label>Record type<select value={insightType} onChange={e=>setInsightType(e.target.value)}><option value="PROBLEM">Problem mentioned</option><option value="CURRENT_SOLUTION">Current workaround</option><option value="OBJECTION">Objection or concern</option><option value="FEATURE_REQUEST">Feature request</option><option value="OTHER">Other observation</option></select></label><label>Anonymous participant label<input value={participantLabel} onChange={e=>setParticipantLabel(e.target.value)} maxLength={100} placeholder="e.g. Student treasurer 01" required/></label><label>What problem or observation did they describe?<textarea value={insightProblem} onChange={e=>setInsightProblem(e.target.value)} maxLength={2000} rows={3} placeholder="Use their words where possible. Avoid adding your interpretation as fact." required/></label><label>How do they solve it today?<textarea value={currentSolution} onChange={e=>setCurrentSolution(e.target.value)} maxLength={1500} rows={2} placeholder="Existing tools, paper records, WhatsApp, manual processes…"/></label><label>How often does it happen?<input value={insightFrequency} onChange={e=>setInsightFrequency(e.target.value)} maxLength={1000} placeholder="e.g. Every weekly contribution cycle"/></label><label>What did they say about trying or paying for a solution?<textarea value={insightWillingness} onChange={e=>setInsightWillingness(e.target.value)} maxLength={1000} rows={2} placeholder="Record their actual response; don't infer willingness from politeness."/></label><label>Evidence or interview context<textarea value={insightEvidence} onChange={e=>setInsightEvidence(e.target.value)} maxLength={2000} rows={2} placeholder="Date, method, direct quote or observed behaviour (no identifying data)."/></label><label>Evidence status<select value={validationStatus} onChange={e=>setValidationStatus(e.target.value)}><option value="UNVERIFIED">Unverified note</option><option value="REPORTED">Reported by participant</option><option value="OBSERVED">Directly observed</option></select></label><button className="primary-button" disabled={insightBusy}>{insightBusy?'Saving…':'Save research record'}</button></form><div className="section-title"><div><h3>Collected evidence</h3><p>Review records for patterns, contradictions and sample limitations.</p></div></div><div className="audit-list">{customerInsights.map(item=><article className="approval-row" key={item.id}><div className="approval-heading"><div><span className="action-type">{item.validationStatus} · {item.insightType.replaceAll('_',' ')}</span><h3>{item.participantLabel}</h3></div><button className="reject-button" onClick={()=>void deleteInsight(item.id)}>Delete</button></div><p>{item.problem}</p>{item.currentSolution&&<p><strong>Current workaround:</strong> {item.currentSolution}</p>}{item.frequency&&<p><strong>Frequency:</strong> {item.frequency}</p>}{item.willingness&&<p><strong>Willingness:</strong> {item.willingness}</p>}{item.evidence&&<p><strong>Evidence:</strong> {item.evidence}</p>}<div className="approval-footer"><small>Saved {new Date(item.createdAt).toLocaleString()}</small></div></article>)}{customerInsights.length===0&&<div className="empty-state"><h3>No customer evidence yet</h3><p>After speaking to a student savings group, record what you heard and observed here. AURORA can then analyse the evidence.</p></div>}</div></section>}

          {tab==='memory' && <section className="content-card module-card"><div className="module-toolbar"><div><h3>Shared knowledge</h3><p>{memories.length} saved item{memories.length===1?'':'s'} · newest 100 shown</p></div><span className="building-label">AGENT CONTEXT ENABLED</span></div><div className="agent-notice"><strong>How shared memory works</strong><p>Every agent receives up to 30 of the newest saved memories as background context when it runs. Add verified findings, decisions and feedback here. Memories are not automatically fact-checked or learned by the model; review and correct them when needed.</p></div><form className="research-form" onSubmit={saveMemory}><label>Category<select value={memoryCategory} onChange={e=>setMemoryCategory(e.target.value)}><option value="FINDING">Finding</option><option value="DECISION">Decision</option><option value="CUSTOMER_FEEDBACK">Customer feedback</option><option value="PREFERENCE">Preference</option><option value="RISK">Risk</option><option value="GENERAL">General</option></select></label><label>Title<input value={memoryTitle} onChange={e=>setMemoryTitle(e.target.value)} maxLength={180} placeholder="e.g. Students prefer weekly contribution reminders" required/></label><label>Details<textarea value={memoryContent} onChange={e=>setMemoryContent(e.target.value)} maxLength={3000} rows={4} placeholder="Record the evidence, context or decision. Distinguish what was observed from what is assumed." required/></label><label>Source (optional)<input value={memorySource} onChange={e=>setMemorySource(e.target.value)} maxLength={180} placeholder="e.g. Interview with 3 student group treasurers"/></label><button className="primary-button" disabled={memoryBusy}>{memoryBusy?'Saving…':'Save shared memory'}</button></form><div className="section-title"><div><h3>Saved memories</h3><p>Review these regularly so agents are not guided by outdated assumptions.</p></div></div><div className="audit-list">{memories.map(m=><article className="approval-row" key={m.id}><div className="approval-heading"><div><span className="action-type">{m.category.replaceAll('_',' ')}</span><h3>{m.title}</h3></div><button className="reject-button" onClick={()=>void deleteMemory(m.id)}>Delete</button></div><p>{m.content}</p><div className="approval-footer"><small>{m.source ? 'Source · ' + m.source + ' · ' : ''}Saved {new Date(m.createdAt).toLocaleString()}</small></div></article>)}{memories.length===0&&<div className="empty-state"><h3>No shared memories yet</h3><p>Save a confirmed finding or important decision. It will be available to all agents on their next run.</p></div>}</div></section>}

          {tab==='members' && <section className="content-card module-card">
            <div className="module-toolbar"><div><h3>Workspace members</h3><p>{members.length} member{members.length===1?'':'s'} · roles are enforced by the backend</p></div><button className="text-button" onClick={()=>void refresh()}>Refresh ↻</button></div>
            <div className="agent-notice"><strong>Delegated administration</strong><p>Owners can appoint administrators. Administrators can invite and manage ordinary members and viewers. Only owners can appoint or remove administrators and manage owner roles.</p></div>
            {canManageMembers && <form className="invite-form" onSubmit={createInvitation}><label>Email address<input type="email" value={inviteEmail} onChange={e=>setInviteEmail(e.target.value)} maxLength={254} placeholder="person@example.com" required/></label><label>Workspace role<select value={inviteRole} onChange={e=>setInviteRole(e.target.value)}><option value="MEMBER">Member</option><option value="VIEWER">Viewer</option>{canInviteAdmins && <option value="ADMIN">Administrator</option>}</select></label><button className="primary-button" disabled={inviteBusy}>{inviteBusy?'Creating…':'Create invitation'}</button></form>}
            {createdInvite && <div className="invite-created"><strong>Invitation created for {createdInvite.email}</strong><p>{createdInvite.emailSent ? 'Invitation email sent successfully. The raw invitation token is not exposed in the dashboard.' : 'Email was not sent. You can still share the one-time invitation link manually.'} It expires {new Date(createdInvite.expiresAt).toLocaleString()}.</p>{createdInvite.token && <><code>{createdInvite.token}</code><button className="text-button" onClick={()=>{void navigator.clipboard.writeText(createdInvite.token!)}}>Copy invitation token</button><button className="text-button" onClick={()=>{const link = `${window.location.origin}${window.location.pathname}?invite=${encodeURIComponent(createdInvite.token!)}`; void navigator.clipboard.writeText(link)}}>Copy shareable invitation link ↗</button></>}<small>The recipient must use {createdInvite.email}. Invitation links expire after seven days.</small></div>}
            <div className="section-title"><div><h3>People with access</h3><p>Changes take effect immediately.</p></div></div>
            <div className="member-list">{members.map(member=><article className="member-row" key={member.membershipId}><div className="member-avatar">{(member.displayName||member.email).slice(0,1).toUpperCase()}</div><div className="member-identity"><strong>{member.displayName}</strong><small>{member.email}</small></div><span className={`role-chip role-${member.role.toLowerCase()}`}>{member.role}</span>{canManageMembers && <><select aria-label={`Change role for ${member.email}`} value={member.role} disabled={memberBusy || (currentRole==='ADMIN' && (member.role==='OWNER'||member.role==='ADMIN')) || (currentRole==='OWNER' && member.role==='OWNER')} onChange={e=>void updateMemberRole(member,e.target.value)}>{currentRole==='OWNER' && <option value="ADMIN">Administrator</option>}<option value="MEMBER">Member</option><option value="VIEWER">Viewer</option></select><button className="reject-button" disabled={memberBusy || (currentRole==='ADMIN' && (member.role==='OWNER'||member.role==='ADMIN')) || (currentRole==='OWNER' && member.role==='OWNER')} onClick={()=>void removeMember(member)}>Remove</button></>}</article>)}{members.length===0&&<div className="empty-state"><h3>No members found</h3><p>Workspace members will appear here.</p></div>}</div>
            <div className="section-title"><div><h3>Invitations</h3><p>Pending and previous invitations for this workspace.</p></div></div>
            <div className="member-list">{invitations.map(invitation=><article className="member-row" key={invitation.id}><div className="member-identity"><strong>{invitation.email}</strong><small>{invitation.role} · Expires {new Date(invitation.expiresAt).toLocaleDateString()}</small></div><span className={`role-chip role-${invitation.status.toLowerCase()}`}>{invitation.status}</span>{invitation.status==='PENDING'&&<button className="reject-button" onClick={()=>void revokeInvitation(invitation)}>Revoke</button>}</article>)}{invitations.length===0&&<p className="empty-note">No invitations have been created yet.</p>}</div>
          </section>}

          {tab==='admin' && platformAdmin && <section className="content-card module-card"><div className="module-toolbar"><div><h3>Platform access monitor</h3><p>Global account and session visibility · auto-refreshes every 30 seconds</p></div><button className="text-button" onClick={()=>void refreshAdminAccess()}>Refresh ↻</button></div>{adminAccess ? <><div className="stats-grid"><article className="stat-card"><div className="stat-top">ONLINE USERS <span className="stat-icon">●</span></div><div className="stat-value">{adminAccess.onlineUsers}</div><div className="stat-foot">Active in the last {adminAccess.onlineWindowMinutes} minutes</div></article><article className="stat-card"><div className="stat-top">REGISTERED ACCOUNTS <span className="stat-icon">♙</span></div><div className="stat-value">{adminAccess.registeredAccounts}</div><div className="stat-foot">{adminAccess.activeAccounts} active · {adminAccess.disabledAccounts} disabled</div></article><article className="stat-card"><div className="stat-top">ACTIVE SESSIONS <span className="stat-icon">↗</span></div><div className="stat-value">{adminAccess.activeSessions}</div><div className="stat-foot">Valid sessions, including idle sessions</div></article><article className="stat-card"><div className="stat-top">SESSIONS STARTED · 30 DAYS <span className="stat-icon">◷</span></div><div className="stat-value">{adminAccess.sessionsStartedLast30Days}</div><div className="stat-foot">{adminAccess.accountsEverLoggedIn} unique accounts have logged in</div></article><article className="stat-card"><div className="stat-top">PENDING INVITATIONS <span className="stat-icon">✉</span></div><div className="stat-value">{adminAccess.pendingInvitations}</div><div className="stat-foot">Invitations not yet accepted or expired</div></article></div><div className="agent-notice"><strong>How these figures work</strong><p>Online means at least one authenticated browser session sent a heartbeat within the last {adminAccess.onlineWindowMinutes} minutes. A person with multiple devices counts once under online users, but each device/browser counts as a separate session. Anonymous landing-page visitors are not included in these account metrics.</p><small>Snapshot generated {new Date(adminAccess.generatedAt).toLocaleString()}</small></div><div className="section-title"><div><h3>Current login sessions</h3><p>Most recently active first · maximum 100 shown · revoke access immediately</p></div></div><div className="member-list">{adminAccess.sessions.map(session=><article className="member-row" key={session.id}><div className="member-avatar">{(session.displayName||session.email).slice(0,1).toUpperCase()}</div><div className="member-identity"><strong>{session.displayName || session.email}</strong><small>{session.email} · Started {new Date(session.createdAt).toLocaleString()} · Last active {new Date(session.lastSeenAt).toLocaleString()}</small></div><span className={'role-chip ' + (session.online?'role-active':'role-pending')}>{session.online?'ONLINE':'IDLE'}</span><button className="reject-button" disabled={adminAccessBusy===session.id} onClick={()=>void revokeAdminSession(session.id)}>{adminAccessBusy===session.id?'Revoking…':'Revoke session'}</button></article>)}{adminAccess.sessions.length===0&&<p className="empty-note">No active sessions.</p>}</div></> : <div className="empty-state"><h3>Access data unavailable</h3><p>Refresh to retry. Only the platform owner can view these metrics.</p></div>}</section>}

          {tab==='settings' && <section className="content-card module-card"><div className="module-toolbar"><div><h3>Account security</h3><p>Change the password used to sign in to AURORA.</p></div><span className="building-label">PASSWORD PROTECTED</span></div><div className="agent-notice"><strong>Keep your account secure</strong><p>Use a unique password of at least 12 characters. This device stays signed in; other active sessions are revoked.</p></div>{passwordNotice && <div className="success-banner">{passwordNotice}</div>}<form className="research-form" onSubmit={changePassword}><label>Current password<input type="password" value={currentPassword} onChange={e=>setCurrentPassword(e.target.value)} autoComplete="current-password" maxLength={72} required/></label><label>New password<input type="password" value={newPassword} onChange={e=>setNewPassword(e.target.value)} autoComplete="new-password" minLength={12} maxLength={72} required/><small>At least 12 characters.</small></label><label>Confirm new password<input type="password" value={confirmNewPassword} onChange={e=>setConfirmNewPassword(e.target.value)} autoComplete="new-password" minLength={12} maxLength={72} required/></label><button className="primary-button" disabled={passwordBusy}>{passwordBusy?'Updating…':'Change password'}</button></form></section>}

          {tab==='agents' && <section className="content-card module-card"><div className="module-toolbar"><div><h3>Specialist agents</h3><p>Agent roles and capabilities registered in AURORA</p></div><span className="building-label">7 SPECIALIST AGENTS</span></div><div className="agent-notice"><strong>AURORA request allowance · not provider billing</strong><p>{aiUsage ? (aiUsage.exempt ? `Owner/admin exemption active. Your new AI requests are not counted. The workspace has recorded ${aiUsage.used} of ${aiUsage.limit} requests this month (${aiUsage.remaining} remaining). Historical requests are not attributed by user, so this total may include admin activity from before the exemption was applied.` : `${aiUsage.used} of ${aiUsage.limit} member requests used this month · ${aiUsage.remaining} remaining.`) : 'Usage allowance unavailable.'} Each agent stage makes one provider call. A four-stage workflow makes four calls; member calls count against the allowance, while owner/admin calls are exempt. Provider charges still apply to every AI request. {aiUsage && (aiUsage.providerRequests !== undefined ? `Provider ledger this month: ${aiUsage.providerRequests} recorded calls · ${(aiUsage.totalTokens ?? 0).toLocaleString()} reported tokens · $${(aiUsage.reportedCostUsd ?? 0).toFixed(6)} reported cost. ${aiUsage.requestsWithoutReportedCost ?? 0} calls have no reported cost yet; do not interpret missing cost as free usage.` : 'Provider usage ledger awaits the backend database migration.')}</p>{aiUsage?.businessUsage && aiUsage.businessUsage.length > 0 && <div className="audit-list">{aiUsage.businessUsage.map(item=><div className="audit-row" key={item.businessId ?? 'workspace'}><div className="audit-marker"/><div><strong>{item.businessName || 'Workspace-level work'}</strong><p>{item.providerRequests} calls · {item.totalTokens.toLocaleString()} tokens · ${item.reportedCostUsd.toFixed(6)} reported cost</p><small>{item.requestsWithoutReportedCost} calls without confirmed cost</small></div></div>)}</div>}</div><div className="agent-grid">{agents.map(agent=><article className="agent-card" key={agent.id}><div className="agent-card-top"><span className="agent-avatar">✳</span><span className={`agent-state ${agent.status.toLowerCase()}`}>{agent.status.replaceAll('_',' ')}</span></div><h3>{agent.name}</h3><span className="agent-role">{agent.role}</span><p>{agent.description}</p><div className="agent-capabilities">{agent.capabilities.map(cap=><span key={cap}>{cap}</span>)}</div><small>{agent.modelConfigured?`Model credentials detected · ${agent.model}`:'No AI model credentials configured'}</small><button className="text-button" onClick={()=>{setSelectedAgentId(agent.id);setResearchReport(null)}}>Use this agent →</button></article>)}</div><div className="agent-notice"><strong>Multi-agent workflow · Chama opportunity validation</strong><p>Run Research → Strategy quality audit → Content → Operations in sequence. Strategy checks source freshness, evidence labels and student-specific proof before recommending action. This creates drafts and recommendations only; it does not publish, spend money or contact customers. Model usage may incur charges.</p><button className="primary-button" onClick={()=>void runChamaWorkflow()} disabled={workflowBusy||!agents.some(a=>a.modelConfigured)}>{workflowBusy?'Agents working in sequence…':'Run validation workflow'}</button>{workflowBusy&&<p>Each specialist is working on the previous agent’s output. This may take a few minutes.</p>}{workflowResult&&<div className="research-report"><div className="report-heading"><strong>{workflowResult.workflow}</strong><small>{workflowResult.status} · {workflowResult.runs.length}/4 stages completed</small></div>{workflowResult.runs.map(run=><article key={run.id}><strong>{run.agentName} · {run.status}</strong><pre>{run.output||run.error||'No output'}</pre></article>)}</div>}</div><form className="research-form" onSubmit={runResearch}><label>Business attribution<select value={selectedBusinessId} onChange={e=>setSelectedBusinessId(e.target.value)}><option value="">Platform / workspace-level work</option>{businesses.map(b=><option key={b.id} value={b.id}>{b.name}</option>)}</select><small>Choose a business to associate this run with its activity and spending ceiling.</small></label><label>Selected agent<select value={selectedAgentId} onChange={e=>setSelectedAgentId(e.target.value)}>{agents.map(a=><option key={a.id} value={a.id}>{a.name}</option>)}</select></label><label>Work request<textarea value={researchQuestion} onChange={e=>setResearchQuestion(e.target.value)} maxLength={5000} rows={4} placeholder="Describe the work you want this agent to perform…" required/></label><button className="primary-button" disabled={researchBusy||!agents.some(a=>a.id===selectedAgentId&&a.modelConfigured)}>{researchBusy?'Agent working…':'Run selected agent'}</button><small>{agents.some(a=>a.id===selectedAgentId&&a.modelConfigured)?'The request will be sent to the configured AI provider and saved in execution history.':'AI provider credentials are not configured yet. Add AURORA_AI_API_KEY on the server to enable execution.'}</small></form>{researchReport&&<article className="research-report"><div className="report-heading"><strong>{researchReport.agentName} · {researchReport.model || 'No model'}</strong><small>{researchReport.status} · {new Date(researchReport.startedAt).toLocaleString()}</small></div><pre>{researchReport.output || researchReport.error || 'No output returned.'}</pre></article>}<div className="section-title"><div><h3>Execution history</h3><p>Last 50 agent runs saved in AURORA</p></div></div><div className="audit-list">{agentRuns.map(run=><article className="audit-row" key={run.id}><div className="audit-marker"/><div><strong>{run.agentName} · {run.status}</strong><p>{run.input}</p><small>Run #{run.id} · {run.businessId ? businesses.find(b=>b.id===run.businessId)?.name || `Business #${run.businessId}` : 'Workspace-level'} · {new Date(run.startedAt).toLocaleString()}</small></div></article>)}{agentRuns.length===0&&<p className="empty-note">No agent runs yet.</p>}</div></section>}

          <footer className="page-footer"><span>PROJECT AURORA <b>·</b> VERSION 0.5.0</span><span>{loading?'Connecting…':online?'Persistent local database · API connected':'API disconnected'}</span></footer>
        </section>
      </main>
    </div>
  )
}

export default App
