package com.example.opensonggoogletvviewer.data

import android.content.SharedPreferences
import com.example.opensonggoogletvviewer.ui.tv.SlideColorScheme

class UiSettingsStore(
    private val prefs: SharedPreferences
) {
    companion object {
        private const val KEY_COLOR_SCHEME = "color_scheme"
    }

    fun loadColorScheme(): SlideColorScheme {
        return when (prefs.getString(KEY_COLOR_SCHEME, SlideColorScheme.Dark.name)) {
            SlideColorScheme.Light.name -> SlideColorScheme.Light
            else -> SlideColorScheme.Dark
        }
    }

    fun saveColorScheme(scheme: SlideColorScheme) {
        prefs.edit()
            .putString(KEY_COLOR_SCHEME, scheme.name)
            .apply()
    }
}