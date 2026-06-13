package com.example.flugbuch

import android.content.Context
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private class AndroidSessionManager(context: Context) : SessionManager {

    private val prefs = context.getSharedPreferences("supabase_session", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    override suspend fun loadSession(): UserSession? {
        val data = prefs.getString(KEY, null) ?: return null
        return runCatching { json.decodeFromString<UserSession>(data) }.getOrNull()
    }

    override suspend fun saveSession(session: UserSession) {
        val data = runCatching { json.encodeToString(session) }.getOrNull() ?: return
        prefs.edit().putString(KEY, data).apply()
    }

    override suspend fun deleteSession() {
        prefs.edit().remove(KEY).apply()
    }

    companion object {
        private const val KEY = "session"
    }
}

// Von FlugbuchApplication.onCreate() aufrufen – setzt Session-Persistenz auf SharedPreferences
fun initializeSupabase(context: Context) {
    SupabaseClientProvider.sessionManager = AndroidSessionManager(context)
}
