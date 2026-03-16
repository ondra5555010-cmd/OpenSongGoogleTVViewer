package com.example.opensonggoogletvviewer.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.opensonggoogletvviewer.data.OpenSongRepository
import com.example.opensonggoogletvviewer.network.OpenSongDiscovery
import com.example.opensonggoogletvviewer.network.OpenSongHttpClient
import com.example.opensonggoogletvviewer.network.OpenSongWsClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

sealed class AppState {
    data object Discovering : AppState()
    data class PickServer(val results: List<String>, val selectedIndex: Int) : AppState()
    data class Running(val host: String) : AppState()
    data class DiscoveryError(val message: String) : AppState()
}

class AppViewModel(
    private val okHttp: OkHttpClient,
    private val port: Int = 8082,
) : ViewModel() {

    private val _state = MutableStateFlow<AppState>(AppState.Discovering)
    val state: StateFlow<AppState> = _state

    private var scanJob: Job? = null

    // Slide VM is created only after host is chosen
    private var slideVm: SlideViewModel? = null
    fun slideViewModelOrNull(): SlideViewModel? = slideVm

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
                    port = port,
                    concurrency = 200
                )
                val ips = found.map { it.ip }
                _state.value = AppState.PickServer(results = ips, selectedIndex = 0)
            } catch (t: Throwable) {
                _state.value = AppState.DiscoveryError(t.message ?: "Discovery failed")
            }
        }
    }

    fun moveSelection(delta: Int) {
        val s = _state.value
        if (s !is AppState.PickServer) return
        if (s.results.isEmpty()) return

        val newIndex = (s.selectedIndex + delta).coerceIn(0, s.results.lastIndex)
        _state.value = s.copy(selectedIndex = newIndex)
    }

    fun chooseSelected() {
        val s = _state.value
        if (s !is AppState.PickServer) return
        val host = s.results.getOrNull(s.selectedIndex) ?: return
        startRunning(host)
    }

    private fun startRunning(host: String) {
        // build a new repo + slide VM for this host
        val http = OpenSongHttpClient(okHttp, host, port)
        val ws = OpenSongWsClient(okHttp, host, port)

        val repo = OpenSongRepository(
            http = http,
            ws = ws,
            scope = viewModelScope
        )

        slideVm = SlideViewModel(repo).also { it.start() }
        _state.value = AppState.Running(host)
    }

    override fun onCleared() {
        scanJob?.cancel()
        slideVm?.stop()
        super.onCleared()
    }
}