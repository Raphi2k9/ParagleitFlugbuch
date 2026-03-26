package com.example.flugbuch.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.flugbuch.data.dao.FlightDao
import com.example.flugbuch.data.dao.GliderDao
import com.example.flugbuch.data.entities.FlightEntity
import com.example.flugbuch.data.entities.GliderEntity

@Database(
    entities = [FlightEntity::class, GliderEntity::class],
    version = 3,
    exportSchema = false
)
abstract class FlightDatabase : RoomDatabase() {

    abstract fun flightDao(): FlightDao
    abstract fun gliderDao(): GliderDao

    companion object {
        @Volatile
        private var INSTANCE: FlightDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE flights ADD COLUMN trainingExercises TEXT")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE flights ADD COLUMN pruefungBestanden INTEGER")
            }
        }

        fun getDatabase(context: Context): FlightDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    FlightDatabase::class.java,
                    "flugbuch_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build().also { INSTANCE = it }
            }
        }
    }
}
