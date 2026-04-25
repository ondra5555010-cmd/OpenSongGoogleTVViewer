package com.example.opensonggoogletvviewer.data

import com.example.opensonggoogletvviewer.model.ConnectionState
import com.example.opensonggoogletvviewer.model.CurrentSlide
import com.example.opensonggoogletvviewer.network.NoPresentationRunningException
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
    @Volatile private var refreshRequested: Boolean = false

    fun start() {
        _connection.value = ConnectionState.Connecting

        ws.connect(
            onPresentationEvent = { refresh() },
            onError = {
                refresh()
            }
        )

        refresh()
    }

    fun stop() {
        ws.close()
    }

    fun refresh() {
        refreshRequested = true

        if (!refreshing.compareAndSet(false, true)) return

        scope.launch {
            try {
                while (refreshRequested) {
                    refreshRequested = false
                    performRefreshOnce()
                }
            } finally {
                refreshing.set(false)
            }
        }
    }

    fun nextSlide() {
        scope.launch {
            try {
                http.nextSlide()
                refresh()
            } catch (_: Throwable) {
                refresh()
            }
        }
    }

    fun previousSlide() {
        scope.launch {
            try {
                http.previousSlide()
                refresh()
            } catch (_: Throwable) {
                refresh()
            }
        }
    }

    private suspend fun performRefreshOnce() {
        try {
            val xml = http.getCurrentSlideXml()
            val parsed = OpenSongSlideParser.parseCurrentSlide(xml)

            val fingerprint = xml.hashCode().toString()
            if (fingerprint != lastFingerprint) {
                lastFingerprint = fingerprint
                _slide.value = parsed
            }

            _connection.value = ConnectionState.Connected
        } catch (e: NoPresentationRunningException) {
            lastFingerprint = null
            _slide.value = CurrentSlide()
            _connection.value = ConnectionState.Idle
        } catch (t: Throwable) {
            _connection.value = ConnectionState.Error("HTTP: ${t.toUserMessage()}")
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