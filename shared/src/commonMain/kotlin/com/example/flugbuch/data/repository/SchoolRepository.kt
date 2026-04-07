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

    // Flug eines Schülers akzeptieren/bestätigen (Instructor)
    suspend fun approveStudentFlight(flightId: String, approved: Boolean) {
        runCatching {
            supabase.postgrest["flights"]
                .update({
                    set("instructor_approved", approved)
                }) {
                    filter { eq("id", flightId) }
                }
        }
    }

    // Flug eines Schülers bearbeiten (School-Admin)
    suspend fun updateStudentFlight(flight: FlightModel) {
        flightRepo.updateFlight(flight)
    }

    private fun generateInviteCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..8).map { chars.random() }.joinToString("")
    }
}
