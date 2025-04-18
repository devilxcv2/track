// TrailDao.kt
/*
 * DAO per inserimento e query dei segmenti di sentiero.
 */
package com.example.treknavigator

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TrailDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(segments: List<TrailEntity>)

    @Query("SELECT * FROM trail_segments")
    suspend fun getAll(): List<TrailEntity>
}
