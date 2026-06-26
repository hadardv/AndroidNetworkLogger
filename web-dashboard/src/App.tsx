import { useNetworkLogs } from './hooks/useNetworkLogs'
import type { NetworkLog } from './types/NetworkLog'

function formatJson(raw: string): string {
  if (!raw) return '(empty)'
  try {
    return JSON.stringify(JSON.parse(raw), null, 2)
  } catch {
    return raw
  }
}

function formatHeaders(raw: string): string {
  if (!raw) return '(none)'
  try {
    const parsed = JSON.parse(raw) as Record<string, string>
    return Object.entries(parsed)
      .map(([key, value]) => `${key}: ${value}`)
      .join('\n')
  } catch {
    return raw
  }
}

function statusColor(log: NetworkLog): string {
  if (!log.isSuccess) return 'text-red-500'
  if (log.statusCode >= 400) return 'text-amber-500'
  return 'text-emerald-500'
}

function StatusBadge({ status }: { status: string }) {
  const colors: Record<string, string> = {
    connected: 'bg-emerald-100 text-emerald-800',
    connecting: 'bg-amber-100 text-amber-800',
    disconnected: 'bg-gray-100 text-gray-600',
    error: 'bg-red-100 text-red-800',
  }
  return (
    <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${colors[status] ?? colors.disconnected}`}>
      {status}
    </span>
  )
}

function LogListItem({
  log,
  isSelected,
  onSelect,
}: {
  log: NetworkLog
  isSelected: boolean
  onSelect: () => void
}) {
  return (
    <button
      type="button"
      onClick={onSelect}
      className={`w-full border-b border-gray-200 px-4 py-3 text-left transition hover:bg-gray-50 ${
        isSelected ? 'bg-indigo-50 border-l-4 border-l-indigo-500' : 'border-l-4 border-l-transparent'
      }`}
    >
      <div className="flex items-center gap-2">
        <span className="rounded bg-gray-200 px-1.5 py-0.5 font-mono text-xs font-semibold">
          {log.method}
        </span>
        <span className={`font-mono text-xs font-semibold ${statusColor(log)}`}>
          {log.statusCode > 0 ? log.statusCode : 'ERR'}
        </span>
        <span className="ml-auto font-mono text-xs text-gray-400">{log.durationMs}ms</span>
      </div>
      <p className="mt-1 truncate font-mono text-sm text-gray-700">{log.url}</p>
      <p className="mt-0.5 text-xs text-gray-400">
        {new Date(log.timestamp).toLocaleTimeString()}
      </p>
    </button>
  )
}

function DetailPanel({ log }: { log: NetworkLog | null }) {
  if (!log) {
    return (
      <div className="flex h-full items-center justify-center text-gray-400">
        Select a request to inspect details
      </div>
    )
  }

  return (
    <div className="flex h-full flex-col overflow-hidden">
      <div className="border-b border-gray-200 bg-gray-50 px-6 py-4">
        <div className="flex flex-wrap items-center gap-3">
          <span className="rounded bg-indigo-100 px-2 py-1 font-mono text-sm font-semibold text-indigo-800">
            {log.method}
          </span>
          <span className={`font-mono text-sm font-semibold ${statusColor(log)}`}>
            {log.statusCode > 0 ? log.statusCode : 'Network Error'}
          </span>
          <span className="font-mono text-sm text-gray-500">{log.durationMs} ms</span>
        </div>
        <p className="mt-2 break-all font-mono text-sm text-gray-800">{log.url}</p>
      </div>

      <div className="flex-1 overflow-y-auto p-6 space-y-6">
        <section>
          <h3 className="mb-2 text-xs font-semibold uppercase tracking-wide text-gray-500">
            Request Headers
          </h3>
          <pre className="overflow-x-auto rounded-lg bg-gray-900 p-4 font-mono text-xs text-gray-100">
            {formatHeaders(log.requestHeaders)}
          </pre>
        </section>

        <section>
          <h3 className="mb-2 text-xs font-semibold uppercase tracking-wide text-gray-500">
            Request Body
          </h3>
          <pre className="overflow-x-auto rounded-lg bg-gray-900 p-4 font-mono text-xs text-green-300">
            {formatJson(log.requestBody)}
          </pre>
        </section>

        <section>
          <h3 className="mb-2 text-xs font-semibold uppercase tracking-wide text-gray-500">
            Response Headers
          </h3>
          <pre className="overflow-x-auto rounded-lg bg-gray-900 p-4 font-mono text-xs text-gray-100">
            {formatHeaders(log.responseHeaders)}
          </pre>
        </section>

        <section>
          <h3 className="mb-2 text-xs font-semibold uppercase tracking-wide text-gray-500">
            Response Body
          </h3>
          <pre className="overflow-x-auto rounded-lg bg-gray-900 p-4 font-mono text-xs text-green-300">
            {formatJson(log.responseBody)}
          </pre>
        </section>
      </div>
    </div>
  )
}

export default function App() {
  const { logs, selectedLog, selectLog, connectionStatus, error, retry } = useNetworkLogs()

  return (
    <div className="flex h-screen flex-col bg-white">
      <header className="flex items-center justify-between border-b border-gray-200 px-6 py-4">
        <div>
          <h1 className="text-xl font-semibold text-gray-900">Network Logger Dashboard</h1>
          <p className="text-sm text-gray-500">
            Real-time inspection via ADB port forward (localhost:8080)
          </p>
        </div>
        <div className="flex items-center gap-3">
          <StatusBadge status={connectionStatus} />
          <span className="text-sm text-gray-500">{logs.length} requests</span>
          {connectionStatus === 'error' && (
            <button
              type="button"
              onClick={retry}
              className="rounded bg-indigo-600 px-3 py-1 text-sm text-white hover:bg-indigo-700"
            >
              Retry
            </button>
          )}
        </div>
      </header>

      {error && (
        <div className="border-b border-amber-200 bg-amber-50 px-6 py-3 text-sm text-amber-800 whitespace-pre-line">
          {error}
        </div>
      )}

      <div className="flex flex-1 overflow-hidden">
        {/* Left panel – request list */}
        <aside className="w-96 shrink-0 overflow-y-auto border-r border-gray-200 bg-white">
          {logs.length === 0 ? (
            <p className="p-6 text-sm text-gray-400">
              No requests yet. Fire a call from the demo app.
            </p>
          ) : (
            logs.map((log) => (
              <LogListItem
                key={log.id}
                log={log}
                isSelected={selectedLog?.id === log.id}
                onSelect={() => selectLog(log.id)}
              />
            ))
          )}
        </aside>

        {/* Right panel – request/response details */}
        <main className="flex-1 overflow-hidden">
          <DetailPanel log={selectedLog} />
        </main>
      </div>
    </div>
  )
}
