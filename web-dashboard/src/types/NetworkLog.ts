/** Mirrors the Kotlin NetworkLogEntity serialized by the embedded Ktor server. */
export interface NetworkLog {
  id: number
  url: string
  method: string
  statusCode: number
  requestHeaders: string
  responseHeaders: string
  requestBody: string
  responseBody: string
  timestamp: number
  durationMs: number
  isSuccess: boolean
}

export type ConnectionStatus = 'connecting' | 'connected' | 'disconnected' | 'error'
