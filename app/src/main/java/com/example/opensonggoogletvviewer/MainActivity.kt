package com.example.opensonggoogletvviewer

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import com.example.opensonggoogletvviewer.data.UiSettingsStore
import com.example.opensonggoogletvviewer.ui.AppRootScreen
import com.example.opensonggoogletvviewer.ui.theme.OpenSongGoogleTVViewerTheme
import com.example.opensonggoogletvviewer.viewmodel.AppState
import com.example.opensonggoogletvviewer.viewmodel.AppViewModel
import okhttp3.OkHttpClient

class MainActivity : ComponentActivity() {

    private val port = 8082
    private lateinit var appVm: AppViewModel

    @RequiresApi(Build.VERSION_CODES.O)
    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.e("MainActivity", "onCreate() reached")

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(java.time.Duration.ofMillis(800))
            .readTimeout(java.time.Duration.ofSeconds(2))
            .callTimeout(java.time.Duration.ofSeconds(2))
            .pingInterval(java.time.Duration.ofSeconds(15))
            .retryOnConnectionFailure(true)
            .build()

        val settingsStore = UiSettingsStore(
            getSharedPreferences("opensong_viewer_prefs", Context.MODE_PRIVATE)
        )

        appVm = AppViewModel(
            okHttp = okHttpClient,
            settingsStore = settingsStore,
            port = port
        )

        Log.e("MainActivity", "Calling appVm.startDiscovery()")
        appVm.startDiscovery()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when (appVm.state.value) {
                    is AppState.Running,
                    is AppState.PickServer,
                    is AppState.DiscoveryError -> {
                        Log.e("MainActivity", "Back pressed, restarting discovery")
                        appVm.startDiscovery()
                    }
                    is AppState.Discovering -> {
                        // ignore
                    }
                }
            }
        })

        setContent {
            OpenSongGoogleTVViewerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppRootScreen(appVm)
                }
            }
        }
    }
}