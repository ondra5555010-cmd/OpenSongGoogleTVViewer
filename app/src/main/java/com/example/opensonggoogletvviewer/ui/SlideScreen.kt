package com.example.opensonggoogletvviewer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.example.opensonggoogletvviewer.model.AppStrings
import com.example.opensonggoogletvviewer.model.ConnectionState
import com.example.opensonggoogletvviewer.ui.tv.SlideColorScheme
import com.example.opensonggoogletvviewer.ui.tv.handleDpad
import com.example.opensonggoogletvviewer.ui.tv.scaled
import com.example.opensonggoogletvviewer.viewmodel.SlideViewModel

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SlideScreen(
    vm: SlideViewModel,
    colorScheme: SlideColorScheme,
    strings: AppStrings,
    fontScale: Float,
    onIncreaseFontScale: () -> Unit,
    onDecreaseFontScale: () -> Unit,
    onSettings: () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val view = LocalView.current

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

    DisposableEffect(view) {
        val previousKeepScreenOn = view.keepScreenOn
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = previousKeepScreenOn }
    }

    val slide by vm.slide.collectAsStateWithLifecycle()
    val conn by vm.connection.collectAsStateWithLifecycle()
    val body = slide.body.orEmpty()

    val backgroundColor = when (colorScheme) {
        SlideColorScheme.Dark -> Color(0xFF000000)
        SlideColorScheme.Light -> Color(0xFFFAFAF5)
    }
    val textColor = when (colorScheme) {
        SlideColorScheme.Dark -> Color.White
        SlideColorScheme.Light -> Color(0xFF111111)
    }
    val mutedTextColor = textColor.copy(alpha = 0.58f)

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .handleDpad(
                onUp = onIncreaseFontScale,
                onDown = onDecreaseFontScale,
                onLeft = { vm.previousSlide() },
                onRight = { vm.nextSlide() }
            )
            .focusable()
    ) {
        CompositionLocalProvider(LocalContentColor provides textColor) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor)
                    .padding(horizontal = 48.dp, vertical = 24.dp)
            ) {
                ConnectionStatus(
                    modifier = Modifier.align(Alignment.TopStart),
                    state = conn,
                    strings = strings,
                    color = mutedTextColor
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .align(Alignment.Center),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = body,
                        color = textColor,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.displaySmall.scaled(fontScale)
                    )
                }
            }
        }
    }
}

@Composable
private fun ConnectionStatus(
    modifier: Modifier = Modifier,
    state: ConnectionState,
    strings: AppStrings,
    color: Color,
) {
    when (state) {
        is ConnectionState.Connected -> Unit
        is ConnectionState.Connecting -> Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = color,
                strokeWidth = 2.dp
            )
            Text(
                text = strings.connecting,
                color = color,
                style = MaterialTheme.typography.bodyLarge
            )
        }
        is ConnectionState.Idle -> Text(
            modifier = modifier,
            text = strings.noPresentationRunning,
            color = color,
            style = MaterialTheme.typography.bodyLarge
        )
        is ConnectionState.Error -> Text(
            modifier = modifier,
            text = "${strings.error}: ${state.message}",
            color = color,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
