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

    private val _pilotName = MutableStateFlow(prefs.getString("pilot_name", "") ?: "")
    val pilotName: StateFlow<String> = _pilotName.asStateFlow()

    private val _licenseNumber = MutableStateFlow(prefs.getString("license_number", "") ?: "")
    val licenseNumber: StateFlow<String> = _licenseNumber.asStateFlow()

    private val _language = MutableStateFlow(prefs.getString("language", "de") ?: "de")
    val language: StateFlow<String> = _language.asStateFlow()

    fun setTheme(theme: ThemePreference) {
        _themePreference.value = theme
        prefs.edit().putString("theme", theme.name).apply()
    }

    fun setPilotName(name: String) {
        _pilotName.value = name
        prefs.edit().putString("pilot_name", name).apply()
    }

    fun setLicenseNumber(number: String) {
        _licenseNumber.value = number
        prefs.edit().putString("license_number", number).apply()
    }

    fun setLanguage(lang: String) {
        _language.value = lang
        prefs.edit().putString("language", lang).apply()
    }
}
