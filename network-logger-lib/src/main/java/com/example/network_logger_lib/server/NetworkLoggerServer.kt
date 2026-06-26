package com.example.network_logger_lib.server

import com.example.network_logger_lib.data.NetworkLogDao
import com.example.network_logger_lib.data.NetworkLogEntity
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.close
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.concurrent.thread

/**
 * Embedded Ktor server that exposes:
 *  - GET  /api/logs  – historical logs from Room
 *  - WS   /ws/logs   – real-time stream of new logs
 *
 * Runs on a dedicated background thread so it never blocks the UI.
 */
class NetworkLoggerServer(
    private val dao: NetworkLogDao,
    private val port: Int = DEFAULT_PORT,
) {

    fun start() {
        thread(name = "NetworkLoggerServer", isDaemon = true) {
            embeddedServer(
                factory = io.ktor.server.cio.CIO,
                host = "0.0.0.0",
                port = port,
                module = { configure(dao) },
            ).start(wait = true)
        }
    }

    companion object {
        const val DEFAULT_PORT = 8080

        private fun Application.configure(dao: NetworkLogDao) {
            install(ContentNegotiation) {
                json(Json { encodeDefaults = true })
            }
            install(WebSockets)
            install(CORS) {
                anyHost()
                allowMethod(HttpMethod.Get)
                allowMethod(HttpMethod.Options)
                allowHeader(HttpHeaders.ContentType)
            }

            routing {
                // REST endpoint – used by the dashboard on initial load
                get("/api/logs") {
                    val logs = dao.getAllOrderedByTimestamp()
                    call.respond(logs)
                }

                // WebSocket endpoint – pushes each new log in real time
                webSocket("/ws/logs") {
                    LogBroadcaster.register(this)
                    try {
                        // Keep the connection alive; the server pushes data out
                        for (frame in incoming) {
                            if (frame is io.ktor.websocket.Frame.Close) break
                        }
                    } finally {
                        LogBroadcaster.unregister(this)
                        close()
                    }
                }
            }
        }

        /** Called by the interceptor after a log is saved to Room. */
        fun notifyNewLog(log: NetworkLogEntity) {
            runBlocking { LogBroadcaster.broadcast(log) }
        }
    }
}
