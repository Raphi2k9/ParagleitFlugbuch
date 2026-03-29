package com.example.flugbuch.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class School(
    val id: String = "",
    val name: String = "",
    val location: String = "",
    @SerialName("invite_code") val inviteCode: String = "",
    @SerialName("created_at")  val createdAt: String? = null
)

// Wird für das Schul-Dashboard verwendet: Schüler mit seinen Flügen
data class StudentWithFlights(
    val profile: UserProfile,
    val flights: List<FlightModel>
)
