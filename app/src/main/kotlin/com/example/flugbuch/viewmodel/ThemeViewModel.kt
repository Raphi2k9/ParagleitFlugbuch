package com.example.flugbuch.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.example.flugbuch.ui.theme.ThemePreference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ThemeViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("flugbuch_prefs", Context.MODE_PRIVATE)

    private val _themePreference = MutableStateFlow(
        ThemePreference.valueOf(
            prefs.getString("theme", ThemePreference.SYSTEM.name) ?: ThemePreference.SYSTEM.name
        )
    )
    val themePreference: StateFlow<ThemePreference> = _themePreference.asStateFlow()

    fun setTheme(theme: ThemePreference) {
        _themePreference.value = theme
        prefs.edit().putString("theme", theme.name).apply()
    }
}
