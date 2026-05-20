package com.example.opensonggoogletvviewer.model

enum class SlideDisplayMode {
    Normal,
    Blank,
    Black
}

data class CurrentSlide(
    val title: String? = null,
    val body: String? = null,
    val displayMode: SlideDisplayMode = SlideDisplayMode.Normal
)
