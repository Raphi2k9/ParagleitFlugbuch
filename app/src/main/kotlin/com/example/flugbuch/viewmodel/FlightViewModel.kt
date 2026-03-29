package com.example.flugbuch.viewmodel

import android.app.Application
import android.content.Context
import android.location.Geocoder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.flugbuch.data.database.FlightDatabase
import com.example.flugbuch.data.entities.FlightEntity
import com.example.flugbuch.data.entities.FlightType
import com.example.flugbuch.data.entities.GliderEntity
import com.example.flugbuch.data.repository.FlightRepository
import com.example.flugbuch.data.repository.SupabaseFlightRepository
import com.example.flugbuch.igc.Coords
import com.example.flugbuch.igc.parseIgc
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.time.ZoneOffset
import java.util.Locale

enum class SortOrder { DATE_DESC, DATE_ASC, TYPE, GLIDER }

data class FilterState(
    val sortOrder: SortOrder = SortOrder.DATE_DESC,
    val selectedFlightTypes: Set<FlightType> = emptySet(),
    val selectedGliders: Set<String> = emptySet()
) {
    val isFiltered: Boolean get() = selectedFlightTypes.isNotEmpty() || selectedGliders.isNotEmpty()
}

class FlightViewModel(application: Application) : AndroidViewModel(application) {

    private val db = FlightDatabase.getDatabase(application)
    private val repository = FlightRepository(db.flightDao(), db.gliderDao())
    private val supabaseRepo = SupabaseFlightRepository()

    // Aktuell eingeloggter User (wird von MainActivity gesetzt)
    var currentUserId: String = ""

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

        if (filter.selectedFlightTypes.isNotEmpty()) {
            val typeNames = filter.selectedFlightTypes.map { it.name }.toSet()
            result = result.filter { it.flightType in typeNames }
        }

        if (filter.selectedGliders.isNotEmpty()) {
            result = result.filter { it.gliderName in filter.selectedGliders }
        }

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

    // ---- CRUD mit Supabase-Sync ------------------------------------------------

    fun insertFlight(flight: FlightEntity) {
        viewModelScope.launch {
            val withUser = flight.copy(userId = currentUserId)
            val localId = repository.insertFlight(withUser)
            // In Supabase hochladen und supabaseId zurückspeichern
            if (currentUserId.isNotBlank()) {
                val model = supabaseRepo.insertFlight(withUser.toFlightModel())
                if (model?.id?.isNotBlank() == true) {
                    val localFlight = db.flightDao().getFlightById(localId.toInt())
                    if (localFlight != null) {
                        db.flightDao().updateFlight(localFlight.copy(supabaseId = model.id))
                    }
                }
            }
        }
    }

    fun updateFlight(flight: FlightEntity) {
        viewModelScope.launch {
            repository.updateFlight(flight)
            if (flight.supabaseId != null) {
                supabaseRepo.updateFlight(flight.toFlightModel())
            }
        }
    }

    fun deleteFlight(flight: FlightEntity) {
        viewModelScope.launch {
            repository.deleteFlight(flight)
            if (flight.supabaseId != null) {
                supabaseRepo.deleteFlight(flight.supabaseId)
            }
        }
    }

    suspend fun getFlightById(id: Int): FlightEntity? =
        repository.getFlightById(id)

    fun deleteGlider(glider: GliderEntity) {
        viewModelScope.launch { repository.deleteGlider(glider) }
    }

    fun deleteAllFlights() {
        viewModelScope.launch {
            repository.deleteAllFlights()
            _message.value = "Flugbuch geleert"
        }
    }

    // ---- Export / Import (unverändert) ----------------------------------------

