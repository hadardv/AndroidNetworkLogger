package com.example.network_logger_lib

import android.content.Context
import com.example.network_logger_lib.data.NetworkLogDatabase
import com.example.network_logger_lib.interceptor.WebNetworkLoggerInterceptor
import com.example.network_logger_lib.server.NetworkLoggerServer
import okhttp3.Interceptor

/**
 * Single entry point for host apps.
 *
 * Usage:
 * ```
 * // In Application.onCreate()
 * NetworkLogger.init(this)
 *
 * // When building OkHttp
 * OkHttpClient.Builder()
 *     .addInterceptor(NetworkLogger.interceptor())
 *     .build()
 * ```
 */
object NetworkLogger {

    private var interceptor: WebNetworkLoggerInterceptor? = null

    /**
     * Initializes Room, starts the embedded Ktor server, and prepares the interceptor.
     * Call once from [android.app.Application.onCreate].
     */
    fun init(context: Context, port: Int = NetworkLoggerServer.DEFAULT_PORT) {
        if (interceptor != null) return

        val dao = NetworkLogDatabase.getInstance(context).networkLogDao()
        interceptor = WebNetworkLoggerInterceptor(dao)
        NetworkLoggerServer(dao, port).start()
    }

    /** Returns the OkHttp interceptor that captures network traffic. */
    fun interceptor(): Interceptor {
        return requireNotNull(interceptor) {
            "NetworkLogger.init() must be called before accessing the interceptor"
        }
    }
}
