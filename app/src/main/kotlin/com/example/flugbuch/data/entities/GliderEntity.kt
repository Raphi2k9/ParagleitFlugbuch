package com.example.flugbuch.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gliders")
data class GliderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String   // z.B. "Advance Alpha 7 Größe S"
)
