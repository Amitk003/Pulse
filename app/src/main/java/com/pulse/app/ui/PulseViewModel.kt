package com.pulse.app.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.pulse.data.CloneState
import com.pulse.data.CloneStateManager
import com.pulse.data.LevelUpInfo
import com.pulse.data.PulseRepository
import com.pulse.data.WeeklyStats
import com.pulse.data.SessionEntity
import com.pulse.movement.SetResult
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * UI state holder. Reads only from PulseRepository; all business rules
 * stay in the data and movement modules.
 */
class PulseViewModel(private val repository: PulseRepository) : ViewModel() {

    val clone: StateFlow<CloneState> = repository.cloneFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CloneStateManager.fresh())

    val weekly: StateFlow<WeeklyStats> = repository.weeklyStats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WeeklyStats(listOf(false, false, false, false, false, false, false), 0, 0, 0))

    val recentSessions: StateFlow<List<SessionEntity>> = repository.recentSessionsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Save one finished set. The callback delivers LevelUpInfo on the
     * main thread so the complete screen can animate the rewards.
     */
    fun saveResult(result: SetResult, onDone: (LevelUpInfo) -> Unit) {
        viewModelScope.launch {
            val info = repository.saveResult(result)
            onDone(info)
        }
    }

    /** Debug helper, kept behind the debug section. */
    fun simulateBreak(days: Int = 8) {
        viewModelScope.launch { repository.shiftLastTrainedBy(days) }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                PulseViewModel(PulseRepository(context.applicationContext))
            }
        }
    }
}
