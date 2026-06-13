package com.example.flugbuch

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

// Singleton – wird einmal in FlugbuchApplication erzeugt und danach überall genutzt
// sessionManager muss VOR dem ersten Zugriff auf `client` gesetzt werden (in Application.onCreate)
object SupabaseClientProvider {

    // Vor dem ersten Zugriff auf `client` setzen (in Application.onCreate)
    var sessionManager: io.github.jan.supabase.auth.SessionManager? = null

    val client by lazy {
        val sm = sessionManager  // außerhalb des Lambdas erfassen, um Namenskonflikt zu vermeiden
        createSupabaseClient(
            supabaseUrl = SupabaseConfig.SUPABASE_URL,
            supabaseKey = SupabaseConfig.SUPABASE_KEY
        ) {
            install(Auth) {
                sm?.let { this.sessionManager = it }
            }
            install(Postgrest)
        }
    }
}
