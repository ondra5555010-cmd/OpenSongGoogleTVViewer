package com.example.opensonggoogletvviewer

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import com.example.opensonggoogletvviewer.data.OpenSongRepository
import com.example.opensonggoogletvviewer.network.OpenSongHttpClient
import com.example.opensonggoogletvviewer.network.OpenSongWsClient
import com.example.opensonggoogletvviewer.ui.SlideScreen
import com.example.opensonggoogletvviewer.ui.theme.OpenSongGoogleTVViewerTheme
import com.example.opensonggoogletvviewer.viewmodel.SlideViewModel
import okhttp3.OkHttpClient

class MainActivity : ComponentActivity() {

    // TODO: move to settings later
    private val presetIp = "192.168.0.153"
    private val port = 8082

    @RequiresApi(Build.VERSION_CODES.O)
    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Shared HTTP client
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(java.time.Duration.ofMillis(800))
            .readTimeout(java.time.Duration.ofSeconds(2))
            .callTimeout(java.time.Duration.ofSeconds(2))
            .pingInterval(java.time.Duration.ofSeconds(15)) // keeps WS alive
            .retryOnConnectionFailure(true)
            .build()

        // Network layer
        val httpClient = OpenSongHttpClient(okHttpClient, presetIp, port)
        val wsClient = OpenSongWsClient(okHttpClient, presetIp, port)

        // Repository
        val repository = OpenSongRepository(
            http = httpClient,
            ws = wsClient,
            scope = lifecycleScope
        )

        setContent {
            OpenSongGoogleTVViewerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val vm = SlideViewModel(repository)
                    SlideScreen(vm)
                }
            }
        }
    }
}