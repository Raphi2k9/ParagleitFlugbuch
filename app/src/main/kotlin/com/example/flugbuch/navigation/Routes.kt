package com.example.flugbuch.navigation

sealed class Routes(val route: String) {
    data object FlightList : Routes("flight_list")
    data object AddFlight : Routes("add_flight")
    data object EditFlight : Routes("edit_flight/{flightId}") {
        fun createRoute(flightId: Int) = "edit_flight/$flightId"
    }
    data object Statistics : Routes("statistics")
    data object ExportImport : Routes("export_import")
    data object Settings : Routes("settings")
}
