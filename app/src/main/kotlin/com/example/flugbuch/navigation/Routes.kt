package com.example.flugbuch.navigation

sealed class Routes(val route: String) {
    // Auth
    data object Login    : Routes("login")
    data object Register : Routes("register")

    // Haupt-App
    data object FlightList  : Routes("flight_list")
    data object AddFlight   : Routes("add_flight")
    data object EditFlight  : Routes("edit_flight/{flightId}") {
        fun createRoute(flightId: Int) = "edit_flight/$flightId"
    }
    data object Statistics   : Routes("statistics")
    data object ExportImport : Routes("export_import")
    data object Settings     : Routes("settings")

    // Flugschule
    data object SchoolDashboard : Routes("school_dashboard/{schoolId}") {
        fun createRoute(schoolId: String) = "school_dashboard/$schoolId"
    }
    data object JoinSchool      : Routes("join_school")
    data object CreateSchool    : Routes("create_school")
}
