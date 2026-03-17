package com.example.flugbuch.data.repository

import com.example.flugbuch.data.dao.FlightDao
import com.example.flugbuch.data.dao.GliderDao
import com.example.flugbuch.data.entities.FlightEntity
import com.example.flugbuch.data.entities.GliderEntity
import kotlinx.coroutines.flow.Flow

class FlightRepository(
    private val flightDao: FlightDao,
    private val gliderDao: GliderDao
) {
    // Flights
    val allFlights: Flow<List<FlightEntity>> = flightDao.getAllFlights()
    val totalFlightMinutes: Flow<Int?> = flightDao.getTotalFlightMinutes()
    val totalFlightCount: Flow<Int> = flightDao.getTotalFlightCount()

    fun getFlightsByType(type: String): Flow<List<FlightEntity>> =
        flightDao.getFlightsByType(type)

    fun getFlightsByGlider(gliderName: String): Flow<List<FlightEntity>> =
        flightDao.getFlightsByGlider(gliderName)

    fun getFlightCountByType(type: String): Flow<Int> =
        flightDao.getFlightCountByType(type)

    suspend fun getFlightById(id: Int): FlightEntity? =
        flightDao.getFlightById(id)

    suspend fun getAllFlightsOnce(): List<FlightEntity> =
        flightDao.getAllFlightsOnce()

    suspend fun insertFlight(flight: FlightEntity): Long {
        // Schirm automatisch speichern, falls neu
        if (flight.gliderName.isNotBlank()) {
            val existing = gliderDao.getGliderByName(flight.gliderName)
            if (existing == null) {
                gliderDao.insertGlider(GliderEntity(name = flight.gliderName))
            }
        }
        return flightDao.insertFlight(flight)
    }

    suspend fun updateFlight(flight: FlightEntity) {
        if (flight.gliderName.isNotBlank()) {
            val existing = gliderDao.getGliderByName(flight.gliderName)
            if (existing == null) {
                gliderDao.insertGlider(GliderEntity(name = flight.gliderName))
            }
        }
        flightDao.updateFlight(flight)
    }

    suspend fun deleteFlight(flight: FlightEntity) =
        flightDao.deleteFlight(flight)

    suspend fun deleteFlightById(id: Int) =
        flightDao.deleteFlightById(id)

    // Gliders
    val allGliders: Flow<List<GliderEntity>> = gliderDao.getAllGliders()

    suspend fun getAllGlidersOnce(): List<GliderEntity> =
        gliderDao.getAllGlidersOnce()

    suspend fun insertGlider(glider: GliderEntity): Long =
        gliderDao.insertGlider(glider)

    suspend fun deleteGlider(glider: GliderEntity) =
        gliderDao.deleteGlider(glider)

    // Bulk import
    suspend fun importFlights(flights: List<FlightEntity>) {
        flights.forEach { insertFlight(it.copy(id = 0)) }
    }
}
