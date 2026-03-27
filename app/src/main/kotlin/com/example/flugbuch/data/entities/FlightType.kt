package com.example.flugbuch.data.entities

import androidx.annotation.StringRes
import com.example.flugbuch.R

/**
 * Flugart-Kategorien.
 * Neue Kategorien können hier einfach hinzugefügt werden.
 */
enum class FlightType(
    val displayName: String,       // German name kept for CSV import matching
    val description: String,       // German description kept for compatibility
    @StringRes val labelRes: Int,
    @StringRes val descRes: Int
) {
    THERMAL("Thermikflug", "Aufwind genutzt", R.string.flight_type_thermal, R.string.flight_type_thermal_desc),
    MANEUVER_TRAINING("Manövertraining", "Aktive Flugübungen", R.string.flight_type_maneuver, R.string.flight_type_maneuver_desc),
    SIV("SIV", "Simulated Incident in Flight", R.string.flight_type_siv, R.string.flight_type_siv_desc),
    CROSS_COUNTRY("Streckenflug", "Streckenoptimierter Flug", R.string.flight_type_cross_country, R.string.flight_type_cross_country_desc),
    HIKE_AND_FLY("Hike & Fly", "Wandern und Fliegen", R.string.flight_type_hike_fly, R.string.flight_type_hike_fly_desc),
    SCHULUNGSFLUG("Schulungsflug", "Bodenhandeln & Flugtechnik", R.string.flight_type_training, R.string.flight_type_training_desc),
    PRUEFUNGSFLUG("Prüfungsflug", "Prüfung der Flugtechnik", R.string.flight_type_exam, R.string.flight_type_exam_desc);
}
