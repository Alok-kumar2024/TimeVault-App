package com.example.timevault.Model

import androidx.appcompat.app.AppCompatDelegate

object ThemeHelper {

    const val LIGHT = "light"
    const val DARK = "dark"
    const val SYSTEM = "system"

    fun applyTheme(theme : String)
    {
        when(theme)
        {
            "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            "system" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }
}