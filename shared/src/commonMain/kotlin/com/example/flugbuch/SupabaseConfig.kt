package com.example.flugbuch

// ============================================================
// Supabase Konfiguration
// Für lokales Testen mit "supabase start":
//   SUPABASE_URL  = "http://10.0.2.2:54321"  (Android Emulator)
//   SUPABASE_URL  = "http://localhost:54321"  (iOS Simulator)
//   SUPABASE_KEY  = anon key aus "supabase start" Ausgabe
//
// Für Produktion:
//   SUPABASE_URL  = "https://<dein-projekt>.supabase.co"
//   SUPABASE_KEY  = anon key aus Supabase Dashboard
// ============================================================
object SupabaseConfig {
    // TODO: Vor dem ersten Start anpassen
    const val SUPABASE_URL = "http://192.168.86.31:54321"
    const val SUPABASE_KEY = "sb_publishable_ACJWlzQHlZjBrEguHvfOxg_3BJgxAaH"
}
