package com.example.flugbuch.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.flugbuch.data.dao.FlightDao
import com.example.flugbuch.data.dao.GliderDao
import com.example.flugbuch.data.entities.FlightEntity
import com.example.flugbuch.data.entities.GliderEntity

@Database(
    entities = [FlightEntity::class, GliderEntity::class],
    version = 1,
    exportSchema = false
)
abstract class FlightDatabase : RoomDatabase() {

    abstract fun flightDao(): FlightDao
    abstract fun gliderDao(): GliderDao

    companion object {
        @Volatile
        private var INSTANCE: FlightDatabase? = null

        fun getDatabase(context: Context): FlightDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    FlightDatabase::class.java,
                    "flugbuch_database"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
