package com.example.opensonggoogletvviewer.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.opensonggoogletvviewer.data.OpenSongRepository
import com.example.opensonggoogletvviewer.data.UiSettingsStore
import com.example.opensonggoogletvviewer.model.AppLanguage
import com.example.opensonggoogletvviewer.model.SlideRefreshRate
import com.example.opensonggoogletvviewer.network.OpenSongDiscovery
import com.example.opensonggoogletvviewer.network.OpenSongHttpClient
import com.example.opensonggoogletvviewer.network.OpenSongWsClient
import com.example.opensonggoogletvviewer.ui.tv.SlideColorScheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

sealed class AppState {
    data object Discovering : AppState()
    data class PickServer(
        val results: List<OpenSongDiscovery.Found>,
        val selectedRowIndex: Int,
    ) : AppState()
    data class Running(val host: String, val port: Int) : AppState()
    data class DiscoveryError(val message: String) : AppState()
}

class AppViewModel(
    private val okHttp: OkHttpClient,
    private val settingsStore: UiSettingsStore,
    private val port: Int = 8082,
) : ViewModel() {

    private companion object {
        const val LANGUAGE_ROW = 0
        const val COLOR_SCHEME_ROW = 1
        const val REFRESH_RATE_ROW = 2
        const val REFRESH_ROW = 3
        const val FIRST_SERVER_ROW = 4
        const val MIN_FONT_SCALE = 0.6f
        const val MAX_FONT_SCALE = 2.2f
        const val FONT_SCALE_STEP = 0.1f
    }

    private val _state = MutableStateFlow<AppState>(
        AppState.PickServer(results = emptyList(), selectedRowIndex = REFRESH_ROW)
    )
    val state: StateFlow<AppState> = _state

    private val _colorScheme = MutableStateFlow(settingsStore.loadColorScheme())
    val colorScheme = _colorScheme.asStateFlow()

    private val _language = MutableStateFlow(settingsStore.loadLanguage())
    val language = _language.asStateFlow()

    private val _fontScale = MutableStateFlow(settingsStore.loadFontScale())
    val fontScale = _fontScale.asStateFlow()

    private val _refreshRate = MutableStateFlow(settingsStore.loadRefreshRate())
    val refreshRate = _refreshRate.asStateFlow()

    private var scanJob: Job? = null
    private var lastResults: List<OpenSongDiscovery.Found> = emptyList()

    private var slideVm: SlideViewModel? = null
    fun slideViewModelOrNull(): SlideViewModel? = slideVm

    fun showMainMenu() {
        cancelDiscovery()
        slideVm?.stop()
        slideVm = null
        _state.value = AppState.PickServer(
            results = lastResults,
            selectedRowIndex = defaultSelectedRow(lastResults)
        )
    }

    fun startDiscovery() {
        Log.d("AppViewModel", "startDiscovery() called")
        slideVm?.stop()
        slideVm = null

        scanJob?.cancel()
        _state.value = AppState.Discovering

        scanJob = viewModelScope.launch {
            try {
                val found = OpenSongDiscovery.discoverOnLocal24(
                    okHttp = okHttp,
                    ports = listOf(port, 8080).distinct(),
                    concurrency = 200
                )
                val ips = found
                lastResults = ips
                _state.value = AppState.PickServer(
                    results = ips,
                    selectedRowIndex = defaultSelectedRow(ips)
                )
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                _state.value = AppState.DiscoveryError(t.message ?: "Discovery failed")
            }
        }
    }

    fun cancelDiscovery() {
        scanJob?.cancel()
        scanJob = null
    }

    fun moveSelection(delta: Int) {
        val s = _state.value
        if (s !is AppState.PickServer) return

        val maxIndex = REFRESH_ROW + s.results.size
        val newIndex = (s.selectedRowIndex + delta).coerceIn(0, maxIndex)
        _state.value = s.copy(selectedRowIndex = newIndex)
    }

    fun chooseSelected() {
        val s = _state.value
        if (s !is AppState.PickServer) return

        when (s.selectedRowIndex) {
            LANGUAGE_ROW -> toggleLanguage()
            COLOR_SCHEME_ROW -> toggleColorScheme()
            REFRESH_RATE_ROW -> toggleRefreshRate()
            REFRESH_ROW -> startDiscovery()
            else -> {
                val server = s.results.getOrNull(s.selectedRowIndex - FIRST_SERVER_ROW) ?: return
                startRunning(server)
            }
        }
    }

    fun toggleColorScheme() {
        val next = when (_colorScheme.value) {
            SlideColorScheme.Dark -> SlideColorScheme.Light
            SlideColorScheme.Light -> SlideColorScheme.Dark
        }

        _colorScheme.value = next
        settingsStore.saveColorScheme(next)
    }

    fun toggleLanguage() {
        val next = _language.value.next()
        _language.value = next
        settingsStore.saveLanguage(next)
    }

    fun toggleRefreshRate() {
        val next = _refreshRate.value.next()
        _refreshRate.value = next
        settingsStore.saveRefreshRate(next)
    }

    fun increaseFontScale() {
        updateFontScale(FONT_SCALE_STEP)
    }

    fun decreaseFontScale() {
        updateFontScale(-FONT_SCALE_STEP)
    }

    private fun updateFontScale(delta: Float) {
        val next = (_fontScale.value + delta).coerceIn(MIN_FONT_SCALE, MAX_FONT_SCALE)
        _fontScale.value = next
        settingsStore.saveFontScale(next)
    }

    private fun startRunning(server: OpenSongDiscovery.Found) {
        val http = OpenSongHttpClient(okHttp, server.ip, server.port)
        val ws = OpenSongWsClient(okHttp, server.ip, server.port)

        val repo = OpenSongRepository(
            http = http,
            ws = ws,
            refreshIntervalMs = _refreshRate.value.intervalMs,
            scope = viewModelScope
        )

        slideVm = SlideViewModel(repo).also { it.start() }
        _state.value = AppState.Running(server.ip, server.port)
    }

    private fun defaultSelectedRow(results: List<OpenSongDiscovery.Found>): Int {
        return if (results.isEmpty()) REFRESH_ROW else FIRST_SERVER_ROW
    }

    override fun onCleared() {
        scanJob?.cancel()
        slideVm?.stop()
        super.onCleared()
    }
}
