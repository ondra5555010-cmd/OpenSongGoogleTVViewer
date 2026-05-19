package com.example.opensonggoogletvviewer.data

import android.content.SharedPreferences
import com.example.opensonggoogletvviewer.model.AppLanguage
import com.example.opensonggoogletvviewer.model.SlideRefreshRate
import com.example.opensonggoogletvviewer.ui.tv.SlideColorScheme

class UiSettingsStore(
    private val prefs: SharedPreferences
) {
    companion object {
        private const val KEY_COLOR_SCHEME = "color_scheme"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_FONT_SCALE = "font_scale"
        private const val KEY_REFRESH_RATE = "refresh_rate"
        private const val DEFAULT_FONT_SCALE = 1.0f
        private const val MIN_FONT_SCALE = 0.6f
        private const val MAX_FONT_SCALE = 2.2f
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

    fun loadLanguage(): AppLanguage {
        return when (prefs.getString(KEY_LANGUAGE, AppLanguage.English.name)) {
            AppLanguage.Czech.name -> AppLanguage.Czech
            AppLanguage.Slovak.name -> AppLanguage.Slovak
            else -> AppLanguage.English
        }
    }

    fun saveLanguage(language: AppLanguage) {
        prefs.edit()
            .putString(KEY_LANGUAGE, language.name)
            .apply()
    }

    fun loadFontScale(): Float {
        return prefs.getFloat(KEY_FONT_SCALE, DEFAULT_FONT_SCALE)
            .coerceIn(MIN_FONT_SCALE, MAX_FONT_SCALE)
    }

    fun saveFontScale(scale: Float) {
        prefs.edit()
            .putFloat(KEY_FONT_SCALE, scale.coerceIn(MIN_FONT_SCALE, MAX_FONT_SCALE))
            .apply()
    }

    fun loadRefreshRate(): SlideRefreshRate {
        return when (prefs.getString(KEY_REFRESH_RATE, SlideRefreshRate.Regular.name)) {
            SlideRefreshRate.Quicker.name -> SlideRefreshRate.Quicker
            SlideRefreshRate.Slower.name -> SlideRefreshRate.Slower
            SlideRefreshRate.Slow.name -> SlideRefreshRate.Slow
            else -> SlideRefreshRate.Regular
        }
    }

    fun saveRefreshRate(rate: SlideRefreshRate) {
        prefs.edit()
            .putString(KEY_REFRESH_RATE, rate.name)
            .apply()
    }
}
