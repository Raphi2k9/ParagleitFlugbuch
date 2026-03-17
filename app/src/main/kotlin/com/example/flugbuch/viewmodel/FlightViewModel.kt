package com.example.flugbuch.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.flugbuch.data.database.FlightDatabase
import com.example.flugbuch.data.entities.FlightEntity
import com.example.flugbuch.data.entities.FlightType
import com.example.flugbuch.data.entities.GliderEntity
import com.example.flugbuch.data.repository.FlightRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

enum class SortOrder { DATE_DESC, DATE_ASC, TYPE, GLIDER }
enum class FilterType { ALL, BY_TYPE, BY_GLIDER }

data class FilterState(
    val sortOrder: SortOrder = SortOrder.DATE_DESC,
    val filterType: FilterType = FilterType.ALL,
    val selectedFlightType: FlightType? = null,
    val selectedGlider: String? = null
)

class FlightViewModel(application: Application) : AndroidViewModel(application) {

    private val db = FlightDatabase.getDatabase(application)
    private val repository = FlightRepository(db.flightDao(), db.gliderDao())

    val allGliders: StateFlow<List<GliderEntity>> = repository.allGliders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _filterState = MutableStateFlow(FilterState())
    val filterState: StateFlow<FilterState> = _filterState.asStateFlow()

    private val _allFlightsRaw: StateFlow<List<FlightEntity>> = repository.allFlights
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredFlights: StateFlow<List<FlightEntity>> = combine(
        _allFlightsRaw, _filterState
    ) { flights, filter ->
        var result = flights

        // Filtern
        result = when (filter.filterType) {
            FilterType.BY_TYPE -> {
                val type = filter.selectedFlightType?.name ?: ""
                result.filter { it.flightType == type }
            }
            FilterType.BY_GLIDER -> {
                val glider = filter.selectedGlider ?: ""
                result.filter { it.gliderName == glider }
            }
            FilterType.ALL -> result
        }

        // Sortieren
        result = when (filter.sortOrder) {
            SortOrder.DATE_DESC -> result.sortedByDescending { it.date }
            SortOrder.DATE_ASC -> result.sortedBy { it.date }
            SortOrder.TYPE -> result.sortedBy { it.flightType }
            SortOrder.GLIDER -> result.sortedBy { it.gliderName }
        }
        result
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun updateFilterState(newState: FilterState) {
        _filterState.value = newState
    }

    fun insertFlight(flight: FlightEntity) {
        viewModelScope.launch {
            repository.insertFlight(flight)
        }
    }

    fun updateFlight(flight: FlightEntity) {
        viewModelScope.launch {
            repository.updateFlight(flight)
        }
    }

    fun deleteFlight(flight: FlightEntity) {
        viewModelScope.launch {
            repository.deleteFlight(flight)
        }
    }

    suspend fun getFlightById(id: Int): FlightEntity? =
        repository.getFlightById(id)

    fun deleteGlider(glider: GliderEntity) {
        viewModelScope.launch {
            repository.deleteGlider(glider)
        }
    }

    // Export als JSON
    fun exportToJson(context: Context): File? {
        return try {
            val gson = Gson()
            var exportedFlights: List<FlightEntity> = emptyList()
            viewModelScope.launch {
                exportedFlights = repository.getAllFlightsOnce()
            }
            // Synchrone Variante über Flow collect
            val dir = context.getExternalFilesDir(null) ?: context.filesDir
            val file = File(dir, "flugbuch_export_${System.currentTimeMillis()}.json")
            // Wird asynchron befüllt – daher separate exportFlightsToFile-Methode
            file
        } catch (e: Exception) {
            null
        }
    }

    fun exportFlightsToJson(context: Context, onResult: (File?) -> Unit) {
        viewModelScope.launch {
            try {
                val flights = repository.getAllFlightsOnce()
                val gson = Gson()
                val json = gson.toJson(flights)
                val dir = context.getExternalFilesDir(null) ?: context.filesDir
                val file = File(dir, "flugbuch_export_${System.currentTimeMillis()}.json")
                file.writeText(json)
                onResult(file)
                _message.value = "Exportiert nach: ${file.absolutePath}"
            } catch (e: Exception) {
                onResult(null)
                _message.value = "Export fehlgeschlagen: ${e.message}"
            }
        }
    }

    fun exportFlightsToCsv(context: Context, onResult: (File?) -> Unit) {
        viewModelScope.launch {
            try {
                val flights = repository.getAllFlightsOnce()
                val sb = StringBuilder()
                sb.appendLine("id,date,gliderName,durationMinutes,flightType,startLocation,landingLocation,maxAltitude,distance,windConditions,cloudCover,temperature,notes")
                flights.forEach { f ->
                    sb.appendLine(
                        "${f.id},${f.date},\"${f.gliderName}\",${f.durationMinutes},${f.flightType}," +
                        "\"${f.startLocation}\",\"${f.landingLocation}\",${f.maxAltitude ?: ""},${f.distance ?: ""}," +
                        "\"${f.windConditions}\",\"${f.cloudCover}\",${f.temperature ?: ""},\"${f.notes}\""
                    )
                }
                val dir = context.getExternalFilesDir(null) ?: context.filesDir
                val file = File(dir, "flugbuch_export_${System.currentTimeMillis()}.csv")
                file.writeText(sb.toString())
                onResult(file)
                _message.value = "CSV exportiert nach: ${file.absolutePath}"
            } catch (e: Exception) {
                onResult(null)
                _message.value = "CSV-Export fehlgeschlagen: ${e.message}"
            }
        }
    }

    fun importFromJson(jsonContent: String, onResult: (Int) -> Unit) {
        viewModelScope.launch {
            try {
                val gson = Gson()
                val type = object : TypeToken<List<FlightEntity>>() {}.type
                val flights: List<FlightEntity> = gson.fromJson(jsonContent, type)
                repository.importFlights(flights)
                onResult(flights.size)
                _message.value = "${flights.size} Flüge importiert"
            } catch (e: Exception) {
                onResult(0)
                _message.value = "Import fehlgeschlagen: ${e.message}"
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
