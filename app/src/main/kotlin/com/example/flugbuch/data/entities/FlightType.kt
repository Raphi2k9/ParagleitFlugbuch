package com.example.flugbuch.data.entities

/**
 * Flugart-Kategorien.
 * Neue Kategorien können hier einfach hinzugefügt werden.
 */
enum class FlightType(val displayName: String, val description: String) {
    THERMAL("Thermikflug", "Aufwind genutzt"),
    MANEUVER_TRAINING("Manövertraining", "Aktive Flugübungen"),
    SIV("SIV", "Simulated Incident in Flight"),
    CROSS_COUNTRY("Streckenflug", "Streckenoptimierter Flug"),
    HIKE_AND_FLY("Hike & Fly", "Wandern und Fliegen");
}
