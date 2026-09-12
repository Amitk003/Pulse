package com.pulse.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsStore by preferencesDataStore("pulse_settings")

/**
 * Small settings outside the database: gym code for gym boards
 * and theme choice for the weather themes later. Empty gym code
 * means global boards. Theme "auto" follows weather when ready.
 */
class PulseSettings(context: Context) {

    private val store = context.applicationContext.settingsStore

    val gymCode: Flow<String> = store.data.map { it[GYM_CODE] ?: "" }

    suspend fun setGymCode(value: String) {
        store.edit { it[GYM_CODE] = value.trim() }
    }

    val theme: Flow<String> = store.data.map { it[THEME] ?: "auto" }

    suspend fun setTheme(value: String) {
        store.edit { it[THEME] = value }
    }

    companion object {
        private val GYM_CODE = stringPreferencesKey("gym_code")
        private val THEME = stringPreferencesKey("theme")
    }
}
