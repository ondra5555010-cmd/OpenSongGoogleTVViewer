package com.example.opensonggoogletvviewer.network

import okhttp3.*
import okio.ByteString
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class OpenSongWsClient(
    private val client: OkHttpClient,
    private val host: String,
    private val port: Int = 8082
) {
    private var ws: WebSocket? = null
    private val connected = AtomicBoolean(false)

    @Volatile private var shouldStayConnected = false
    private val scheduler = Executors.newSingleThreadScheduledExecutor()
    @Volatile private var reconnectTask: ScheduledFuture<*>? = null

    @Synchronized
    fun connect(
        onPresentationEvent: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        shouldStayConnected = true
        if (connected.get()) return

        // If an old socket exists, close it before creating a new one.
        ws?.close(1000, "reconnect")
        ws = null

        val request = Request.Builder()
            .url("ws://$host:$port/ws")
            .addHeader(
                "User-Agent",
                "Mozilla/5.0 (Android TV; Linux; Android 12) AppleWebKit/537.36 Chrome/120 Safari/537.36"
            )
            .build()

        ws = client.newWebSocket(request, object : WebSocketListener() {

            override fun onOpen(webSocket: WebSocket, response: Response) {
                connected.set(true)
                cancelReconnect()

                // OpenSong subscribe
                webSocket.send("/ws/subscribe/presentation")

                // Refresh immediately after subscribing
                onPresentationEvent()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                onPresentationEvent()
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                onPresentationEvent()
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                connected.set(false)
                webSocket.close(code, reason)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                connected.set(false)
                scheduleReconnect(onPresentationEvent, onError)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                connected.set(false)
                onError(t)
                scheduleReconnect(onPresentationEvent, onError)
            }
        })
    }

    @Synchronized
    private fun scheduleReconnect(
        onPresentationEvent: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        if (!shouldStayConnected) return
        if (reconnectTask?.isDone == false) return

        reconnectTask = scheduler.schedule({
            if (!shouldStayConnected) return@schedule
            connected.set(false)
            connect(onPresentationEvent, onError)
        }, 2, TimeUnit.SECONDS)
    }

    @Synchronized
    private fun cancelReconnect() {
        reconnectTask?.cancel(false)
        reconnectTask = null
    }

    /**
     * Intentional close (background / user leaves). Disables auto-reconnect.
     */
    @Synchronized
    fun close() {
        shouldStayConnected = false
        cancelReconnect()
        ws?.close(1000, "closing")
        ws = null
        connected.set(false)
    }

    fun isConnected(): Boolean = connected.get()
}