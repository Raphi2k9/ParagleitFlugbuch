package com.example.flugbuch.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "flights")
data class FlightEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    // Pflichtfelder
    val date: Long,                     // Timestamp in Millisekunden
    val gliderName: String,             // z.B. "Advance Alpha 7"
    val durationMinutes: Int,           // Gesamtdauer in Minuten
    val flightType: String,             // FlightType.name

    // Optionale Felder
    val startLocation: String = "",
    val landingLocation: String = "",
    val maxAltitude: Int? = null,       // Meter
    val distance: Double? = null,       // km
    val windConditions: String = "",
    val cloudCover: String = "",
    val temperature: Int? = null,       // Celsius
    val notes: String = "",
    val trainingExercises: String? = null,  // Komma-getrennte TrainingExercise-Namen (nur für SCHULUNGSFLUG)
    val pruefungBestanden: Boolean? = null  // Prüfungsergebnis (nur für PRUEFUNGSFLUG)
)
