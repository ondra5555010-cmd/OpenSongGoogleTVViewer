package com.example.opensonggoogletvviewer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.example.opensonggoogletvviewer.ui.tv.SlideColorScheme
import com.example.opensonggoogletvviewer.ui.tv.handleDpad

@Composable
fun ServerPickerScreen(
    ips: List<String>,
    selectedIndex: Int,
    colorScheme: SlideColorScheme,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onLeft: () -> Unit,
    onRight: () -> Unit,
    onOk: () -> Unit,
) {
    val focusRequester = FocusRequester()
    val style = TextStyle(fontSize = 28.sp)

    val backgroundColor = when (colorScheme) {
        SlideColorScheme.Dark -> Color.Black
        SlideColorScheme.Light -> Color.White
    }
    val textColor = when (colorScheme) {
        SlideColorScheme.Dark -> Color.White
        SlideColorScheme.Light -> Color.Black
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
            .handleDpad(
                onUp = onUp,
                onDown = onDown,
                onLeft = onLeft,
                onRight = onRight,
                onCenter = onOk
            )
            .padding(48.dp),
        contentAlignment = Alignment.TopStart
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            Text(
                text = "Select OpenSong server",
                style = style,
                color = textColor
            )

            Spacer(Modifier.height(24.dp))

            if (ips.isEmpty()) {
                Text(
                    text = "No servers found.",
                    style = style,
                    color = textColor
                )
            } else {
                val start = (selectedIndex - 6).coerceAtLeast(0)
                val end = (start + 12).coerceAtMost(ips.size)

                for (i in start until end) {
                    val prefix = if (i == selectedIndex) "➤ " else "  "

                    Text(
                        text = prefix + ips[i],
                        style = style,
                        color = textColor
                    )
                }
            }
        }
    }
}