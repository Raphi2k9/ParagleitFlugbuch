package com.example.flugbuch

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.flugbuch.navigation.NavGraph
import com.example.flugbuch.ui.theme.FlugbuchTheme
import com.example.flugbuch.ui.theme.ThemePreference
import com.example.flugbuch.viewmodel.FlightViewModel
import com.example.flugbuch.viewmodel.StatisticsViewModel
import com.example.flugbuch.viewmodel.ThemeViewModel
import java.util.Locale

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        val prefs = newBase.getSharedPreferences("flugbuch_prefs", Context.MODE_PRIVATE)
        val lang = prefs.getString("language", "de") ?: "de"
        val locale = Locale(lang)
        Locale.setDefault(locale)
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeViewModel: ThemeViewModel = viewModel()
            val themePreference by themeViewModel.themePreference.collectAsStateWithLifecycle()
            val pilotName by themeViewModel.pilotName.collectAsStateWithLifecycle()
            val licenseNumber by themeViewModel.licenseNumber.collectAsStateWithLifecycle()
            val language by themeViewModel.language.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val isDark = when (themePreference) {
                ThemePreference.DARK -> true
                ThemePreference.LIGHT -> false
                ThemePreference.SYSTEM -> systemDark
            }
            FlugbuchTheme(darkTheme = isDark, dynamicColor = false) {
                FlugbuchApp(
                    themePreference = themePreference,
                    onThemeChange = themeViewModel::setTheme,
                    pilotName = pilotName,
                    onPilotNameChange = themeViewModel::setPilotName,
                    licenseNumber = licenseNumber,
                    onLicenseNumberChange = themeViewModel::setLicenseNumber,
                    language = language,
                    onLanguageChange = { lang ->
                        themeViewModel.setLanguage(lang)
                        recreate()
                    }
                )
            }
        }
    }
}

@Composable
fun FlugbuchApp(
    themePreference: ThemePreference,
    onThemeChange: (ThemePreference) -> Unit,
    pilotName: String,
    onPilotNameChange: (String) -> Unit,
    licenseNumber: String,
    onLicenseNumberChange: (String) -> Unit,
    language: String,
    onLanguageChange: (String) -> Unit
) {
    val navController = rememberNavController()
    val flightViewModel: FlightViewModel = viewModel()
    val statisticsViewModel: StatisticsViewModel = viewModel()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        NavGraph(
            navController = navController,
            flightViewModel = flightViewModel,
            statisticsViewModel = statisticsViewModel,
            themePreference = themePreference,
            onThemeChange = onThemeChange,
            pilotName = pilotName,
            onPilotNameChange = onPilotNameChange,
            licenseNumber = licenseNumber,
            onLicenseNumberChange = onLicenseNumberChange,
            language = language,
            onLanguageChange = onLanguageChange
        )
    }
}
