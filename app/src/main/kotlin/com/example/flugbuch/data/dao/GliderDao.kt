package com.example.flugbuch.data.dao

import androidx.room.*
import com.example.flugbuch.data.entities.GliderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GliderDao {

    @Query("SELECT * FROM gliders ORDER BY name ASC")
    fun getAllGliders(): Flow<List<GliderEntity>>

    @Query("SELECT * FROM gliders ORDER BY name ASC")
    suspend fun getAllGlidersOnce(): List<GliderEntity>

    @Query("SELECT * FROM gliders WHERE name = :name LIMIT 1")
    suspend fun getGliderByName(name: String): GliderEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertGlider(glider: GliderEntity): Long

    @Delete
    suspend fun deleteGlider(glider: GliderEntity)

    @Query("DELETE FROM gliders WHERE id = :id")
    suspend fun deleteGliderById(id: Int)
}
