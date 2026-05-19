package com.example.opensonggoogletvviewer.model

enum class SlideRefreshRate(
    val intervalMs: Long,
) {
    Quicker(500),
    Regular(1000),
    Slower(1500),
    Slow(2000);

    fun next(): SlideRefreshRate {
        val values = entries
        return values[(ordinal + 1) % values.size]
    }

    fun displayName(strings: AppStrings): String {
        val label = when (this) {
            Quicker -> strings.refreshRateQuicker
            Regular -> strings.refreshRateRegular
            Slower -> strings.refreshRateSlower
            Slow -> strings.refreshRateSlow
        }

        return "$label (${intervalMs} ms)"
    }
}
