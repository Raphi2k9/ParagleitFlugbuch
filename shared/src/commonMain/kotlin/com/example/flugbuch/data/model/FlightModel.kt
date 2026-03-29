package com.example.flugbuch.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Repräsentiert einen Flug wie er in Supabase gespeichert ist (snake_case Spaltennamen)
@Serializable
data class FlightModel(
    val id: String = "",
    @SerialName("user_id")          val userId: String = "",
    val date: Long = 0L,
    @SerialName("glider_name")      val gliderName: String = "",
    @SerialName("duration_minutes") val durationMinutes: Int = 0,
    @SerialName("flight_type")      val flightType: String = "",
    @SerialName("start_location")   val startLocation: String = "",
    @SerialName("landing_location") val landingLocation: String = "",
    @SerialName("max_altitude")     val maxAltitude: Int? = null,
    val distance: Double? = null,
    @SerialName("wind_conditions")  val windConditions: String = "",
    @SerialName("cloud_cover")      val cloudCover: String = "",
    val temperature: Int? = null,
    val notes: String = "",
    @SerialName("training_exercises") val trainingExercises: String? = null,
    @SerialName("pruefung_bestanden") val pruefungBestanden: Boolean? = null,
    @SerialName("created_at")       val createdAt: String? = null,
    @SerialName("updated_at")       val updatedAt: String? = null
)
