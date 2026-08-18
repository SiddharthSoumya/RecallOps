import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  Activity,
  AlertTriangle,
  Brain,
  Check,
  ChevronRight,
  CircleDot,
  Database,
  History,
  Loader2,
  Play,
  RefreshCw,
  Server,
  ShieldCheck,
  Sparkles,
  Terminal,
  Zap,
} from 'lucide-react'
import './App.css'

type Severity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'

type InvestigationState =
  | 'NEW'
  | 'LOG_ANALYSIS'
  | 'GENERATE_HYPOTHESES'
  | 'RECALL_SIMILAR_INCIDENTS'
  | 'REFINE_HYPOTHESES'
  | 'PLAN_NEXT_ACTION'
  | 'WAITING'
  | 'RESUMED'
  | 'RESOLVED'
  | 'LEARNING'

interface Incident {
  id: string
  title: string
  description: string
  affectedService: string
  severity: Severity
  status: string
  createdAt: string
  updatedAt: string
}

interface Investigation {
  id: string
  incidentId: string
  state: InvestigationState
  resolved: boolean
  resolvedAt: string | null
  createdAt: string
  updatedAt: string
}

interface WorkingMemory {
  id: string
  investigationId: string
  currentHypothesis: string | null
  confidence: number | null
  observations: unknown
  completedActions: unknown
  nextAction: string | null
  version: number | null
  createdAt: string
  updatedAt: string
}

interface RecoveryResponse {
  investigationId: string
  incidentId: string
  previousState: InvestigationState
  recoveredState: InvestigationState
  workingMemory: WorkingMemory | null
}

const STATES: InvestigationState[] = [
  'NEW',
  'LOG_ANALYSIS',
  'GENERATE_HYPOTHESES',
  'RECALL_SIMILAR_INCIDENTS',
  'REFINE_HYPOTHESES',
  'PLAN_NEXT_ACTION',
  'WAITING',
  'RESUMED',
  'RESOLVED',
  'LEARNING',
]

const TRANSITIONS: Partial<Record<InvestigationState, InvestigationState>> = {
  NEW: 'LOG_ANALYSIS',
  LOG_ANALYSIS: 'GENERATE_HYPOTHESES',
  GENERATE_HYPOTHESES: 'RECALL_SIMILAR_INCIDENTS',
  RECALL_SIMILAR_INCIDENTS: 'REFINE_HYPOTHESES',
  REFINE_HYPOTHESES: 'PLAN_NEXT_ACTION',
  PLAN_NEXT_ACTION: 'WAITING',
  WAITING: 'RESUMED',
  RESUMED: 'RESOLVED',
  RESOLVED: 'LEARNING',
}

async function api<T>(url: string, options?: RequestInit): Promise<T> {
  const response = await fetch(url, {
    headers: {
      'Content-Type': 'application/json',
      ...(options?.headers ?? {}),
    },
    ...options,
  })

  if (!response.ok) {
    let message = `Request failed (${response.status})`

    try {
      const body = await response.json()
      message =
        body.message ||
        body.error ||
        body.detail ||
        message
    } catch {
      // Keep the default message.
    }

    throw new Error(message)
  }

  if (response.status === 204) {
    return undefined as T
  }

  return response.json() as Promise<T>
}

function formatState(state: InvestigationState) {
  return state.replaceAll('_', ' ')
}

function formatDate(value?: string | null) {
  if (!value) return '—'

  return new Intl.DateTimeFormat('en-IN', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value))
}

function confidencePercent(value?: number | null) {
  if (value == null) return '—'

  return `${Math.round(value * 100)}%`
}

function normalizeArray(value: unknown): string[] {
  if (!Array.isArray(value)) return []

  return value.map((item) =>
    typeof item === 'string' ? item : JSON.stringify(item),
  )
}

