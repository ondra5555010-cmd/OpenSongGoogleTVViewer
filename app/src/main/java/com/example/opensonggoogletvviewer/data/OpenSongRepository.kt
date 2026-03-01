package com.example.opensonggoogletvviewer.data

import com.example.opensonggoogletvviewer.model.ConnectionState
import com.example.opensonggoogletvviewer.model.CurrentSlide
import com.example.opensonggoogletvviewer.network.OpenSongHttpClient
import com.example.opensonggoogletvviewer.network.OpenSongWsClient
import com.example.opensonggoogletvviewer.parser.OpenSongSlideParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.atomic.AtomicBoolean

class OpenSongRepository(
    private val http: OpenSongHttpClient,
    private val ws: OpenSongWsClient,
    private val scope: CoroutineScope
) {
    private val _slide = MutableStateFlow(CurrentSlide())
    val slide: StateFlow<CurrentSlide> = _slide

    private val _connection = MutableStateFlow<ConnectionState>(ConnectionState.Connecting)
    val connection: StateFlow<ConnectionState> = _connection

    private var lastFingerprint: String? = null
    private val refreshing = AtomicBoolean(false)

    fun start() {
        _connection.value = ConnectionState.Connecting

        // initial load
        refresh()

        // realtime triggers
        ws.connect(
            onPresentationEvent = { refresh() },
            onError = { t ->
                _connection.value = ConnectionState.Error("WebSocket: ${t.toUserMessage()}")
            }
        )
    }

    fun stop() {
        // Intentional disconnect
        ws.close()
    }

    fun refresh() {
        // latest-wins: ignore if one refresh is already running
        if (!refreshing.compareAndSet(false, true)) return

        scope.launch {
            try {
                val xml = http.getCurrentSlideXml()
                val parsed = OpenSongSlideParser.parseCurrentSlide(xml)

                val fingerprint = "${parsed.title.orEmpty()}|${parsed.body.orEmpty()}"
                if (fingerprint != lastFingerprint) {
                    lastFingerprint = fingerprint
                    _slide.value = parsed
                }
                _connection.value = ConnectionState.Connected
            } catch (t: Throwable) {
                _connection.value = ConnectionState.Error("HTTP: ${t.toUserMessage()}")
            } finally {
                refreshing.set(false)
            }
        }
    }
}

private fun Throwable.toUserMessage(): String {
    return when (this) {
        is UnknownHostException -> "Unknown host"
        is ConnectException -> "Connection refused / host unreachable"
        is SocketTimeoutException -> "Timeout"
        else -> (message ?: this::class.java.simpleName)
    }
}