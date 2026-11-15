package com.universalremote.tv.controller

import com.universalremote.tv.data.CredentialStore
import com.universalremote.tv.model.DiscoveredTV
import com.universalremote.tv.model.TVApp
import com.universalremote.tv.model.TVKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

abstract class BaseTVController(val tv: DiscoveredTV, protected val credentialStore: CredentialStore) {
    @Volatile protected var connected = false
    protected val normalClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS).readTimeout(10, TimeUnit.SECONDS).writeTimeout(5, TimeUnit.SECONDS).build()
    protected val insecureClient: OkHttpClient by lazy { trustAllClient() }

    abstract suspend fun connect(): Result<Unit>
    abstract suspend fun pair(pin: String? = null): Result<Unit>
    abstract suspend fun disconnect()
    abstract suspend fun sendKey(key: TVKey): Result<Unit>
    abstract suspend fun launchApp(appId: String): Result<Unit>
    abstract suspend fun getAppList(): Result<List<TVApp>>
    abstract suspend fun powerOff(): Result<Unit>
    abstract suspend fun volumeUp(): Result<Unit>
    abstract suspend fun volumeDown(): Result<Unit>
    abstract suspend fun setMute(mute: Boolean): Result<Unit>
    abstract suspend fun channelUp(): Result<Unit>
    abstract suspend fun channelDown(): Result<Unit>
    abstract fun isConnected(): Boolean

    protected suspend fun http(
        method: String,
        url: String,
        body: String? = null,
        headers: Map<String, String> = emptyMap(),
        insecure: Boolean = false,
        contentType: String = "application/json; charset=utf-8"
    ): HttpResult = withContext(Dispatchers.IO) {
        val builder = Request.Builder().url(url)
        headers.forEach { (k, v) -> builder.header(k, v) }
        val requestBody = body?.toRequestBody(contentType.toMediaType())
        when (method.uppercase()) {
            "POST" -> builder.post(requestBody ?: ByteArray(0).toRequestBody(null))
            "PUT" -> builder.put(requestBody ?: ByteArray(0).toRequestBody(null))
            "DELETE" -> builder.delete(requestBody)
            else -> builder.get()
        }
        val client = if (insecure) insecureClient else normalClient
        client.newCall(builder.build()).execute().use { response: Response ->
            HttpResult(response.code, response.body?.string().orEmpty(), response.headers.toMultimap())
        }
    }

    protected suspend fun openWebSocket(url: String, insecure: Boolean = false, onText: (String) -> Unit): WebSocket =
        withContext(Dispatchers.IO) {
            suspendCancellableCoroutine { continuation ->
                val request = Request.Builder().url(url).build()
                val client = (if (insecure) insecureClient else normalClient).newBuilder().readTimeout(0, TimeUnit.MILLISECONDS).pingInterval(20, TimeUnit.SECONDS).build()
                val socket = client.newWebSocket(request, object : WebSocketListener() {
                    override fun onOpen(webSocket: WebSocket, response: Response) {
                        if (continuation.isActive) continuation.resume(webSocket)
                    }
                    override fun onMessage(webSocket: WebSocket, text: String) = onText(text)
                    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) { webSocket.close(code, reason) }
                    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) { connected = false }
                    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                        connected = false
                        if (continuation.isActive) continuation.resumeWithException(t)
                    }
                })
                continuation.invokeOnCancellation { socket.cancel() }
            }
        }

    protected fun ok(): Result<Unit> = Result.success(Unit)
    protected fun failed(message: String): Result<Unit> = Result.failure(IllegalStateException(message))
    protected fun <T> failedValue(message: String): Result<T> = Result.failure(IllegalStateException(message))
    protected fun requireConnected(): Result<Unit> = if (connected) ok() else failed("Not connected to ${tv.friendlyName}")

    protected fun trustAllClient(): OkHttpClient {
        val trustManager = object : X509TrustManager {
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
        }
        val context = SSLContext.getInstance("TLS").apply { init(null, arrayOf<TrustManager>(trustManager), SecureRandom()) }
        return OkHttpClient.Builder().sslSocketFactory(context.socketFactory, trustManager)
            .hostnameVerifier { _, _ -> true }
            .connectTimeout(5, TimeUnit.SECONDS).readTimeout(10, TimeUnit.SECONDS).writeTimeout(5, TimeUnit.SECONDS).build()
    }

    protected fun safeMessage(error: Throwable): String = error.message?.takeIf { it.isNotBlank() } ?: "Could not communicate with this TV"
}

data class HttpResult(val code: Int, val body: String, val headers: Map<String, List<String>>) {
    val successful: Boolean get() = code in 200..299
}
