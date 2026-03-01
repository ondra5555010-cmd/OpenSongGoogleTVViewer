package com.example.opensonggoogletvviewer.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.*
import com.example.opensonggoogletvviewer.model.ConnectionState
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

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
        ) {
            Text(
                text = when (conn) {
                    is ConnectionState.Connecting -> "Connecting…"
                    is ConnectionState.Connected -> "Connected"
                    is ConnectionState.Error -> "Error: ${(conn as ConnectionState.Error).message}"
                },
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = slide.title ?: "",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = slide.body ?: "",
                style = MaterialTheme.typography.headlineLarge
            )
        }
    }
}