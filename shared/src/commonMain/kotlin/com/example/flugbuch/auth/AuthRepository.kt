package com.example.flugbuch.auth

import com.example.flugbuch.SupabaseClientProvider
import com.example.flugbuch.data.model.UserProfile
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class AuthRepository {

    private val supabase = SupabaseClientProvider.client

    // ---- Session ----------------------------------------------------------------

    val currentUser: UserInfo?
        get() = supabase.auth.currentUserOrNull()

    val currentUserId: String?
        get() = currentUser?.id

    val isLoggedIn: Boolean
        get() = currentUser != null

    // Wartet, bis die persistierte Session aus dem Speicher geladen wurde.
    // Vorher liefert currentUserOrNull() beim Kaltstart immer null.
    suspend fun awaitSessionLoaded() {
        supabase.auth.awaitInitialization()
    }

    // Liefert den Auth-Status als Flow (null = ausgeloggt)
    val authStateFlow: Flow<UserInfo?> = supabase.auth.sessionStatus.map { status ->
        supabase.auth.currentUserOrNull()
    }

    // ---- Login / Logout ---------------------------------------------------------

    suspend fun login(email: String, password: String) {
        supabase.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun logout() {
        supabase.auth.signOut()
    }

    // ---- Registrierung ----------------------------------------------------------

    suspend fun register(
        email: String,
        password: String,
        fullName: String,
        licenseNumber: String,
        role: String = "STUDENT"
    ) {
        supabase.auth.signUpWith(Email) {
            this.email = email
            this.password = password
            // Metadaten → handle_new_user Trigger legt damit das Profil an
            data = buildJsonObject {
                put("full_name", fullName)
                put("license_number", licenseNumber)
                put("role", role)
            }
        }
    }

    // ---- Profil abrufen ---------------------------------------------------------

    suspend fun fetchProfile(userId: String): UserProfile? {
        return runCatching {
            supabase.postgrest["user_profiles"]
                .select {
                    filter { eq("id", userId) }
                }
                .decodeSingleOrNull<UserProfile>()
        }.getOrNull()
    }

    suspend fun fetchCurrentProfile(): UserProfile? {
        val uid = currentUserId ?: return null
        return fetchProfile(uid)
    }

    // ---- Schule beitreten -------------------------------------------------------

    suspend fun joinSchool(inviteCode: String): Boolean {
        val uid = currentUserId ?: return false
        val school = runCatching {
            supabase.postgrest["schools"]
                .select { filter { eq("invite_code", inviteCode) } }
                .decodeSingleOrNull<com.example.flugbuch.data.model.School>()
        }.getOrNull() ?: return false

        val updateResult = runCatching {
            supabase.postgrest["user_profiles"]
                .update({ set("school_id", school.id) }) {
                    filter { eq("id", uid) }
                }
        }
        return updateResult.isSuccess
    }

    // ---- Passwort zurücksetzen --------------------------------------------------

    suspend fun resetPassword(email: String) {
        supabase.auth.resetPasswordForEmail(email)
    }
}
