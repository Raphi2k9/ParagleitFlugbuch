package com.example.flugbuch.data.repository

import com.example.flugbuch.SupabaseClientProvider
import com.example.flugbuch.data.model.FlightModel
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order

class SupabaseFlightRepository {

    private val supabase = SupabaseClientProvider.client

    // Nur die EIGENEN Flüge des aktuellen Nutzers für den Sync ins lokale Logbuch.
    // Wichtig: Für Fluglehrer/Admins erlaubt die RLS auch das Lesen der Schüler-Flüge
    // ihrer Schule – ohne diesen user_id-Filter würden die im persönlichen Logbuch
    // des Lehrers landen. Die Schüler-Flüge gehören nur ins Schul-Dashboard.
    // Wirft bei Netzwerk-/Serverfehlern, damit der Aufrufer Fehler von
    // "wirklich keine Flüge" unterscheiden kann (wichtig für den Sync!)
    suspend fun getFlights(): List<FlightModel> {
        val uid = supabase.auth.currentUserOrNull()?.id ?: return emptyList()
        return supabase.postgrest["flights"]
            .select {
                filter { eq("user_id", uid) }
                order("date", Order.DESCENDING)
            }
            .decodeList<FlightModel>()
    }

    // Einzelnen Flug einfügen, gibt die erzeugte UUID zurück
    suspend fun insertFlight(flight: FlightModel): FlightModel? {
        return runCatching {
            supabase.postgrest["flights"]
                .insert(flight) { select() }
                .decodeSingleOrNull<FlightModel>()
        }.getOrNull()
    }

    // Flug aktualisieren (anhand der Supabase-UUID).
    // Bewusst nur die vom Nutzer editierbaren Felder – NICHT user_id und NICHT die
    // Signatur-Spalten (instructor_approved/approved_by/approved_by_name/approved_at).
    // Sonst würde ein Schüler-Edit die Fluglehrer-Unterschrift überschreiben.
    suspend fun updateFlight(flight: FlightModel) {
        runCatching {
            supabase.postgrest["flights"]
                .update({
                    set("date", flight.date)
                    set("glider_name", flight.gliderName)
                    set("duration_minutes", flight.durationMinutes)
                    set("flight_type", flight.flightType)
                    set("start_location", flight.startLocation)
                    set("landing_location", flight.landingLocation)
                    set("max_altitude", flight.maxAltitude)
                    set("distance", flight.distance)
                    set("wind_conditions", flight.windConditions)
                    set("cloud_cover", flight.cloudCover)
                    set("temperature", flight.temperature)
                    set("notes", flight.notes)
                    set("training_exercises", flight.trainingExercises)
                    set("pruefung_bestanden", flight.pruefungBestanden)
                }) {
                    filter { eq("id", flight.id) }
                }
        }
    }

    // Flug löschen
    suspend fun deleteFlight(supabaseId: String) {
        runCatching {
            supabase.postgrest["flights"]
                .delete { filter { eq("id", supabaseId) } }
        }
    }

    // Alle Flüge eines bestimmten Schülers (nur für Instruktoren/Admins – RLS erlaubt das)
    suspend fun getFlightsByUser(userId: String): List<FlightModel> {
        return runCatching {
            supabase.postgrest["flights"]
                .select {
                    filter { eq("user_id", userId) }
                    order("date", Order.DESCENDING)
                }
                .decodeList<FlightModel>()
        }.getOrDefault(emptyList())
    }
}
