package com.example.flugbuch.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.flugbuch.data.database.FlightDatabase
import com.example.flugbuch.data.entities.FlightType
import com.example.flugbuch.data.repository.FlightRepository
import kotlinx.coroutines.flow.*

data class FlightTypeStats(
    val flightType: FlightType,
    val count: Int,
    val totalMinutes: Int
)

data class GliderStats(
    val gliderName: String,
    val count: Int,
    val totalMinutes: Int
)

data class DistanceStats(
    val totalKm: Double,
    val avgKm: Double,
    val maxKm: Double,
    val byGlider: List<Pair<String, Double>>,
    val byMonth: Map<String, Double>
)

data class AltitudeStats(
    val globalMaxAlt: Int,
    val avgMaxAlt: Double,
    val maxByGlider: List<Pair<String, Int>>,
    val byMonth: Map<String, Int>
)

data class StatisticsData(
    val totalFlights: Int = 0,
    val totalMinutes: Int = 0,
    val typeStats: List<FlightTypeStats> = emptyList(),
    val gliderStats: List<GliderStats> = emptyList(),
    val flightsByMonth: Map<String, Int> = emptyMap(),
    val distanceStats: DistanceStats? = null,
    val altitudeStats: AltitudeStats? = null
)

class StatisticsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = FlightDatabase.getDatabase(application)
    private val repository = FlightRepository(db.flightDao(), db.gliderDao())

    val statistics: StateFlow<StatisticsData> = repository.allFlights
        .map { flights ->
            if (flights.isEmpty()) return@map StatisticsData()

            val totalMinutes = flights.sumOf { it.durationMinutes }

            val typeStats = FlightType.entries.mapNotNull { type ->
                val typeFlights = flights.filter { it.flightType == type.name }
                if (typeFlights.isEmpty()) null
                else FlightTypeStats(
                    flightType = type,
                    count = typeFlights.size,
                    totalMinutes = typeFlights.sumOf { it.durationMinutes }
                )
            }

            val gliderStats = flights
                .groupBy { it.gliderName }
                .map { (name, gliderFlights) ->
                    GliderStats(
                        gliderName = name,
                        count = gliderFlights.size,
                        totalMinutes = gliderFlights.sumOf { it.durationMinutes }
                    )
                }
                .sortedByDescending { it.count }

            val calendar = java.util.Calendar.getInstance()
            fun monthKey(millis: Long): String {
                calendar.timeInMillis = millis
                return "%04d-%02d".format(
                    calendar.get(java.util.Calendar.YEAR),
                    calendar.get(java.util.Calendar.MONTH) + 1
                )
            }

            val flightsByMonth = flights
                .groupBy { monthKey(it.date) }
                .mapValues { it.value.size }
                .entries.sortedBy { it.key }
                .takeLast(12)
                .associate { it.key to it.value }

            // Distanz-Statistiken
            val flightsWithDistance = flights.filter { (it.distance ?: 0.0) > 0.0 }
            val distanceStats = if (flightsWithDistance.isNotEmpty()) {
                val totalKm = flightsWithDistance.sumOf { it.distance!! }
                val avgKm = totalKm / flightsWithDistance.size
                val maxKm = flightsWithDistance.maxOf { it.distance!! }
                val byGlider = flightsWithDistance
                    .groupBy { it.gliderName }
                    .map { (name, gFlights) -> name to gFlights.sumOf { it.distance!! } }
                    .sortedByDescending { it.second }
                val byMonth = flightsWithDistance
                    .groupBy { monthKey(it.date) }
                    .mapValues { entry -> entry.value.sumOf { it.distance!! } }
                    .entries.sortedBy { it.key }
                    .takeLast(12)
                    .associate { it.key to it.value }
                DistanceStats(totalKm, avgKm, maxKm, byGlider, byMonth)
            } else null

            // Höhen-Statistiken
            val flightsWithAlt = flights.filter { (it.maxAltitude ?: 0) > 0 }
            val altitudeStats = if (flightsWithAlt.isNotEmpty()) {
                val globalMax = flightsWithAlt.maxOf { it.maxAltitude!! }
                val avgMax = flightsWithAlt.sumOf { it.maxAltitude!! }.toDouble() / flightsWithAlt.size
                val maxByGlider = flightsWithAlt
                    .groupBy { it.gliderName }
                    .map { (name, gFlights) -> name to gFlights.maxOf { it.maxAltitude!! } }
                    .sortedByDescending { it.second }
                val byMonth = flightsWithAlt
                    .groupBy { monthKey(it.date) }
                    .mapValues { entry -> entry.value.maxOf { it.maxAltitude!! } }
                    .entries.sortedBy { it.key }
                    .takeLast(12)
                    .associate { it.key to it.value }
                AltitudeStats(globalMax, avgMax, maxByGlider, byMonth)
            } else null

            StatisticsData(
                totalFlights = flights.size,
                totalMinutes = totalMinutes,
                typeStats = typeStats,
                gliderStats = gliderStats,
                flightsByMonth = flightsByMonth,
                distanceStats = distanceStats,
                altitudeStats = altitudeStats
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatisticsData())
}
