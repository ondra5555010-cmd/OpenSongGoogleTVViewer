package com.example.opensonggoogletvviewer.ui.tv

import androidx.compose.runtime.*

enum class SlideColorScheme {
    Dark,      // white on black
    Light      // black on white
}

@Stable
class ColorSchemeController(initial: SlideColorScheme = SlideColorScheme.Dark) {

    var scheme by mutableStateOf(initial)
        private set

    fun next() {
        scheme = when (scheme) {
            SlideColorScheme.Dark -> SlideColorScheme.Light
            SlideColorScheme.Light -> SlideColorScheme.Dark
        }
    }

    fun previous() {
        // only two modes, so same as next
        next()
    }
}

@Composable
fun rememberColorSchemeController(
    initial: SlideColorScheme = SlideColorScheme.Dark
): ColorSchemeController = remember {
    ColorSchemeController(initial)
}