function App() {
  const [incident, setIncident] = useState<Incident | null>(null)
  const [investigation, setInvestigation] =
    useState<Investigation | null>(null)
  const [memory, setMemory] = useState<WorkingMemory | null>(null)

  const [loading, setLoading] = useState(false)
  const [backendOnline, setBackendOnline] = useState(false)
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')

  const [recovery, setRecovery] = useState<RecoveryResponse | null>(null)

  const [form, setForm] = useState({
    title: 'Payment service latency spike',
    description:
      'Payment API latency has increased sharply. Requests are intermittently timing out and customer transactions are failing.',
    affectedService: 'payment-service',
    severity: 'CRITICAL' as Severity,
  })

  const currentIndex = useMemo(
    () =>
      investigation
        ? STATES.indexOf(investigation.state)
        : -1,
    [investigation],
  )

  const checkBackend = useCallback(async () => {
    try {
      await api('/actuator/health')
      setBackendOnline(true)
    } catch {
      setBackendOnline(false)
    }
  }, [])

  useEffect(() => {
    const initialCheck = window.setTimeout(() => {
      void checkBackend()
    }, 0)

    const timer = window.setInterval(() => {
      void checkBackend()
    }, 5000)

    return () => {
      window.clearTimeout(initialCheck)
      window.clearInterval(timer)
    }
  }, [checkBackend])

  const clearNotifications = () => {
    setError('')
    setMessage('')
  }

  const createIncident = async () => {
    clearNotifications()
    setLoading(true)

    try {
      const created = await api<Incident>('/api/v1/incidents', {
        method: 'POST',
        body: JSON.stringify(form),
      })

      setIncident(created)
      setInvestigation(null)
      setMemory(null)
      setRecovery(null)

      setMessage('Incident created successfully.')
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to create incident.')
    } finally {
      setLoading(false)
    }
  }

  const startInvestigation = async () => {
    if (!incident) return

    clearNotifications()
    setLoading(true)

    try {
      const created = await api<Investigation>(
        `/api/v1/incidents/${incident.id}/investigation`,
        {
          method: 'POST',
        },
      )

      setInvestigation(created)
      setMemory(null)
      setRecovery(null)

      setMessage('Investigation initialized in NEW state.')
    } catch (e) {
      setError(
        e instanceof Error
          ? e.message
          : 'Failed to start investigation.',
      )
    } finally {
      setLoading(false)
    }
  }

  const createMemory = async () => {
    if (!investigation) return

    clearNotifications()
    setLoading(true)

    try {
      const created = await api<WorkingMemory>(
        `/api/v1/investigations/${investigation.id}/memory`,
        {
          method: 'POST',
          body: JSON.stringify({
            currentHypothesis:
              'Database connection pool exhaustion is causing payment request latency.',
            confidence: 0.91,
            observations: [
              'Payment API latency increased',
              'Requests intermittently time out',
              'Database connection utilization is suspected to be saturated',
            ],
            completedActions: [
              'Collected payment service telemetry',
              'Reviewed recent latency symptoms',
            ],
            nextAction:
              'Inspect database connection pool utilization and active connections.',
          }),
        },
      )

      setMemory(created)
      setMessage('Working memory persisted to CockroachDB.')
    } catch (e) {
      setError(
        e instanceof Error
          ? e.message
          : 'Failed to create working memory.',
      )
    } finally {
      setLoading(false)
    }
  }

  const refreshData = async () => {
    if (!investigation) {
      await checkBackend()
      return
    }

    clearNotifications()
    setLoading(true)

    try {
      const [freshInvestigation, freshMemory] =
        await Promise.all([
          api<Investigation>(
            `/api/v1/investigations/${investigation.id}`,
          ),
          api<WorkingMemory>(
            `/api/v1/investigations/${investigation.id}/memory`,
          ).catch(() => null),
        ])

      setInvestigation(freshInvestigation)
      setMemory(freshMemory)

      if (incident) {
        try {
          const freshIncident = await api<Incident>(
            `/api/v1/incidents/${incident.id}`,
          )
          setIncident(freshIncident)
        } catch {
          // Investigation data is still useful.
        }
      }

      setMessage('Data refreshed from backend.')
    } catch (e) {
      setError(
        e instanceof Error ? e.message : 'Backend is unavailable.',
      )
      setBackendOnline(false)
    } finally {
      setLoading(false)
    }
  }

  const advanceState = async () => {
    if (!investigation) return

    const target = TRANSITIONS[investigation.state]

    if (!target) {
      setMessage('This investigation is already at a terminal state.')
      return
    }

    clearNotifications()
    setLoading(true)

    try {
      const updated = await api<Investigation>(
        `/api/v1/investigations/${investigation.id}/state`,
        {
          method: 'PATCH',
          body: JSON.stringify({
            targetState: target,
          }),
        },
      )

      setInvestigation(updated)

      if (target === 'RESOLVED') {
        setMessage('Investigation resolved. Next checkpoint: LEARNING.')
      } else {
        setMessage(`Investigation advanced to ${target}.`)
      }
    } catch (e) {
      setError(
        e instanceof Error ? e.message : 'State transition failed.',
      )
    } finally {
      setLoading(false)
    }
  }

  const agentStep = async () => {
    if (!investigation || !memory) return

    clearNotifications()
    setLoading(true)

    try {
      const updated = await api<WorkingMemory>(
        `/api/v1/investigations/${investigation.id}/agent/step`,
        {
          method: 'POST',
        },
      )

      setMemory(updated)
      setMessage('Agent reasoning step completed and memory persisted.')
    } catch (e) {
      setError(
        e instanceof Error ? e.message : 'Agent step failed.',
      )
    } finally {
      setLoading(false)
    }
  }

  const recover = async () => {
    if (!investigation) return

    clearNotifications()
    setLoading(true)

    try {
      const result = await api<RecoveryResponse>(
        `/api/v1/investigations/${investigation.id}/recover`,
        {
          method: 'POST',
        },
      )

      setRecovery(result)

      const freshInvestigation = await api<Investigation>(
        `/api/v1/investigations/${investigation.id}`,
      )

      setInvestigation(freshInvestigation)
      setMemory(result.workingMemory)

      setMessage(
        `Recovery complete: ${result.previousState} → ${result.recoveredState}.`,
      )
    } catch (e) {
      setError(
        e instanceof Error ? e.message : 'Recovery failed.',
      )
    } finally {
      setLoading(false)
    }
  }

  const resetDemo = () => {
    setIncident(null)
    setInvestigation(null)
    setMemory(null)
    setRecovery(null)
    clearNotifications()
  }

  const canCreateMemory =
    !!investigation &&
    !memory

  const canAgentStep =
    !!investigation &&
    !!memory &&
    !investigation.resolved &&
    investigation.state !== 'LEARNING'

  const canAdvance =
    !!investigation &&
    !!memory &&
    !!TRANSITIONS[investigation.state]

  return (
    <div className="app-shell grid-bg min-h-screen">
      <header className="sticky top-0 z-30 border-b border-slate-800/80 bg-[#070b12]/90 backdrop-blur-xl">
        <div className="mx-auto flex max-w-[1600px] items-center justify-between px-4 py-4 sm:px-6 lg:px-8">
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-blue-600/15 text-blue-400 ring-1 ring-blue-500/20">
              <Brain size={21} />
            </div>

            <div>
              <div className="flex items-center gap-2">
                <h1 className="text-lg font-bold tracking-tight text-white">
                  RecallOps
                </h1>

                <span className="rounded-md border border-blue-500/20 bg-blue-500/10 px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wider text-blue-300">
                  AI SRE
                </span>
              </div>

              <p className="text-xs text-slate-500">
                Persistent incident investigation memory
              </p>
            </div>
          </div>

          <div className="flex items-center gap-3">
            <div className="hidden items-center gap-2 rounded-lg border border-slate-800 bg-slate-900/60 px-3 py-2 text-xs sm:flex">
              <span
                className={`h-2 w-2 rounded-full ${
                  backendOnline
                    ? 'bg-emerald-400'
                    : 'bg-red-400 pulse-soft'
                }`}
              />
              <span className="text-slate-400">
                Backend
              </span>
              <span
                className={
                  backendOnline
                    ? 'font-medium text-emerald-400'
                    : 'font-medium text-red-400'
                }
              >
                {backendOnline ? 'ONLINE' : 'OFFLINE'}
              </span>
            </div>

            <button
              onClick={() => void refreshData()}
              disabled={loading}
              className="flex items-center gap-2 rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-xs font-medium text-slate-300 transition hover:border-slate-600 hover:text-white disabled:opacity-50"
            >
              <RefreshCw
                size={14}
                className={loading ? 'animate-spin' : ''}
              />
              Refresh
            </button>

            <button
              onClick={resetDemo}
              className="hidden rounded-lg border border-slate-800 px-3 py-2 text-xs text-slate-500 transition hover:border-slate-700 hover:text-slate-300 md:block"
            >
              Reset
            </button>
          </div>
        </div>
      </header>

      <main className="mx-auto max-w-[1600px] px-4 py-6 sm:px-6 lg:px-8 lg:py-8">
        {error && (
          <div className="mb-5 flex items-start gap-3 rounded-xl border border-red-500/20 bg-red-500/10 p-4 text-sm text-red-300">
            <AlertTriangle size={18} className="mt-0.5 shrink-0" />
            <div>
              <div className="font-semibold">Request failed</div>
              <div className="mt-1 text-red-300/80">{error}</div>
            </div>
          </div>
        )}

        {message && !error && (
          <div className="mb-5 flex items-center gap-3 rounded-xl border border-emerald-500/20 bg-emerald-500/10 p-4 text-sm text-emerald-300">
            <Check size={18} />
            {message}
          </div>
        )}

        <section className="mb-8">
          <div className="mb-2 flex items-center gap-2 text-xs font-semibold uppercase tracking-[0.18em] text-blue-400">
            <Activity size={14} />
            Autonomous Incident Investigation
          </div>

          <h2 className="max-w-4xl text-3xl font-bold tracking-tight text-white md:text-4xl">
            An SRE agent that{' '}
            <span className="text-blue-400">
              never forgets
            </span>{' '}
            an investigation.
          </h2>

          <p className="mt-3 max-w-3xl text-sm leading-6 text-slate-400">
            RecallOps persists the agent&apos;s investigation state and
            working memory in CockroachDB, allowing an unfinished
            investigation to recover after process failure.
          </p>
        </section>

        <div className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_360px]">
          <div className="space-y-6">
            <section className="glass rounded-2xl p-5 shadow-2xl shadow-black/20">
              <div className="mb-5 flex items-center justify-between">
                <div>
                  <h3 className="flex items-center gap-2 font-semibold text-white">
                    <AlertTriangle size={17} className="text-amber-400" />
                    Incident
                  </h3>
                  <p className="mt-1 text-xs text-slate-500">
                    Create a production incident for the agent.
                  </p>
                </div>

                {incident && (
                  <span className="rounded-full bg-emerald-500/10 px-3 py-1 text-xs font-medium text-emerald-400">
                    {incident.status}
                  </span>
                )}
              </div>

              <div className="grid gap-4 md:grid-cols-2">
                <div className="md:col-span-2">
                  <label className="mb-2 block text-xs font-medium text-slate-400">
                    Incident title
                  </label>

                  <input
                    value={form.title}
                    onChange={(e) =>
                      setForm({
                        ...form,
                        title: e.target.value,
                      })
                    }
                    disabled={!!incident}
                    className="w-full rounded-xl border border-slate-700 bg-slate-950/70 px-4 py-3 text-sm text-white outline-none transition placeholder:text-slate-600 focus:border-blue-500 disabled:opacity-60"
                  />
                </div>

                <div className="md:col-span-2">
                  <label className="mb-2 block text-xs font-medium text-slate-400">
                    Description
                  </label>

                  <textarea
                    rows={3}
                    value={form.description}
                    onChange={(e) =>
                      setForm({
                        ...form,
                        description: e.target.value,
                      })
                    }
                    disabled={!!incident}
                    className="w-full resize-none rounded-xl border border-slate-700 bg-slate-950/70 px-4 py-3 text-sm leading-6 text-white outline-none transition placeholder:text-slate-600 focus:border-blue-500 disabled:opacity-60"
                  />
                </div>

                <div>
                  <label className="mb-2 block text-xs font-medium text-slate-400">
                    Affected service
                  </label>

                  <input
                    value={form.affectedService}
                    onChange={(e) =>
                      setForm({
                        ...form,
                        affectedService: e.target.value,
                      })
                    }
                    disabled={!!incident}
                    className="w-full rounded-xl border border-slate-700 bg-slate-950/70 px-4 py-3 text-sm text-white outline-none focus:border-blue-500 disabled:opacity-60"
                  />
                </div>

                <div>
                  <label className="mb-2 block text-xs font-medium text-slate-400">
                    Severity
                  </label>

                  <select
                    value={form.severity}
                    onChange={(e) =>
                      setForm({
                        ...form,
                        severity: e.target.value as Severity,
                      })
                    }
                    disabled={!!incident}
                    className="w-full rounded-xl border border-slate-700 bg-slate-950/70 px-4 py-3 text-sm text-white outline-none focus:border-blue-500 disabled:opacity-60"
                  >
                    <option value="LOW">LOW</option>
                    <option value="MEDIUM">MEDIUM</option>
                    <option value="HIGH">HIGH</option>
                    <option value="CRITICAL">CRITICAL</option>
                  </select>
                </div>
              </div>

              {!incident && (
                <button
                  onClick={() => void createIncident()}
                  disabled={loading || !backendOnline}
                  className="mt-5 flex w-full items-center justify-center gap-2 rounded-xl bg-blue-600 px-4 py-3 text-sm font-semibold text-white transition hover:bg-blue-500 disabled:cursor-not-allowed disabled:opacity-50"
                >
                  {loading ? (
                    <Loader2 size={17} className="animate-spin" />
                  ) : (
                    <Zap size={17} />
                  )}
                  Create Incident
                </button>
              )}

              {incident && (
                <div className="mt-5 flex items-center justify-between rounded-xl border border-slate-800 bg-slate-950/40 p-4">
                  <div>
                    <div className="text-xs text-slate-500">
                      Incident ID
                    </div>
                    <div className="mt-1 break-all font-mono text-xs text-slate-300">
                      {incident.id}
                    </div>
                  </div>

                  {!investigation && (
                    <button
                      onClick={() => void startInvestigation()}
                      disabled={loading}
                      className="flex shrink-0 items-center gap-2 rounded-lg bg-emerald-600 px-4 py-2 text-xs font-semibold text-white transition hover:bg-emerald-500 disabled:opacity-50"
                    >
                      <Play size={14} />
                      Start Investigation
                    </button>
                  )}
                </div>
              )}
            </section>

            <section className="glass rounded-2xl p-5">
              <div className="mb-6 flex items-center justify-between">
                <div>
                  <h3 className="flex items-center gap-2 font-semibold text-white">
                    <History size={17} className="text-blue-400" />
                    Investigation State
                  </h3>
                  <p className="mt-1 text-xs text-slate-500">
                    Durable state machine persisted by Spring Boot.
                  </p>
                </div>

                {investigation && (
                  <div className="rounded-lg border border-blue-500/20 bg-blue-500/10 px-3 py-2 text-xs font-semibold text-blue-300">
                    {formatState(investigation.state)}
                  </div>
                )}
              </div>

              {!investigation ? (
                <div className="flex min-h-[170px] items-center justify-center rounded-xl border border-dashed border-slate-800 text-center">
                  <div>
                    <CircleDot
                      size={28}
                      className="mx-auto mb-3 text-slate-700"
                    />
                    <p className="text-sm text-slate-500">
                      Start an investigation to view its state machine.
                    </p>
                  </div>
                </div>
              ) : (
                <>
                  <div className="relative overflow-x-auto pb-4">
                    <div className="relative flex min-w-[900px] justify-between gap-3">
                      <div className="state-line" />

                      {STATES.map((state, index) => {
                        const reached = index <= currentIndex
                        const active = state === investigation.state

                        return (
                          <div
                            key={state}
                            className="relative z-10 flex w-[82px] shrink-0 flex-col items-center text-center"
                          >
                            <div
                              className={`flex h-9 w-9 items-center justify-center rounded-full border-2 transition ${
                                active
                                  ? 'border-blue-400 bg-blue-500 text-white shadow-lg shadow-blue-500/20'
                                  : reached
                                    ? 'border-emerald-400/70 bg-emerald-500/10 text-emerald-400'
                                    : 'border-slate-700 bg-slate-950 text-slate-600'
                              }`}
                            >
                              {reached && !active ? (
                                <Check size={15} />
                              ) : (
                                <span className="text-[10px] font-bold">
                                  {index + 1}
                                </span>
                              )}
                            </div>

                            <span
                              className={`mt-3 text-[10px] font-medium leading-4 ${
                                active
                                  ? 'text-blue-300'
                                  : reached
                                    ? 'text-slate-300'
                                    : 'text-slate-600'
                              }`}
                            >
                              {formatState(state)}
                            </span>
                          </div>
                        )
                      })}
                    </div>
                  </div>

                  <div className="mt-3 grid gap-3 sm:grid-cols-3">
                    <Metric
                      icon={<Server size={16} />}
                      label="Current state"
                      value={formatState(investigation.state)}
                    />

                    <Metric
                      icon={<Database size={16} />}
                      label="Persistence"
                      value={memory ? 'Memory persisted' : 'Awaiting memory'}
                    />

                    <Metric
                      icon={<ShieldCheck size={16} />}
                      label="Resolution"
                      value={
                        investigation.resolved
                          ? 'Resolved'
                          : 'In progress'
                      }
                    />
                  </div>
                </>
              )}
            </section>

            {investigation && (
              <section className="glass rounded-2xl p-5">
                <div className="mb-5 flex items-center justify-between">
                  <div>
                    <h3 className="flex items-center gap-2 font-semibold text-white">
                      <Terminal size={17} className="text-cyan-400" />
                      Investigation Controls
                    </h3>
                    <p className="mt-1 text-xs text-slate-500">
                      Execute real backend operations.
                    </p>
                  </div>
                </div>

                <div className="grid gap-3 sm:grid-cols-3">
                  {canCreateMemory && (
                    <ActionButton
                      icon={<Database size={16} />}
                      label="Persist Working Memory"
                      description="Write checkpoint"
                      onClick={() => void createMemory()}
                      disabled={loading}
                    />
                  )}

                  {canAgentStep && (
                    <ActionButton
                      icon={<Sparkles size={16} />}
                      label="Run Agent Step"
                      description="Update reasoning"
                      onClick={() => void agentStep()}
                      disabled={loading}
                    />
                  )}

                  {canAdvance && (
                    <ActionButton
                      icon={<ChevronRight size={16} />}
                      label={`Advance → ${formatState(
                        TRANSITIONS[investigation.state]!,
                      )}`}
                      description="Persist state transition"
                      onClick={() => void advanceState()}
                      disabled={loading}
                    />
                  )}

                  {investigation.state === 'WAITING' && (
                    <ActionButton
                      icon={<RefreshCw size={16} />}
                      label="Recover Investigation"
                      description="Restore from checkpoint"
                      onClick={() => void recover()}
                      disabled={loading}
                      highlight
                    />
                  )}
                </div>

                <div className="mt-4 rounded-xl border border-amber-500/15 bg-amber-500/5 p-4">
                  <div className="flex items-start gap-3">
                    <AlertTriangle
                      size={17}
                      className="mt-0.5 shrink-0 text-amber-400"
                    />

                    <div>
                      <div className="text-xs font-semibold text-amber-300">
                        Crash demonstration
                      </div>

                      <p className="mt-1 text-xs leading-5 text-slate-500">
                        To demonstrate an actual process failure, stop the
                        Spring Boot process while the investigation is in
                        WAITING. CockroachDB retains the checkpoint. Restart
                        the backend, then click Refresh/Recover.
                      </p>
                    </div>
                  </div>
                </div>
              </section>
            )}
          </div>

          <aside className="space-y-6">
            <section className="glass rounded-2xl p-5">
              <div className="mb-5 flex items-center justify-between">
                <div>
                  <h3 className="flex items-center gap-2 font-semibold text-white">
                    <Brain size={17} className="text-purple-400" />
                    Working Memory
                  </h3>
                  <p className="mt-1 text-xs text-slate-500">
                    Agent context persisted in CockroachDB.
                  </p>
                </div>

                {memory && (
                  <span className="rounded-md bg-purple-500/10 px-2 py-1 text-[10px] font-semibold text-purple-300">
                    v{memory.version ?? '—'}
                  </span>
                )}
              </div>

              {!memory ? (
                <div className="rounded-xl border border-dashed border-slate-800 p-8 text-center">
                  <Database
                    size={28}
                    className="mx-auto mb-3 text-slate-700"
                  />

                  <p className="text-sm text-slate-500">
                    No working memory loaded.
                  </p>

                  <p className="mt-1 text-xs text-slate-600">
                    Persist a checkpoint to begin the memory demo.
                  </p>
                </div>
              ) : (
                <div className="space-y-5">
                  <div>
                    <div className="mb-2 text-[10px] font-semibold uppercase tracking-wider text-slate-500">
                      Current hypothesis
                    </div>

                    <p className="text-sm leading-6 text-slate-200">
                      {memory.currentHypothesis || 'No hypothesis'}
                    </p>
                  </div>

                  <div>
                    <div className="mb-2 flex items-center justify-between text-[10px] font-semibold uppercase tracking-wider text-slate-500">
                      <span>Confidence</span>
                      <span className="text-emerald-400">
                        {confidencePercent(memory.confidence)}
                      </span>
                    </div>

                    <div className="h-2 overflow-hidden rounded-full bg-slate-800">
                      <div
                        className="h-full rounded-full bg-emerald-400 transition-all"
                        style={{
                          width: `${Math.max(
                            0,
                            Math.min(
                              100,
                              (memory.confidence ?? 0) * 100,
                            ),
                          )}%`,
                        }}
                      />
                    </div>
                  </div>

                  <MemoryList
                    title="Observations"
                    items={normalizeArray(memory.observations)}
                  />

                  <MemoryList
                    title="Completed actions"
                    items={normalizeArray(memory.completedActions)}
                  />

                  <div className="rounded-xl border border-blue-500/15 bg-blue-500/5 p-4">
                    <div className="mb-2 text-[10px] font-semibold uppercase tracking-wider text-blue-400">
                      Next action
                    </div>

                    <p className="text-sm leading-5 text-blue-100">
                      {memory.nextAction || 'No next action'}
                    </p>
                  </div>

                  <div className="border-t border-slate-800 pt-4 text-[10px] text-slate-600">
                    Last persisted: {formatDate(memory.updatedAt)}
                  </div>
                </div>
              )}
            </section>

            <section className="glass rounded-2xl p-5">
              <div className="mb-4 flex items-center gap-2">
                <Database size={17} className="text-cyan-400" />
                <h3 className="font-semibold text-white">
                  Persistence Layer
                </h3>
              </div>

              <div className="space-y-3">
                <ArchitectureRow
                  label="Investigation state"
                  value={
                    investigation
                      ? formatState(investigation.state)
                      : 'Not started'
                  }
                  active={!!investigation}
                />

                <ArchitectureRow
                  label="Working memory"
                  value={memory ? 'Persisted' : 'Not created'}
                  active={!!memory}
                />

                <ArchitectureRow
                  label="Recovery service"
                  value={
                    recovery
                      ? `${recovery.previousState} → ${recovery.recoveredState}`
                      : 'Startup capable'
                  }
                  active={!!recovery}
                />

                <ArchitectureRow
                  label="Database"
                  value="CockroachDB"
                  active
                />
              </div>
            </section>

            {recovery && (
              <section className="rounded-2xl border border-emerald-500/20 bg-emerald-500/5 p-5">
                <div className="flex items-start gap-3">
                  <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-emerald-500/10 text-emerald-400">
                    <RefreshCw size={17} />
                  </div>

                  <div>
                    <div className="font-semibold text-emerald-300">
                      Recovery successful
                    </div>

                    <p className="mt-1 text-xs leading-5 text-slate-500">
                      The backend restored the investigation from its
                      persisted checkpoint.
                    </p>

                    <div className="mt-3 flex items-center gap-2 font-mono text-xs">
                      <span className="text-slate-400">
                        {recovery.previousState}
                      </span>
                      <ChevronRight
                        size={13}
                        className="text-emerald-400"
                      />
                      <span className="font-semibold text-emerald-400">
                        {recovery.recoveredState}
                      </span>
                    </div>
                  </div>
                </div>
              </section>
            )}
          </aside>
        </div>

        <footer className="mt-10 border-t border-slate-800/70 py-6">
          <div className="flex flex-col justify-between gap-3 text-xs text-slate-600 sm:flex-row">
            <span>
              RecallOps · Durable AI incident investigation
            </span>

            <span>
              Spring Boot · CockroachDB · Persistent Agent Memory
            </span>
          </div>
        </footer>
      </main>
    </div>
  )
}

