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

data class StatisticsData(
    val totalFlights: Int = 0,
    val totalMinutes: Int = 0,
    val typeStats: List<FlightTypeStats> = emptyList(),
    val gliderStats: List<GliderStats> = emptyList(),
    val flightsByMonth: Map<String, Int> = emptyMap()
)

class StatisticsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = FlightDatabase.getDatabase(application)
    private val repository = FlightRepository(db.flightDao(), db.gliderDao())

    val statistics: StateFlow<StatisticsData> = repository.allFlights
        .map { flights ->
            if (flights.isEmpty()) return@map StatisticsData()

            val totalMinutes = flights.sumOf { it.durationMinutes }

            // Stats per Flugart
            val typeStats = FlightType.entries.mapNotNull { type ->
                val typeFlights = flights.filter { it.flightType == type.name }
                if (typeFlights.isEmpty()) null
                else FlightTypeStats(
                    flightType = type,
                    count = typeFlights.size,
                    totalMinutes = typeFlights.sumOf { it.durationMinutes }
                )
            }

            // Stats per Schirm
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

            // Flüge pro Monat (letzte 12 Monate)
            val calendar = java.util.Calendar.getInstance()
            val flightsByMonth = flights
                .groupBy {
                    calendar.timeInMillis = it.date
                    val year = calendar.get(java.util.Calendar.YEAR)
                    val month = calendar.get(java.util.Calendar.MONTH) + 1
                    "%04d-%02d".format(year, month)
                }
                .mapValues { it.value.size }
                .entries
                .sortedBy { it.key }
                .takeLast(12)
                .associate { it.key to it.value }

            StatisticsData(
                totalFlights = flights.size,
                totalMinutes = totalMinutes,
                typeStats = typeStats,
                gliderStats = gliderStats,
                flightsByMonth = flightsByMonth
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatisticsData())
}
