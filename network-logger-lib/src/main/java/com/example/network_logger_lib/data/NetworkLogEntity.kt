package com.example.network_logger_lib.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Core data model for a single captured HTTP exchange.
 * Used both as a Room entity (persistence) and as the JSON payload
 * sent to the web dashboard via REST / WebSocket.
 */
@Serializable
@Entity(tableName = "network_logs")
data class NetworkLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val url: String,
    val method: String,
    val statusCode: Int,
    val requestHeaders: String,
    val responseHeaders: String,
    val requestBody: String,
    val responseBody: String,
    val timestamp: Long,
    val durationMs: Long,
    val isSuccess: Boolean,
)
