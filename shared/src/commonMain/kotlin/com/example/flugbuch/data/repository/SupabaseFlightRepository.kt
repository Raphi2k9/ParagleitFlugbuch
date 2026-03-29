package com.example.flugbuch.data.repository

import com.example.flugbuch.SupabaseClientProvider
import com.example.flugbuch.data.model.FlightModel
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order

class SupabaseFlightRepository {

    private val supabase = SupabaseClientProvider.client

    // Alle Flüge des aktuellen Nutzers (RLS filtert automatisch)
    suspend fun getFlights(): List<FlightModel> {
        return runCatching {
            supabase.postgrest["flights"]
                .select {
                    order("date", Order.DESCENDING)
                }
                .decodeList<FlightModel>()
        }.getOrDefault(emptyList())
    }

    // Einzelnen Flug einfügen, gibt die erzeugte UUID zurück
    suspend fun insertFlight(flight: FlightModel): FlightModel? {
        return runCatching {
            supabase.postgrest["flights"]
                .insert(flight) { select() }
                .decodeSingleOrNull<FlightModel>()
        }.getOrNull()
    }

    // Flug aktualisieren (anhand der Supabase-UUID)
    suspend fun updateFlight(flight: FlightModel) {
        runCatching {
            supabase.postgrest["flights"]
                .update(flight) {
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
