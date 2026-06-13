package com.example.flugbuch

import android.app.Application
import com.example.flugbuch.data.database.FlightDatabase

class FlugbuchApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Session-Persistenz einrichten, bevor der Supabase-Client das erste Mal benutzt wird
        initializeSupabase(this)
    }

    val database: FlightDatabase by lazy {
        FlightDatabase.getDatabase(this)
    }
}