    fun exportFlightsToJson(context: Context, onResult: (File?) -> Unit) {
        viewModelScope.launch {
            try {
                val flights = repository.getAllFlightsOnce()
                val json = Gson().toJson(flights)
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
                val type = object : TypeToken<List<FlightEntity>>() {}.type
                val flights: List<FlightEntity> = Gson().fromJson(jsonContent, type)
                flights.forEach { flight ->
                    insertFlight(flight.copy(id = 0, userId = currentUserId, supabaseId = null))
                }
                onResult(flights.size)
                _message.value = "${flights.size} Flüge importiert"
            } catch (e: Exception) {
                onResult(0)
                _message.value = "Import fehlgeschlagen: ${e.message}"
            }
        }
    }

    fun importFromCsv(csvContent: String, onResult: (Int) -> Unit) {
        viewModelScope.launch {
            try {
                val lines = csvContent.lines().filter { it.isNotBlank() }
                if (lines.size < 2) {
                    _message.value = "CSV-Datei ist leer oder hat keine Daten"
                    onResult(0)
                    return@launch
                }

                val headers = parseCsvLine(lines[0]).map { it.trim().lowercase() }

                fun col(vararg names: String) = names.firstNotNullOfOrNull { name ->
                    headers.indexOfFirst { it == name }.takeIf { it >= 0 }
                }

                val idxDate     = col("date", "datum", "flugdatum")
                val idxGlider   = col("glidername", "glider", "schirm", "gleitschirm", "flügel")
                val idxDuration = col("durationminutes", "duration", "dauer", "dauerminuten", "flugzeit", "minuten")
                val idxType     = col("flighttype", "type", "flugtyp", "typ")
                val idxStart    = col("startlocation", "start", "startort", "startplatz", "abflug")
                val idxLanding  = col("landinglocation", "landing", "landeplatz", "landung", "ziel")
                val idxAltitude = col("maxaltitude", "altitude", "hoehe", "höhe", "maxhoehe", "maxhöhe", "hoehe_m")
                val idxDistance = col("distance", "distanz", "strecke", "km")
                val idxWind     = col("windconditions", "wind", "windbedingungen")
                val idxCloud    = col("cloudcover", "cloud", "bedeckung", "wolken")
                val idxTemp     = col("temperature", "temp", "temperatur")
                val idxNotes    = col("notes", "notizen", "bemerkungen", "anmerkungen", "kommentar")

                if (idxDate == null || idxGlider == null || idxDuration == null) {
                    _message.value = "CSV fehlt Pflichtfelder: Datum, Schirm oder Dauer"
                    onResult(0)
                    return@launch
                }

                val dateFormats = listOf("dd.MM.yyyy", "yyyy-MM-dd", "dd/MM/yyyy", "MM/dd/yyyy", "yyyy/MM/dd")
                fun parseDate(raw: String): Long? {
                    val trimmed = raw.trim()
                    trimmed.toLongOrNull()?.let { return it }
                    for (fmt in dateFormats) {
                        try { return SimpleDateFormat(fmt, Locale.getDefault()).parse(trimmed)?.time } catch (_: Exception) {}
                    }
                    return null
                }

                fun parseFlightType(raw: String): String {
                    val s = raw.trim()
                    FlightType.entries.firstOrNull { it.name.equals(s, ignoreCase = true) }?.let { return it.name }
                    FlightType.entries.firstOrNull { it.displayName.equals(s, ignoreCase = true) }?.let { return it.name }
                    FlightType.entries.firstOrNull { it.displayName.contains(s, ignoreCase = true) || s.contains(it.displayName, ignoreCase = true) }?.let { return it.name }
                    return FlightType.THERMAL.name
                }

                fun parseDuration(raw: String): Int {
                    val trimmed = raw.trim()
                    val colonIdx = trimmed.indexOf(':')
                    if (colonIdx > 0) {
                        val h = trimmed.substring(0, colonIdx).toIntOrNull() ?: 0
                        val m = trimmed.substring(colonIdx + 1).toIntOrNull() ?: 0
                        return h * 60 + m
                    }
                    return trimmed.toIntOrNull() ?: 0
                }

                val flights = mutableListOf<FlightEntity>()
                for (line in lines.drop(1)) {
                    val cols = parseCsvLine(line)
                    if (cols.size <= maxOf(idxDate, idxGlider, idxDuration)) continue

                    fun get(idx: Int?) = if (idx != null && idx < cols.size) cols[idx].trim() else ""

                    val date = parseDate(get(idxDate)) ?: continue
                    val glider = get(idxGlider).ifBlank { continue }
                    val duration = parseDuration(get(idxDuration))

                    flights.add(FlightEntity(
                        id = 0, date = date, gliderName = glider, durationMinutes = duration,
                        flightType = parseFlightType(get(idxType)),
                        startLocation = get(idxStart), landingLocation = get(idxLanding),
                        maxAltitude = get(idxAltitude).toIntOrNull(),
                        distance = get(idxDistance).replace(',', '.').toDoubleOrNull(),
                        windConditions = get(idxWind), cloudCover = get(idxCloud),
                        temperature = get(idxTemp).toIntOrNull(), notes = get(idxNotes),
                        userId = currentUserId
                    ))
                }

                flights.forEach { insertFlight(it) }
                onResult(flights.size)
                _message.value = "${flights.size} Flüge aus CSV importiert"
            } catch (e: Exception) {
                onResult(0)
                _message.value = "CSV-Import fehlgeschlagen: ${e.message}"
            }
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' && inQuotes && i + 1 < line.length && line[i + 1] == '"' -> { current.append('"'); i++ }
                c == '"' -> inQuotes = !inQuotes
                c == ',' && !inQuotes -> { result.add(current.toString()); current.clear() }
                c == ';' && !inQuotes -> { result.add(current.toString()); current.clear() }
                else -> current.append(c)
            }
            i++
        }
        result.add(current.toString())
        return result
    }

    fun importFromIgc(igcContent: String, onResult: (Int) -> Unit) {
        viewModelScope.launch {
            try {
                val igc = parseIgc(igcContent)
                val startLocation = reverseGeocode(igc.launchCoords)
                val landingLocation = reverseGeocode(igc.landingCoords)
                val flight = FlightEntity(
                    id = 0,
                    date = igc.date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
                    gliderName = igc.glider ?: "Unbekannt",
                    durationMinutes = (igc.durationSeconds / 60).toInt(),
                    flightType = FlightType.THERMAL.name,
                    maxAltitude = igc.maxAltitudeMeters,
                    distance = igc.trackDistanceKm,
                    startLocation = startLocation,
                    landingLocation = landingLocation,
                    notes = igc.pilot?.let { "Pilot: $it" } ?: "",
                    userId = currentUserId
                )
                insertFlight(flight)
                onResult(1)
                _message.value = "IGC-Flug importiert (${igc.date})"
            } catch (e: Exception) {
                onResult(0)
                _message.value = "IGC-Import fehlgeschlagen: ${e.message}"
            }
        }
    }

    @Suppress("DEPRECATION")
    private suspend fun reverseGeocode(coords: Coords): String = withContext(Dispatchers.IO) {
        runCatching {
            val geocoder = Geocoder(getApplication(), Locale.getDefault())
            geocoder.getFromLocation(coords.lat, coords.lng, 1)
                ?.firstOrNull()
                ?.let { addr ->
                    listOfNotNull(addr.locality ?: addr.subAdminArea, addr.adminArea)
                        .joinToString(", ").ifBlank { null }
                } ?: coordsToString(coords)
        }.getOrElse { coordsToString(coords) }
    }

    private fun coordsToString(coords: Coords): String {
        val latDir = if (coords.lat >= 0) "N" else "S"
        val lngDir = if (coords.lng >= 0) "E" else "W"
        return "%.4f°%s, %.4f°%s".format(Math.abs(coords.lat), latDir, Math.abs(coords.lng), lngDir)
    }

    fun clearMessage() { _message.value = null }
}
