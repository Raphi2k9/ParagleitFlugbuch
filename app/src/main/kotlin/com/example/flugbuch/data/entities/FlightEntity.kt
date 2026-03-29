package com.example.flugbuch.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.flugbuch.data.model.FlightModel

@Entity(tableName = "flights")
data class FlightEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    // Pflichtfelder
    val date: Long,
    val gliderName: String,
    val durationMinutes: Int,
    val flightType: String,

    // Optionale Felder
    val startLocation: String = "",
    val landingLocation: String = "",
    val maxAltitude: Int? = null,
    val distance: Double? = null,
    val windConditions: String = "",
    val cloudCover: String = "",
    val temperature: Int? = null,
    val notes: String = "",
    val trainingExercises: String? = null,
    val pruefungBestanden: Boolean? = null,

    // Cloud-Sync Felder (neu in Version 4)
    val userId: String = "",         // Supabase User-UUID des Eigentümers
    val supabaseId: String? = null   // UUID des Eintrags in Supabase (null = noch nicht synchronisiert)
) {
    // Konvertierung zu FlightModel für Supabase-Upload
    fun toFlightModel(): FlightModel = FlightModel(
        id = supabaseId ?: "",
        userId = userId,
        date = date,
        gliderName = gliderName,
        durationMinutes = durationMinutes,
        flightType = flightType,
        startLocation = startLocation,
        landingLocation = landingLocation,
        maxAltitude = maxAltitude,
        distance = distance,
        windConditions = windConditions,
        cloudCover = cloudCover,
        temperature = temperature,
        notes = notes,
        trainingExercises = trainingExercises,
        pruefungBestanden = pruefungBestanden
    )

    companion object {
        // Konvertierung von FlightModel (Supabase) zu FlightEntity (Room)
        fun fromFlightModel(model: FlightModel, localId: Int = 0): FlightEntity = FlightEntity(
            id = localId,
            date = model.date,
            gliderName = model.gliderName,
            durationMinutes = model.durationMinutes,
            flightType = model.flightType,
            startLocation = model.startLocation,
            landingLocation = model.landingLocation,
            maxAltitude = model.maxAltitude,
            distance = model.distance,
            windConditions = model.windConditions,
            cloudCover = model.cloudCover,
            temperature = model.temperature,
            notes = model.notes,
            trainingExercises = model.trainingExercises,
            pruefungBestanden = model.pruefungBestanden,
            userId = model.userId,
            supabaseId = model.id.ifBlank { null }
        )
    }
}
