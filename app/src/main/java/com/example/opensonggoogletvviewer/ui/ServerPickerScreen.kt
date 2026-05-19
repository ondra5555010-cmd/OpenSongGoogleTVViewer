package com.example.opensonggoogletvviewer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.example.opensonggoogletvviewer.model.AppLanguage
import com.example.opensonggoogletvviewer.model.AppStrings
import com.example.opensonggoogletvviewer.model.SlideRefreshRate
import com.example.opensonggoogletvviewer.network.OpenSongDiscovery
import com.example.opensonggoogletvviewer.ui.tv.SlideColorScheme
import com.example.opensonggoogletvviewer.ui.tv.handleDpad

@Composable
fun ServerPickerScreen(
    servers: List<OpenSongDiscovery.Found>,
    selectedRowIndex: Int,
    colorScheme: SlideColorScheme,
    language: AppLanguage,
    refreshRate: SlideRefreshRate,
    strings: AppStrings,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onOk: () -> Unit,
) {
    val focusRequester = FocusRequester()
    val serverListState = rememberLazyListState()

    val backgroundColor = when (colorScheme) {
        SlideColorScheme.Dark -> Color(0xFF050505)
        SlideColorScheme.Light -> Color(0xFFF7F7F2)
    }
    val textColor = when (colorScheme) {
        SlideColorScheme.Dark -> Color.White
        SlideColorScheme.Light -> Color(0xFF151515)
    }
    val mutedTextColor = textColor.copy(alpha = 0.64f)
    val accentColor = when (colorScheme) {
        SlideColorScheme.Dark -> Color(0xFFFFC857)
        SlideColorScheme.Light -> Color(0xFF006D77)
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(selectedRowIndex, servers.size) {
        if (selectedRowIndex >= 4 && servers.isNotEmpty()) {
            serverListState.animateScrollToItem(selectedRowIndex - 4)
        }
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
                onCenter = onOk
            )
            .padding(horizontal = 72.dp, vertical = 44.dp),
        contentAlignment = Alignment.TopStart
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = strings.appTitle,
                style = MaterialTheme.typography.displaySmall,
                color = textColor
            )

            Text(
                text = strings.selectServer,
                style = MaterialTheme.typography.titleLarge,
                color = mutedTextColor
            )

            Spacer(Modifier.height(6.dp))

            MenuRow(
                selected = selectedRowIndex == 0,
                label = strings.language,
                value = language.displayName,
                textColor = textColor,
                mutedTextColor = mutedTextColor,
                accentColor = accentColor
            )

            val colorSchemeLabel = when (colorScheme) {
                SlideColorScheme.Dark -> strings.dark
                SlideColorScheme.Light -> strings.light
            }

            MenuRow(
                selected = selectedRowIndex == 1,
                label = strings.colorScheme,
                value = colorSchemeLabel,
                textColor = textColor,
                mutedTextColor = mutedTextColor,
                accentColor = accentColor
            )

            MenuRow(
                selected = selectedRowIndex == 2,
                label = strings.refreshRate,
                value = refreshRate.displayName(strings),
                textColor = textColor,
                mutedTextColor = mutedTextColor,
                accentColor = accentColor
            )

            MenuRow(
                selected = selectedRowIndex == 3,
                label = strings.refreshServers,
                value = "",
                textColor = textColor,
                mutedTextColor = mutedTextColor,
                accentColor = accentColor
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "${strings.servers.uppercase()} (${servers.size})",
                style = MaterialTheme.typography.labelLarge,
                color = mutedTextColor
            )

            if (servers.isEmpty()) {
                MenuRow(
                    selected = false,
                    label = strings.noServersFound,
                    value = "",
                    textColor = mutedTextColor,
                    mutedTextColor = mutedTextColor,
                    accentColor = accentColor
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    state = serverListState,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(
                        items = servers,
                        key = { _, server -> "${server.ip}:${server.port}" }
                    ) { index, server ->
                        MenuRow(
                            selected = selectedRowIndex == index + 4,
                            label = server.label,
                            value = server.port.toString(),
                            textColor = textColor,
                            mutedTextColor = mutedTextColor,
                            accentColor = accentColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuRow(
    selected: Boolean,
    label: String,
    value: String,
    textColor: Color,
    mutedTextColor: Color,
    accentColor: Color,
) {
    val shape = RoundedCornerShape(8.dp)
    val rowBackground = if (selected) accentColor.copy(alpha = 0.18f) else Color.Transparent
    val borderColor = if (selected) accentColor else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(rowBackground, shape)
            .border(width = 2.dp, color = borderColor, shape = shape)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = label,
            color = if (selected) textColor else mutedTextColor,
            fontSize = 24.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        if (value.isNotBlank()) {
            Text(
                text = value,
                color = textColor,
                fontSize = 22.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
