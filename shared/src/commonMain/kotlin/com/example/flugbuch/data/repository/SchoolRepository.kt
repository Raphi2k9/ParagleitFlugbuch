package com.example.flugbuch.data.repository

import com.example.flugbuch.SupabaseClientProvider
import com.example.flugbuch.data.model.FlightModel
import com.example.flugbuch.data.model.School
import com.example.flugbuch.data.model.StudentWithFlights
import com.example.flugbuch.data.model.UserProfile
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class SchoolRepository {

    private val supabase = SupabaseClientProvider.client
    private val flightRepo = SupabaseFlightRepository()

    // Separates Insert-DTO ohne id/createdAt, damit Postgres die UUID selbst generiert
    @Serializable
    private data class SchoolInsert(
        val name: String,
        val location: String,
        @SerialName("invite_code") val inviteCode: String
    )

    // Schule anhand der Schul-ID laden
    suspend fun getSchool(schoolId: String): School? {
        return runCatching {
            supabase.postgrest["schools"]
                .select { filter { eq("id", schoolId) } }
                .decodeSingleOrNull<School>()
        }.getOrNull()
    }

    // Neue Schule anlegen (nur SCHOOL_ADMIN)
    suspend fun createSchool(name: String, location: String): School? {
        val inviteCode = generateInviteCode()
        return runCatching {
            supabase.postgrest["schools"]
                .insert(SchoolInsert(name = name, location = location, inviteCode = inviteCode)) { select() }
                .decodeSingleOrNull<School>()
        }.getOrNull()
    }

    // Admin-Rolle für den aktuellen Nutzer setzen und Schule zuweisen
    suspend fun assignSchoolAdmin(userId: String, schoolId: String) {
        runCatching {
            supabase.postgrest["user_profiles"]
                .update({
                    set("school_id", schoolId)
                    set("role", "SCHOOL_ADMIN")
                }) {
                    filter { eq("id", userId) }
                }
        }
    }

    // Alle Schüler der Schule laden
    suspend fun getStudents(schoolId: String): List<UserProfile> {
        return runCatching {
            supabase.postgrest["user_profiles"]
                .select {
                    filter {
                        eq("school_id", schoolId)
                        eq("role", "STUDENT")
                    }
                }
                .decodeList<UserProfile>()
        }.getOrDefault(emptyList())
    }

    // Alle Mitglieder der Schule (Schüler + Lehrer) für Dashboard
    suspend fun getStudentsWithFlights(schoolId: String): List<StudentWithFlights> {
        val students = getStudents(schoolId)
        return students.map { student ->
            val flights = flightRepo.getFlightsByUser(student.id)
            StudentWithFlights(profile = student, flights = flights)
        }
    }

    // Flug eines Schülers unterschreiben bzw. Unterschrift zurückziehen (Fluglehrer/Admin).
    // Beim Unterschreiben werden Name und Zeitpunkt des Lehrers festgehalten,
    // beim Zurückziehen alle Signatur-Felder wieder geleert.
    suspend fun setFlightSignature(
        flightId: String,
        approved: Boolean,
        instructorId: String?,
        instructorName: String?,
        signedAtIso: String?
    ) {
        val by: String? = if (approved) instructorId else null
        val byName: String? = if (approved) instructorName else null
        val at: String? = if (approved) signedAtIso else null
        runCatching {
            supabase.postgrest["flights"]
                .update({
                    set("instructor_approved", approved)
                    set("approved_by", by)
                    set("approved_by_name", byName)
                    set("approved_at", at)
                }) {
                    filter { eq("id", flightId) }
                }
        }
    }

    // Flug eines Schülers bearbeiten (School-Admin)
    suspend fun updateStudentFlight(flight: FlightModel) {
        flightRepo.updateFlight(flight)
    }

    // Mitglied aus der Schule entfernen (nur School-Admin): school_id -> NULL.
    // Läuft über eine SECURITY-DEFINER-RPC, die die Berechtigung serverseitig
    // prüft (Admin derselben Schule, nicht sich selbst). Die Flüge des Mitglieds
    // bleiben in dessen eigenem Flugbuch erhalten.
    suspend fun removeMember(userId: String): Boolean {
        return runCatching {
            supabase.postgrest.rpc(
                "remove_school_member",
                buildJsonObject { put("target_user", userId) }
            )
        }.isSuccess
    }

    private fun generateInviteCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..8).map { chars.random() }.joinToString("")
    }
}
