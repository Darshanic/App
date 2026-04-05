package com.sktc.ticketprinter

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager

object AppThemeManager {
    const val KEY_THEME_MODE = "theme_mode"
    private const val MODE_SYSTEM = "system"
    private const val MODE_DARK = "dark"
    private const val MODE_LIGHT = "light"

    fun applyTheme(context: Context) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val selectedMode = prefs.getString(KEY_THEME_MODE, MODE_SYSTEM) ?: MODE_SYSTEM
        val nightMode = when (selectedMode) {
            MODE_DARK -> AppCompatDelegate.MODE_NIGHT_YES
            MODE_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }
}
