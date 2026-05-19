package com.example.opensonggoogletvviewer.data

import com.example.opensonggoogletvviewer.model.ConnectionState
import com.example.opensonggoogletvviewer.model.CurrentSlide
import com.example.opensonggoogletvviewer.network.NoPresentationRunningException
import com.example.opensonggoogletvviewer.network.OpenSongHttpClient
import com.example.opensonggoogletvviewer.network.OpenSongWsClient
import com.example.opensonggoogletvviewer.parser.OpenSongSlideParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.atomic.AtomicBoolean

class OpenSongRepository(
    private val http: OpenSongHttpClient,
    private val ws: OpenSongWsClient,
    private val refreshIntervalMs: Long,
    private val scope: CoroutineScope
) {
    private val _slide = MutableStateFlow(CurrentSlide())
    val slide: StateFlow<CurrentSlide> = _slide

    private val _connection = MutableStateFlow<ConnectionState>(ConnectionState.Connecting)
    val connection: StateFlow<ConnectionState> = _connection

    private var lastFingerprint: String? = null
    private var consecutiveRefreshFailures: Int = 0
    private var pollingJob: Job? = null

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
        startPolling()
    }

    fun stop() {
        pollingJob?.cancel()
        pollingJob = null
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

    private fun acceptSlideXml(xml: String) {
        try {
            val parsed = OpenSongSlideParser.parseCurrentSlide(xml)
            val fingerprint = xml.hashCode().toString()
            if (fingerprint != lastFingerprint) {
                lastFingerprint = fingerprint
                _slide.value = parsed
            }
            consecutiveRefreshFailures = 0
            _connection.value = ConnectionState.Connected
        } catch (_: Throwable) {
            refresh()
        }
    }

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = scope.launch {
            while (isActive) {
                delay(refreshIntervalMs)
                refresh()
            }
        }
    }

    private suspend fun performRefreshOnce() {
        try {
            val xml = http.getCurrentSlideXml()
            acceptSlideXml(xml)
        } catch (e: NoPresentationRunningException) {
            lastFingerprint = null
            consecutiveRefreshFailures = 0
            _slide.value = CurrentSlide()
            _connection.value = ConnectionState.Idle
        } catch (t: Throwable) {
            val hasPreviousSlide =
                !_slide.value.title.isNullOrBlank() || !_slide.value.body.isNullOrBlank()

            consecutiveRefreshFailures++
            if (hasPreviousSlide && consecutiveRefreshFailures < 3) {
                _connection.value = ConnectionState.Connected
            } else {
                _connection.value = ConnectionState.Error("HTTP: ${t.toUserMessage()}")
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
