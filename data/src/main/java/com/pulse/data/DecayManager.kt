package com.pulse.data

import kotlin.math.pow

/**
 * Soft strength loss after long breaks. Pure math, no Android code.
 *
 * Nothing happens for the first 7 days. After that each extra day
 * keeps 99.5 percent of strength. A state that never trained is
 * left alone so the first session starts clean.
 */
object DecayManager {

    const val DECAY_START_DAYS = 7L
    const val DECAY_RATE = 0.995
    const val DAY_MS = 86_400_000L

    fun daysSince(lastTrainedAt: Long, now: Long): Long {
        if (lastTrainedAt <= 0L || now < lastTrainedAt) {
            return 0L
        }
        return (now - lastTrainedAt) / DAY_MS
    }

    fun applyDecay(state: CloneState, now: Long): CloneState {
        val extra = daysSince(state.lastTrainedAt, now) - DECAY_START_DAYS
        if (extra <= 0L) {
            return state
        }
        val kept = DECAY_RATE.pow(extra.toDouble()).toFloat()
        return state.copy(strength = state.strength * kept)
    }
}
