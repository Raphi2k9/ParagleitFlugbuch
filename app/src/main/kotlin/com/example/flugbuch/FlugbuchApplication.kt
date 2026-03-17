package com.example.flugbuch

import android.app.Application
import com.example.flugbuch.data.database.FlightDatabase

class FlugbuchApplication : Application() {

    val database: FlightDatabase by lazy {
        FlightDatabase.getDatabase(this)
    }
}
