package com.example.flugbuch

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

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeViewModel: ThemeViewModel = viewModel()
            val themePreference by themeViewModel.themePreference.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val isDark = when (themePreference) {
                ThemePreference.DARK -> true
                ThemePreference.LIGHT -> false
                ThemePreference.SYSTEM -> systemDark
            }
            FlugbuchTheme(darkTheme = isDark, dynamicColor = false) {
                FlugbuchApp(
                    themePreference = themePreference,
                    onThemeChange = themeViewModel::setTheme
                )
            }
        }
    }
}

@Composable
fun FlugbuchApp(
    themePreference: ThemePreference,
    onThemeChange: (ThemePreference) -> Unit
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
            onThemeChange = onThemeChange
        )
    }
}
