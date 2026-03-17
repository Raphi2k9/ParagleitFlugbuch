package com.example.flugbuch.data.dao

import androidx.room.*
import com.example.flugbuch.data.entities.FlightEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FlightDao {

    @Query("SELECT * FROM flights ORDER BY date DESC")
    fun getAllFlights(): Flow<List<FlightEntity>>

    @Query("SELECT * FROM flights WHERE id = :id")
    suspend fun getFlightById(id: Int): FlightEntity?

    @Query("SELECT * FROM flights ORDER BY date DESC")
    suspend fun getAllFlightsOnce(): List<FlightEntity>

    @Query("SELECT * FROM flights WHERE flightType = :type ORDER BY date DESC")
    fun getFlightsByType(type: String): Flow<List<FlightEntity>>

    @Query("SELECT * FROM flights WHERE gliderName = :gliderName ORDER BY date DESC")
    fun getFlightsByGlider(gliderName: String): Flow<List<FlightEntity>>

    @Query("SELECT * FROM flights WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getFlightsByDateRange(startDate: Long, endDate: Long): Flow<List<FlightEntity>>

    @Query("SELECT SUM(durationMinutes) FROM flights")
    fun getTotalFlightMinutes(): Flow<Int?>

    @Query("SELECT COUNT(*) FROM flights")
    fun getTotalFlightCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM flights WHERE flightType = :type")
    fun getFlightCountByType(type: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlight(flight: FlightEntity): Long

    @Update
    suspend fun updateFlight(flight: FlightEntity)

    @Delete
    suspend fun deleteFlight(flight: FlightEntity)

    @Query("DELETE FROM flights WHERE id = :id")
    suspend fun deleteFlightById(id: Int)
}
