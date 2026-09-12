package com.pulse.data

import com.pulse.movement.SetResult

/** Clone progress in plain values. The database maps to and from this. */
data class CloneState(
    val strength: Float,
    val xp: Int,
    val level: Int,
    val consistency: Float,
    val formMastery: Float,
    val recovery: Float,
    val lastTrainedAt: Long,
    val mistakeMemory: List<List<String>>,
    val comebackLeft: Int
)

/** What one saved set earned, for the result screen. */
data class LevelUpInfo(
    val xpGained: Int,
    val leveledUp: Boolean,
    val newLevel: Int,
    val comebackApplied: Boolean,
    val decayed: Boolean
)

/** Small history row the progress math needs. */
data class SessionInput(
    val formScore: Int,
    val timestamp: Long
)

/**
 * Turns a finished set into clone progress. Pure logic, no Android code.
 *
 * Order per set: soft decay first, then comeback check, then XP. A break
 * of 7 or more days arms 2 comeback sessions at 1.75 times XP. Only
 * quality sets with score 60 or more spend a comeback session.
 */
object CloneStateManager {

    const val XP_PER_LEVEL = 500
    const val COMEBACK_MULT = 1.75
    const val COMEBACK_SESSIONS = 2
    const val STRENGTH_PER_XP = 0.01f
    const val QUALITY_SCORE = 60

    fun fresh(): CloneState {
        return CloneState(
            strength = 10f,
            xp = 0,
            level = 1,
            consistency = 0f,
            formMastery = 0f,
            recovery = 1f,
            lastTrainedAt = 0L,
            mistakeMemory = emptyList(),
            comebackLeft = 0
        )
    }

    fun applySession(
        state: CloneState,
        result: SetResult,
        now: Long,
        history: List<SessionInput>
    ): Pair<CloneState, LevelUpInfo> {
        val rested = DecayManager.applyDecay(state, now)
        val decayed = rested.strength != state.strength

        var current = rested
        val longBreak = state.lastTrainedAt > 0L &&
            DecayManager.daysSince(state.lastTrainedAt, now) >= DecayManager.DECAY_START_DAYS
        if (longBreak && current.comebackLeft == 0) {
            current = current.copy(comebackLeft = COMEBACK_SESSIONS)
        }

        val comeback = current.comebackLeft > 0
        val xpGained = (result.reps * result.formScore * if (comeback) COMEBACK_MULT else 1.0).toInt()
        val xp = current.xp + xpGained
        val level = xp / XP_PER_LEVEL + 1

        var left = current.comebackLeft
        if (comeback && result.formScore >= QUALITY_SCORE) {
            left -= 1
        }

        val weekStart = now - 7 * DecayManager.DAY_MS
        val weekCount = history.count { it.timestamp >= weekStart } + 1
        val scores = (history.map { it.formScore } + result.formScore).takeLast(10)
        val stamps = (history.map { it.timestamp } + now).sorted()
        val memory = (current.mistakeMemory + listOf(result.mistakes)).takeLast(10)

        val next = current.copy(
            strength = current.strength + xpGained * STRENGTH_PER_XP,
            xp = xp,
            level = level,
            consistency = StatsCalculator.consistency(weekCount),
            formMastery = StatsCalculator.formMastery(scores),
            recovery = StatsCalculator.recovery(StatsCalculator.averageGapHours(stamps)),
            lastTrainedAt = now,
            mistakeMemory = memory,
            comebackLeft = left
        )
        val info = LevelUpInfo(
            xpGained = xpGained,
            leveledUp = level > current.level,
            newLevel = level,
            comebackApplied = comeback,
            decayed = decayed
        )
        return next to info
    }
}
