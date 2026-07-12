package com.example.flugbuch.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.flugbuch.ui.screens.*
import com.example.flugbuch.ui.theme.ThemePreference
import com.example.flugbuch.viewmodel.AuthViewModel
import com.example.flugbuch.viewmodel.FlightViewModel
import com.example.flugbuch.viewmodel.SchoolViewModel
import com.example.flugbuch.viewmodel.StatisticsViewModel

@Composable
fun NavGraph(
    navController: NavHostController,
    flightViewModel: FlightViewModel,
    statisticsViewModel: StatisticsViewModel,
    authViewModel: AuthViewModel,
    schoolViewModel: SchoolViewModel,
    themePreference: ThemePreference,
    onThemeChange: (ThemePreference) -> Unit,
    pilotName: String,
    onPilotNameChange: (String) -> Unit,
    licenseNumber: String,
    onLicenseNumberChange: (String) -> Unit,
    language: String,
    onLanguageChange: (String) -> Unit,
    startDestination: String
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // ---- Auth ----------------------------------------------------------------
        composable(Routes.Login.route) {
            LoginScreen(
                viewModel = authViewModel,
                onNavigateToRegister = { navController.navigate(Routes.Register.route) },
                language = language,
                onLanguageChange = onLanguageChange
            )
        }

        composable(Routes.Register.route) {
            RegisterScreen(
                viewModel = authViewModel,
                onNavigateBack = { navController.popBackStack() },
                language = language,
                onLanguageChange = onLanguageChange
            )
        }

        // ---- Haupt-App -----------------------------------------------------------
        composable(Routes.FlightList.route) {
            FlightListScreen(
                viewModel = flightViewModel,
                onAddFlight = { navController.navigate(Routes.AddFlight.route) },
                onEditFlight = { flightId ->
                    navController.navigate(Routes.EditFlight.createRoute(flightId))
                },
                onNavigateToStats = { navController.navigate(Routes.Statistics.route) },
                onNavigateToExport = { navController.navigate(Routes.ExportImport.route) },
                onNavigateToSettings = { navController.navigate(Routes.Settings.route) },
                themePreference = themePreference,
                onThemeChange = onThemeChange
            )
        }

        composable(Routes.AddFlight.route) {
            AddEditFlightScreen(
                viewModel = flightViewModel,
                flightId = null,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.EditFlight.route,
            arguments = listOf(navArgument("flightId") { type = NavType.IntType })
        ) { backStackEntry ->
            val flightId = backStackEntry.arguments?.getInt("flightId")
            AddEditFlightScreen(
                viewModel = flightViewModel,
                flightId = flightId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.Statistics.route) {
            StatisticsScreen(
                viewModel = statisticsViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.ExportImport.route) {
            ExportImportScreen(
                viewModel = flightViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.Settings.route) {
            SettingsScreen(
                themePreference = themePreference,
                onThemeChange = onThemeChange,
                pilotName = pilotName,
                onPilotNameChange = onPilotNameChange,
                licenseNumber = licenseNumber,
                onLicenseNumberChange = onLicenseNumberChange,
                language = language,
                onLanguageChange = onLanguageChange,
                onNavigateBack = { navController.popBackStack() },
                authViewModel = authViewModel,
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Routes.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToSchoolDashboard = { schoolId ->
                    navController.navigate(Routes.SchoolDashboard.createRoute(schoolId))
                },
                onNavigateToJoinSchool = {
                    navController.navigate(Routes.JoinSchool.route)
                },
                onNavigateToCreateSchool = {
                    navController.navigate(Routes.CreateSchool.route)
                }
            )
        }

        // ---- Flugschule ----------------------------------------------------------
        composable(
            route = Routes.SchoolDashboard.route,
            arguments = listOf(navArgument("schoolId") { type = NavType.StringType })
        ) { backStackEntry ->
            val schoolId = backStackEntry.arguments?.getString("schoolId") ?: ""
            SchoolDashboardScreen(
                schoolViewModel = schoolViewModel,
                authViewModel = authViewModel,
                schoolId = schoolId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSettings = { navController.navigate(Routes.Settings.route) }
            )
        }

        composable(Routes.JoinSchool.route) {
            JoinSchoolScreen(
                authViewModel = authViewModel,
                onNavigateBack = { navController.popBackStack() },
                onSkip = {
                    navController.navigate(Routes.FlightList.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.CreateSchool.route) {
            CreateSchoolScreen(
                schoolViewModel = schoolViewModel,
                authViewModel = authViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