function Metric({
  icon,
  label,
  value,
}: {
  icon: React.ReactNode
  label: string
  value: string
}) {
  return (
    <div className="rounded-xl border border-slate-800 bg-slate-950/40 p-4">
      <div className="mb-2 flex items-center gap-2 text-slate-500">
        {icon}
        <span className="text-[10px] font-semibold uppercase tracking-wider">
          {label}
        </span>
      </div>

      <div className="truncate text-sm font-medium text-slate-200">
        {value}
      </div>
    </div>
  )
}

function ActionButton({
  icon,
  label,
  description,
  onClick,
  disabled,
  highlight = false,
}: {
  icon: React.ReactNode
  label: string
  description: string
  onClick: () => void
  disabled?: boolean
  highlight?: boolean
}) {
  return (
    <button
      onClick={onClick}
      disabled={disabled}
      className={`group rounded-xl border p-4 text-left transition disabled:cursor-not-allowed disabled:opacity-50 ${
        highlight
          ? 'border-emerald-500/30 bg-emerald-500/10 hover:border-emerald-400/50 hover:bg-emerald-500/15'
          : 'border-slate-800 bg-slate-950/40 hover:border-blue-500/30 hover:bg-blue-500/5'
      }`}
    >
      <div
        className={`mb-3 flex h-8 w-8 items-center justify-center rounded-lg ${
          highlight
            ? 'bg-emerald-500/10 text-emerald-400'
            : 'bg-blue-500/10 text-blue-400'
        }`}
      >
        {icon}
      </div>

      <div className="text-xs font-semibold text-slate-200">
        {label}
      </div>

      <div className="mt-1 text-[10px] text-slate-600">
        {description}
      </div>
    </button>
  )
}

