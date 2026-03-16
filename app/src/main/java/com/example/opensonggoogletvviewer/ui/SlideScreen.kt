package com.example.opensonggoogletvviewer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.example.opensonggoogletvviewer.model.ConnectionState
import com.example.opensonggoogletvviewer.ui.tv.SlideColorScheme
import com.example.opensonggoogletvviewer.ui.tv.handleDpad
import com.example.opensonggoogletvviewer.ui.tv.rememberColorSchemeController
import com.example.opensonggoogletvviewer.ui.tv.rememberFontScaleController
import com.example.opensonggoogletvviewer.ui.tv.scaled
import com.example.opensonggoogletvviewer.viewmodel.SlideViewModel

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SlideScreen(vm: SlideViewModel) {
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> vm.start()
                Lifecycle.Event.ON_STOP -> vm.stop()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    val slide by vm.slide.collectAsStateWithLifecycle()
    val conn by vm.connection.collectAsStateWithLifecycle()

    val font = rememberFontScaleController()
    val scale = font.scale

    val scheme = rememberColorSchemeController()
    val backgroundColor = when (scheme.scheme) {
        SlideColorScheme.Dark -> Color.Black
        SlideColorScheme.Light -> Color.White
    }
    val textColor = when (scheme.scheme) {
        SlideColorScheme.Dark -> Color.White
        SlideColorScheme.Light -> Color.Black
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .handleDpad(
                onUp = { font.increase() },
                onDown = { font.decrease() },
                onLeft = { scheme.previous() },
                onRight = { scheme.next() }
            )
            .focusable()
    ) {
        CompositionLocalProvider(LocalContentColor provides textColor) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp)
                ) {
                    Text(
                        text = when (val state = conn) {
                            is ConnectionState.Connecting -> "Connecting…"
                            is ConnectionState.Connected -> "Connected"
                            is ConnectionState.Idle -> "Connected — no presentation running"
                            is ConnectionState.Error -> "Error: ${state.message}"
                        },
                        style = MaterialTheme.typography.bodyLarge.scaled(scale)
                    )

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = slide.title ?: "",
                        style = MaterialTheme.typography.headlineMedium.scaled(scale)
                    )

                    Spacer(Modifier.height(24.dp))

                    Text(
                        text = slide.body ?: "",
                        style = MaterialTheme.typography.headlineLarge.scaled(scale)
                    )
                }
            }
        }
    }
}