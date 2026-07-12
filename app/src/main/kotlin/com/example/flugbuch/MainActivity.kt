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
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.flugbuch.auth.AuthState
import com.example.flugbuch.data.model.UserRole
import com.example.flugbuch.navigation.NavGraph
import com.example.flugbuch.navigation.Routes
import com.example.flugbuch.ui.theme.FlugbuchTheme
import com.example.flugbuch.ui.theme.ThemePreference
import com.example.flugbuch.viewmodel.AuthViewModel
import com.example.flugbuch.viewmodel.FlightViewModel
import com.example.flugbuch.viewmodel.SchoolViewModel
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
            val authViewModel: AuthViewModel = viewModel()
            val flightViewModel: FlightViewModel = viewModel()
            val statisticsViewModel: StatisticsViewModel = viewModel()
            val schoolViewModel: SchoolViewModel = viewModel()

            val themePreference by themeViewModel.themePreference.collectAsStateWithLifecycle()
            val pilotName by themeViewModel.pilotName.collectAsStateWithLifecycle()
            val licenseNumber by themeViewModel.licenseNumber.collectAsStateWithLifecycle()
            val language by themeViewModel.language.collectAsStateWithLifecycle()
            val authState by authViewModel.authState.collectAsStateWithLifecycle()

            val systemDark = isSystemInDarkTheme()
            val isDark = when (themePreference) {
                ThemePreference.DARK -> true
                ThemePreference.LIGHT -> false
                ThemePreference.SYSTEM -> systemDark
            }

            // userId in FlightViewModel setzen sobald eingeloggt
            LaunchedEffect(authState) {
                if (authState is AuthState.LoggedIn) {
                    flightViewModel.currentUserId = (authState as AuthState.LoggedIn).profile.id
                }
            }

            // Startdestination abhängig vom Auth-Status und Rolle
            val startDestination = when (authState) {
                is AuthState.LoggedIn -> {
                    val profile = (authState as AuthState.LoggedIn).profile
                    val sid = profile.schoolId
                    when (profile.userRole) {
                        UserRole.SCHOOL_ADMIN ->
                            if (sid != null) Routes.SchoolDashboard.createRoute(sid)
                            else Routes.CreateSchool.route
                        UserRole.INSTRUCTOR ->
                            if (sid != null) Routes.SchoolDashboard.createRoute(sid)
                            else Routes.JoinSchool.route
                        UserRole.STUDENT -> Routes.FlightList.route
                    }
                }
                is AuthState.LoggedOut, is AuthState.Error -> Routes.Login.route
                is AuthState.Loading -> Routes.Login.route
            }

            FlugbuchTheme(darkTheme = isDark, dynamicColor = false) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    // Wenn Auth-Status sich ändert, zur richtigen Destination navigieren
                    LaunchedEffect(authState) {
                        when (authState) {
                            is AuthState.LoggedIn -> {
                                val profile = (authState as AuthState.LoggedIn).profile
                                val sid = profile.schoolId
                                val dest = when (profile.userRole) {
                                    UserRole.SCHOOL_ADMIN ->
                                        if (sid != null) Routes.SchoolDashboard.createRoute(sid)
                                        else Routes.CreateSchool.route
                                    UserRole.INSTRUCTOR ->
                                        if (sid != null) Routes.SchoolDashboard.createRoute(sid)
                                        else Routes.JoinSchool.route
                                    UserRole.STUDENT -> Routes.FlightList.route
                                }
                                navController.navigate(dest) {
                                    popUpTo(Routes.Login.route) { inclusive = true }
                                }
                            }
                            is AuthState.LoggedOut -> {
                                val currentRoute = navController.currentBackStackEntry?.destination?.route
                                if (currentRoute != Routes.Login.route && currentRoute != Routes.Register.route) {
                                    navController.navigate(Routes.Login.route) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            }
                            else -> {}
                        }
                    }

                    NavGraph(
                        navController = navController,
                        flightViewModel = flightViewModel,
                        statisticsViewModel = statisticsViewModel,
                        authViewModel = authViewModel,
                        schoolViewModel = schoolViewModel,
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
                        },
                        startDestination = startDestination
                    )
                }
            }
        }
    }
}