function MemoryList({
  title,
  items,
}: {
  title: string
  items: string[]
}) {
  return (
    <div>
      <div className="mb-2 text-[10px] font-semibold uppercase tracking-wider text-slate-500">
        {title}
      </div>

      {items.length === 0 ? (
        <p className="text-xs text-slate-600">
          Nothing recorded yet.
        </p>
      ) : (
        <ul className="space-y-2">
          {items.map((item, index) => (
            <li
              key={`${item}-${index}`}
              className="flex gap-2 text-xs leading-5 text-slate-400"
            >
              <span className="mt-2 h-1 w-1 shrink-0 rounded-full bg-slate-600" />
              <span>{item}</span>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}

function ArchitectureRow({
  label,
  value,
  active,
}: {
  label: string
  value: string
  active: boolean
}) {
  return (
    <div className="flex items-center justify-between rounded-lg border border-slate-800 bg-slate-950/30 px-3 py-3">
      <span className="text-xs text-slate-500">
        {label}
      </span>

      <div className="flex max-w-[55%] items-center gap-2">
        <span
          className={`h-1.5 w-1.5 rounded-full ${
            active ? 'bg-emerald-400' : 'bg-slate-700'
          }`}
        />

        <span className="truncate text-xs font-medium text-slate-300">
          {value}
        </span>
      </div>
    </div>
  )
}

export default App
