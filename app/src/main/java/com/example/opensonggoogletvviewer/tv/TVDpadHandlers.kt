package com.example.opensonggoogletvviewer.ui.tv

import android.view.KeyEvent
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type

/**
 * Reusable DPAD handler. Returns true when the key was consumed.
 */
fun Modifier.handleDpad(
    onUp: (() -> Unit)? = null,
    onDown: (() -> Unit)? = null,
    onLeft: (() -> Unit)? = null,
    onRight: (() -> Unit)? = null,
    onCenter: (() -> Unit)? = null,
): Modifier = this.onKeyEvent { e ->
    if (e.type != KeyEventType.KeyDown) return@onKeyEvent false

    when (e.nativeKeyEvent.keyCode) {
        KeyEvent.KEYCODE_DPAD_UP -> onUp?.let { it(); true } ?: false
        KeyEvent.KEYCODE_DPAD_DOWN -> onDown?.let { it(); true } ?: false
        KeyEvent.KEYCODE_DPAD_LEFT -> onLeft?.let { it(); true } ?: false
        KeyEvent.KEYCODE_DPAD_RIGHT -> onRight?.let { it(); true } ?: false
        KeyEvent.KEYCODE_DPAD_CENTER,
        KeyEvent.KEYCODE_ENTER -> onCenter?.let { it(); true } ?: false
        else -> false
    }
}