package com.example.flugbuch.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.flugbuch.ui.screens.AddEditFlightScreen
import com.example.flugbuch.ui.screens.ExportImportScreen
import com.example.flugbuch.ui.screens.FlightListScreen
import com.example.flugbuch.ui.screens.StatisticsScreen
import com.example.flugbuch.ui.theme.ThemePreference
import com.example.flugbuch.viewmodel.FlightViewModel
import com.example.flugbuch.viewmodel.StatisticsViewModel

@Composable
fun NavGraph(
    navController: NavHostController,
    flightViewModel: FlightViewModel,
    statisticsViewModel: StatisticsViewModel,
    themePreference: ThemePreference,
    onThemeChange: (ThemePreference) -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = Routes.FlightList.route
    ) {
        composable(Routes.FlightList.route) {
            FlightListScreen(
                viewModel = flightViewModel,
                onAddFlight = { navController.navigate(Routes.AddFlight.route) },
                onEditFlight = { flightId ->
                    navController.navigate(Routes.EditFlight.createRoute(flightId))
                },
                onNavigateToStats = { navController.navigate(Routes.Statistics.route) },
                onNavigateToExport = { navController.navigate(Routes.ExportImport.route) },
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
    }
}
