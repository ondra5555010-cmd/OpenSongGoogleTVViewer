package com.example.opensonggoogletvviewer.ui.tv

import androidx.compose.runtime.*
import androidx.compose.ui.text.TextStyle

@Stable
class FontScaleController(
    initial: Float = 1.0f,
    private val min: Float = 0.6f,
    private val max: Float = 2.2f,
    private val step: Float = 0.1f
) {
    var scale by mutableFloatStateOf(initial)
        private set

    fun increase() { scale = (scale + step).coerceIn(min, max) }
    fun decrease() { scale = (scale - step).coerceIn(min, max) }
}

@Composable
fun rememberFontScaleController(
    initial: Float = 1.0f,
    min: Float = 0.6f,
    max: Float = 2.2f,
    step: Float = 0.1f
): FontScaleController = remember {
    FontScaleController(initial = initial, min = min, max = max, step = step)
}

/**
 * Scales a TextStyle proportionally (font size + line height + letter spacing).
 * Useful for multi-line lyrics on TV.
 */
fun TextStyle.scaled(scale: Float): TextStyle = this.copy(
    fontSize = this.fontSize * scale,
    lineHeight = this.lineHeight * scale,
    letterSpacing = this.letterSpacing * scale
)