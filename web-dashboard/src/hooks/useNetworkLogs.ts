import { useCallback, useEffect, useRef, useState } from 'react'
import type { ConnectionStatus, NetworkLog } from '../types/NetworkLog'

const API_BASE = 'http://localhost:8080'
const WS_URL = 'ws://localhost:8080/ws/logs'
const MAX_RETRIES = 10
const RETRY_DELAY_MS = 2000

function mergeLogs(existing: NetworkLog[], incoming: NetworkLog[]): NetworkLog[] {
  const byId = new Map<number, NetworkLog>()
  for (const log of [...existing, ...incoming]) {
    byId.set(log.id, log)
  }
  return Array.from(byId.values()).sort((a, b) => b.timestamp - a.timestamp)
}

async function checkServerReachable(): Promise<boolean> {
  try {
    const response = await fetch(`${API_BASE}/api/logs`, { signal: AbortSignal.timeout(3000) })
    return response.ok
  } catch {
    return false
  }
}

export function useNetworkLogs() {
  const [logs, setLogs] = useState<NetworkLog[]>([])
  const [selectedId, setSelectedId] = useState<number | null>(null)
  const [connectionStatus, setConnectionStatus] = useState<ConnectionStatus>('connecting')
  const [error, setError] = useState<string | null>(null)
  const wsRef = useRef<WebSocket | null>(null)
  const retryCountRef = useRef(0)
  const retryTimerRef = useRef<number | null>(null)
  const intentionalCloseRef = useRef(false)

  const selectedLog = logs.find((log) => log.id === selectedId) ?? logs[0] ?? null

  const selectLog = useCallback((id: number) => {
    setSelectedId(id)
  }, [])

  const loadHistory = useCallback(async () => {
    const response = await fetch(`${API_BASE}/api/logs`)
    if (!response.ok) throw new Error(`HTTP ${response.status}`)
    const data = (await response.json()) as NetworkLog[]
    setLogs(data)
    if (data.length > 0) setSelectedId((prev) => prev ?? data[0].id)
  }, [])

  const connectWebSocket = useCallback(() => {
    intentionalCloseRef.current = false
    setConnectionStatus('connecting')

    const ws = new WebSocket(WS_URL)
    wsRef.current = ws

    ws.onopen = () => {
      retryCountRef.current = 0
      setConnectionStatus('connected')
      setError(null)
    }

    ws.onmessage = (event) => {
      try {
        const log = JSON.parse(event.data as string) as NetworkLog
        setLogs((prev) => mergeLogs(prev, [log]))
        setSelectedId((prev) => prev ?? log.id)
      } catch {
        // Ignore malformed frames
      }
    }

    ws.onerror = () => {
      // onclose will handle retry logic
    }

    ws.onclose = () => {
      if (intentionalCloseRef.current) {
        setConnectionStatus('disconnected')
        return
      }

      if (retryCountRef.current < MAX_RETRIES) {
        retryCountRef.current += 1
        setConnectionStatus('connecting')
        retryTimerRef.current = window.setTimeout(connectWebSocket, RETRY_DELAY_MS)
      } else {
        setConnectionStatus('error')
        setError(
          'Cannot reach the Android app on localhost:8080. ' +
            'Make sure the demo app is running, then run: adb forward tcp:8080 tcp:8080',
        )
      }
    }
  }, [])

  const retry = useCallback(async () => {
    retryCountRef.current = 0
    setError(null)
    setConnectionStatus('connecting')

    const reachable = await checkServerReachable()
    if (!reachable) {
      setConnectionStatus('error')
      setError(
        'Cannot reach localhost:8080. Run this in a terminal, then click Retry:\n' +
          'adb forward tcp:8080 tcp:8080',
      )
      return
    }

    try {
      await loadHistory()
    } catch (err) {
      setConnectionStatus('error')
      setError(err instanceof Error ? err.message : 'Failed to load logs')
      return
    }

    connectWebSocket()
  }, [connectWebSocket, loadHistory])

  useEffect(() => {
    let cancelled = false

    async function bootstrap() {
      const reachable = await checkServerReachable()
      if (cancelled) return

      if (!reachable) {
        setConnectionStatus('error')
        setError(
          'Cannot reach localhost:8080. Before using the dashboard:\n' +
            '1. Run the demo app on your emulator/device\n' +
            '2. Run: adb forward tcp:8080 tcp:8080',
        )
        return
      }

      try {
        await loadHistory()
      } catch (err) {
        if (!cancelled) {
          setConnectionStatus('error')
          setError(err instanceof Error ? err.message : 'Failed to load logs')
        }
        return
      }

      if (!cancelled) connectWebSocket()
    }

    bootstrap()

    return () => {
      cancelled = true
      intentionalCloseRef.current = true
      if (retryTimerRef.current !== null) window.clearTimeout(retryTimerRef.current)
      wsRef.current?.close()
      wsRef.current = null
    }
  }, [connectWebSocket, loadHistory])

  return {
    logs,
    selectedLog,
    selectLog,
    connectionStatus,
    error,
    retry,
  }
}
