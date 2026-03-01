package com.example.opensonggoogletvviewer.network

import okhttp3.*
import okio.ByteString
import java.util.concurrent.atomic.AtomicBoolean

class OpenSongWsClient(
    private val client: OkHttpClient,
    private val host: String,
    private val port: Int = 8082
) {
    private var ws: WebSocket? = null
    private val connected = AtomicBoolean(false)

    @Volatile private var reconnecting = false
    @Volatile private var shouldStayConnected = false

    fun connect(
        onPresentationEvent: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        shouldStayConnected = true
        if (connected.get()) return

        reconnecting = false

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
                reconnecting = false

                webSocket.send("/ws/subscribe/presentation")
                onPresentationEvent() // refresh on connect
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                onPresentationEvent()
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                onPresentationEvent()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                connected.set(false)
                onError(t)
                scheduleReconnect(onPresentationEvent, onError)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                connected.set(false)
                scheduleReconnect(onPresentationEvent, onError)
            }
        })
    }

    private fun scheduleReconnect(
        onPresentationEvent: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        if (!shouldStayConnected) return
        if (reconnecting) return
        reconnecting = true

        Thread {
            try {
                Thread.sleep(2000)
            } catch (_: InterruptedException) {
                return@Thread
            }

            if (!shouldStayConnected) {
                reconnecting = false
                return@Thread
            }

            connected.set(false)
            connect(onPresentationEvent, onError)
        }.start()
    }

    /**
     * Intentional close (background / user leaves). Disables auto-reconnect.
     */
    fun close() {
        shouldStayConnected = false
        reconnecting = false
        ws?.close(1000, "closing")
        ws = null
        connected.set(false)
    }

    fun isConnected(): Boolean = connected.get()
}