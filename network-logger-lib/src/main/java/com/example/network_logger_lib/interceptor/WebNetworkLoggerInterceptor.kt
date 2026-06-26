package com.example.network_logger_lib.interceptor

import com.example.network_logger_lib.data.NetworkLogDao
import com.example.network_logger_lib.data.NetworkLogEntity
import com.example.network_logger_lib.server.NetworkLoggerServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.Buffer
import org.json.JSONObject
import java.io.IOException

/**
 * OkHttp [Interceptor] that captures every HTTP exchange, persists it to Room,
 * and broadcasts it to connected WebSocket clients – all on a background thread.
 */
class WebNetworkLoggerInterceptor(
    private val dao: NetworkLogDao,
) : Interceptor {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val requestBodyString = readRequestBody(originalRequest)
        // Rebuild the request so the body can still be sent after we read it
        val request = originalRequest.newBuilder()
            .method(
                originalRequest.method,
                requestBodyString?.toRequestBody(originalRequest.body?.contentType()),
            )
            .build()

        val startTime = System.currentTimeMillis()

        return try {
            val response = chain.proceed(request)
            val durationMs = System.currentTimeMillis() - startTime
            val responseBodyString = peekResponseBody(response)

            persistAndBroadcast(
                request = request,
                response = response,
                requestBody = requestBodyString.orEmpty(),
                responseBody = responseBodyString,
                durationMs = durationMs,
                isSuccess = response.isSuccessful,
            )
            response
        } catch (e: IOException) {
            val durationMs = System.currentTimeMillis() - startTime
            persistAndBroadcast(
                request = request,
                statusCode = -1,
                responseHeaders = "",
                requestBody = requestBodyString.orEmpty(),
                responseBody = e.message.orEmpty(),
                durationMs = durationMs,
                isSuccess = false,
            )
            throw e
        }
    }

    private fun readRequestBody(request: Request): String? {
        val body = request.body ?: return null
        val buffer = Buffer()
        body.writeTo(buffer)
        return buffer.readUtf8()
    }

    private fun peekResponseBody(response: Response): String {
        return try {
            response.peekBody(MAX_BODY_BYTES).string()
        } catch (_: Exception) {
            ""
        }
    }

    private fun persistAndBroadcast(
        request: Request,
        response: Response,
        requestBody: String,
        responseBody: String,
        durationMs: Long,
        isSuccess: Boolean,
    ) {
        persistAndBroadcast(
            request = request,
            statusCode = response.code,
            responseHeaders = headersToJson(response.headers),
            requestBody = requestBody,
            responseBody = responseBody,
            durationMs = durationMs,
            isSuccess = isSuccess,
        )
    }

    private fun persistAndBroadcast(
        request: Request,
        statusCode: Int,
        responseHeaders: String,
        requestBody: String,
        responseBody: String,
        durationMs: Long,
        isSuccess: Boolean,
    ) {
        val entity = NetworkLogEntity(
            url = request.url.toString(),
            method = request.method,
            statusCode = statusCode,
            requestHeaders = headersToJson(request.headers),
            responseHeaders = responseHeaders,
            requestBody = requestBody,
            responseBody = responseBody,
            timestamp = System.currentTimeMillis(),
            durationMs = durationMs,
            isSuccess = isSuccess,
        )

        scope.launch {
            val id = dao.insert(entity)
            NetworkLoggerServer.notifyNewLog(entity.copy(id = id))
        }
    }

    private fun headersToJson(headers: Headers): String {
        val json = JSONObject()
        for (i in 0 until headers.size) {
            json.put(headers.name(i), headers.value(i))
        }
        return json.toString()
    }

    companion object {
        private const val MAX_BODY_BYTES = 1024L * 1024L
    }
}
