package com.example.network_logger_lib.server

import com.example.network_logger_lib.data.NetworkLogEntity
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Keeps track of active WebSocket clients and pushes new logs to all of them.
 * A simple in-memory pub/sub – no message queue needed for a demo project.
 */
object LogBroadcaster {

    private val json = Json { encodeDefaults = true }
    private val sessions = mutableSetOf<WebSocketSession>()
    private val mutex = Mutex()

    suspend fun register(session: WebSocketSession) {
        mutex.withLock { sessions.add(session) }
    }

    suspend fun unregister(session: WebSocketSession) {
        mutex.withLock { sessions.remove(session) }
    }

    /** Serializes [log] to JSON and sends it to every connected dashboard client. */
    suspend fun broadcast(log: NetworkLogEntity) {
        val payload = json.encodeToString(log)
        val staleSessions = mutableListOf<WebSocketSession>()

        mutex.withLock {
            sessions.forEach { session ->
                try {
                    session.send(Frame.Text(payload))
                } catch (_: Exception) {
                    staleSessions.add(session)
                }
            }
            sessions.removeAll(staleSessions)
        }
    }
}
