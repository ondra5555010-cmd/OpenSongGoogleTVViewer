package com.example.opensonggoogletvviewer

import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import com.example.opensonggoogletvviewer.ui.AppRootScreen
import com.example.opensonggoogletvviewer.ui.theme.OpenSongGoogleTVViewerTheme
import com.example.opensonggoogletvviewer.viewmodel.AppViewModel
import okhttp3.OkHttpClient

class MainActivity : ComponentActivity() {

    private val port = 8082
    private lateinit var appVm: AppViewModel

    private var lastBackPressMs = 0L
    private val doubleTapWindowMs = 350L

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

        appVm = AppViewModel(okHttp = okHttpClient, port = port)

        Log.e("MainActivity", "Calling appVm.startDiscovery()")
        appVm.startDiscovery()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val now = SystemClock.uptimeMillis()

                if (now - lastBackPressMs <= doubleTapWindowMs) {
                    lastBackPressMs = 0L
                    Log.e("MainActivity", "Double back detected, restarting discovery")
                    appVm.startDiscovery()
                    return
                }

                lastBackPressMs = now
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