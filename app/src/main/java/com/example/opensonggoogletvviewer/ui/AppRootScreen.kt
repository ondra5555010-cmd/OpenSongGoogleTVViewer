package com.example.opensonggoogletvviewer.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.tv.material3.Text
import com.example.opensonggoogletvviewer.viewmodel.AppState
import com.example.opensonggoogletvviewer.viewmodel.AppViewModel
import com.example.opensonggoogletvviewer.ui.tv.handleDpad

@Composable
fun AppRootScreen(appVm: AppViewModel) {
    val state by appVm.state.collectAsState()

    when (val s = state) {
        is AppState.Discovering -> {
            // requirement: empty screen appears
            Box(modifier = Modifier.fillMaxSize())
        }

        is AppState.DiscoveryError -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("Discovery error: ${s.message}")
            }
        }

        is AppState.PickServer -> {
            ServerPickerScreen(
                ips = s.results,
                selectedIndex = s.selectedIndex,
                onUp = { appVm.moveSelection(-1) },
                onDown = { appVm.moveSelection(+1) },
                onOk = { appVm.chooseSelected() }
            )
        }

        is AppState.Running -> {
            val slideVm = appVm.slideViewModelOrNull()
            if (slideVm != null) {
                SlideScreen(slideVm)
            } else {
                Box(modifier = Modifier.fillMaxSize())
            }
        }
    }
}