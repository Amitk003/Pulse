package com.pulse.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One finished exercise set. Stores only small numbers, never video
 * or landmarks. Mistakes stay as fixed lower-case ids joined by
 * commas so old rows keep working when new mistakes are added.
 */
@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val exercise: String,
    val reps: Int,
    val formScore: Int,
    val mistakesCsv: String,
    val durationSec: Int,
    val timestamp: Long,
    val flagged: Boolean = false
)
