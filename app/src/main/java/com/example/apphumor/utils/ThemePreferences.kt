package com.example.apphumor.utils

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object ThemePreferences {
    private const val PREFS_NAME = "app_humor_prefs"
    private const val KEY_THEME = "key_theme_mode"

    const val THEME_LIGHT = "light"
    const val THEME_DARK = "dark"
    const val THEME_SYSTEM = "system"

    fun applyTheme(context: Context) {
        val mode = getSavedTheme(context)
        val nightMode = when (mode) {
            THEME_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            THEME_DARK -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }

    fun saveTheme(context: Context, themeMode: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_THEME, themeMode).apply()
        applyTheme(context)
    }

    fun getSavedTheme(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_THEME, THEME_SYSTEM) ?: THEME_SYSTEM
    }
}