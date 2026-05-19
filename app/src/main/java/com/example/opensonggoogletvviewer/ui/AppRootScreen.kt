package com.example.opensonggoogletvviewer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.example.opensonggoogletvviewer.model.stringsFor
import com.example.opensonggoogletvviewer.ui.tv.SlideColorScheme
import com.example.opensonggoogletvviewer.ui.tv.handleDpad
import com.example.opensonggoogletvviewer.viewmodel.AppState
import com.example.opensonggoogletvviewer.viewmodel.AppViewModel

@Composable
fun AppRootScreen(appVm: AppViewModel) {
    val state by appVm.state.collectAsState()
    val colorScheme by appVm.colorScheme.collectAsState()
    val language by appVm.language.collectAsState()
    val fontScale by appVm.fontScale.collectAsState()
    val refreshRate by appVm.refreshRate.collectAsState()
    val strings = stringsFor(language)

    when (val s = state) {
        is AppState.Discovering -> {
            StatusScreen(
                title = strings.appTitle,
                message = strings.searching,
                colorScheme = colorScheme,
                onReturn = { appVm.showMainMenu() }
            )
        }

        is AppState.DiscoveryError -> {
            StatusScreen(
                title = strings.discoveryError,
                message = s.message,
                colorScheme = colorScheme,
                onReturn = { appVm.showMainMenu() }
            )
        }

        is AppState.PickServer -> {
            ServerPickerScreen(
                servers = s.results,
                selectedRowIndex = s.selectedRowIndex,
                colorScheme = colorScheme,
                language = language,
                refreshRate = refreshRate,
                strings = strings,
                onUp = { appVm.moveSelection(-1) },
                onDown = { appVm.moveSelection(+1) },
                onOk = { appVm.chooseSelected() }
            )
        }

        is AppState.Running -> {
            val slideVm = appVm.slideViewModelOrNull()
            if (slideVm != null) {
                SlideScreen(
                    vm = slideVm,
                    colorScheme = colorScheme,
                    strings = strings,
                    fontScale = fontScale,
                    onIncreaseFontScale = { appVm.increaseFontScale() },
                    onDecreaseFontScale = { appVm.decreaseFontScale() },
                    onSettings = { appVm.toggleColorScheme() }
                )
            } else {
                Box(modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun StatusScreen(
    title: String,
    message: String,
    colorScheme: SlideColorScheme,
    onReturn: () -> Unit,
) {
    val focusRequester = FocusRequester()
    val backgroundColor = when (colorScheme) {
        SlideColorScheme.Dark -> Color(0xFF050505)
        SlideColorScheme.Light -> Color(0xFFF7F7F2)
    }
    val textColor = when (colorScheme) {
        SlideColorScheme.Dark -> Color.White
        SlideColorScheme.Light -> Color(0xFF151515)
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .focusRequester(focusRequester)
            .focusable()
            .handleDpad(onBack = onReturn)
            .padding(64.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                color = textColor,
                style = MaterialTheme.typography.headlineLarge
            )
            Text(
                text = message,
                color = textColor.copy(alpha = 0.78f),
                style = MaterialTheme.typography.headlineSmall
            )
        }
    }
}
