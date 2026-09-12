package com.pulse.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Reads and writes for the single clone row. */
@Dao
interface CloneDao {

    @Query("SELECT * FROM clone WHERE id = 1")
    suspend fun get(): CloneEntity?

    @Query("SELECT * FROM clone WHERE id = 1")
    fun watch(): Flow<CloneEntity?>

    @Upsert
    suspend fun save(clone: CloneEntity)

    @Query("UPDATE clone SET lastTrainedAt = :timestamp WHERE id = 1")
    suspend fun moveLastTrained(timestamp: Long)
}
