package com.example.opensonggoogletvviewer.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.tv.material3.Text
import com.example.opensonggoogletvviewer.viewmodel.AppState
import com.example.opensonggoogletvviewer.viewmodel.AppViewModel

@Composable
fun AppRootScreen(appVm: AppViewModel) {
    val state by appVm.state.collectAsState()
    val colorScheme by appVm.colorScheme.collectAsState()

    when (val s = state) {
        is AppState.Discovering -> {
            Box(modifier = Modifier.fillMaxSize())
        }

        is AppState.DiscoveryError -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text("Discovery error: ${s.message}")
            }
        }

        is AppState.PickServer -> {
            ServerPickerScreen(
                ips = s.results,
                selectedIndex = s.selectedIndex,
                colorScheme = colorScheme,
                onUp = { appVm.moveSelection(-1) },
                onDown = { appVm.moveSelection(+1) },
                onLeft = { appVm.previousColorScheme() },
                onRight = { appVm.nextColorScheme() },
                onOk = { appVm.chooseSelected() }
            )
        }

        is AppState.Running -> {
            val slideVm = appVm.slideViewModelOrNull()
            if (slideVm != null) {
                SlideScreen(
                    vm = slideVm,
                    colorScheme = colorScheme,
                    onLeft = { appVm.previousColorScheme() },
                    onRight = { appVm.nextColorScheme() }
                )
            } else {
                Box(modifier = Modifier.fillMaxSize())
            }
        }
    }
}