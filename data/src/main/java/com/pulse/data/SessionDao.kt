package com.pulse.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

/** Reads and writes for finished sessions. */
@Dao
interface SessionDao {

    @Insert
    suspend fun insert(session: SessionEntity): Long

    @Query("SELECT * FROM sessions ORDER BY timestamp DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<SessionEntity>

    @Query("SELECT * FROM sessions WHERE timestamp >= :since ORDER BY timestamp ASC")
    suspend fun since(since: Long): List<SessionEntity>

    @Query("SELECT COUNT(*) FROM sessions WHERE timestamp >= :since")
    suspend fun countSince(since: Long): Int
}
