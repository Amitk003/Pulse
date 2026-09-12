package com.pulse.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * The game clone. Exactly one row with id 1. Strength and level grow
 * from real training. The last 10 sessions of mistakes are kept as
 * text lines so the coach can use them later. Lines are split by
 * newline, ids inside one line by comma. Ids never hold those marks.
 */
@Entity(tableName = "clone")
data class CloneEntity(
    @PrimaryKey val id: Int = 1,
    val strength: Float,
    val xp: Int,
    val level: Int,
    val consistency: Float,
    val formMastery: Float,
    val recovery: Float,
    val lastTrainedAt: Long,
    val mistakeMemory: String,
    val comebackLeft: Int
)
