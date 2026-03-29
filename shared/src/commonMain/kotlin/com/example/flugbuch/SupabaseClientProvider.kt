package com.example.flugbuch

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

// Singleton – wird einmal in FlugbuchApplication erzeugt und danach überall genutzt
object SupabaseClientProvider {

    val client by lazy {
        createSupabaseClient(
            supabaseUrl = SupabaseConfig.SUPABASE_URL,
            supabaseKey = SupabaseConfig.SUPABASE_KEY
        ) {
            install(Auth)
            install(Postgrest)
        }
    }
}
