// TrailEntity.kt
/*
 * Entità Room per segmenti di sentiero (coordinate di punti consecutivi).
 */
package com.example.treknavigator

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trail_segments")
data class TrailEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val startLat: Double,
    val startLon: Double,
    val endLat: Double,
    val endLon: Double
)
