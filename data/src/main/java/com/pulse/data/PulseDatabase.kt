package com.pulse.data

import androidx.room.Database
import androidx.room.RoomDatabase

/** Local database. Version 1. Sessions plus the single clone row. */
@Database(entities = [SessionEntity::class, CloneEntity::class], version = 1, exportSchema = false)
abstract class PulseDatabase : RoomDatabase() {

    abstract fun sessions(): SessionDao

    abstract fun clones(): CloneDao
}
