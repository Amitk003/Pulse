package com.pulse.data

import android.content.Context
import androidx.room.Room
import com.pulse.bridge.FightResult
import com.pulse.movement.SetResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * The only door to stored progress. Screens call save and watch,
 * they never touch tables directly. Everything runs offline.
 */
class PulseRepository(context: Context) {

    private val db = Room.databaseBuilder(
        context.applicationContext,
        PulseDatabase::class.java,
        "pulse.db"
    ).build()
    private val sessions = db.sessions()
    private val clones = db.clones()

    /** Live clone state for the UI. Starts fresh when nothing is stored yet. */
    val cloneFlow: Flow<CloneState> = clones.watch().map { entity ->
        entity?.toState() ?: CloneStateManager.fresh()
    }

    /** Live list of recent sessions, newest first. */
    val recentSessionsFlow: Flow<List<SessionEntity>> = sessions.watchRecent(20)

    /**
     * Store one finished set and move the clone forward.
     * Returns what the set earned so the result screen can show it.
     */
    suspend fun saveResult(result: SetResult, now: Long = System.currentTimeMillis()): LevelUpInfo {
        val stored = clones.get()
        val state = stored?.toState() ?: CloneStateManager.fresh()
        val history = sessions.recent(50).map { SessionInput(it.formScore, it.timestamp) }.reversed()
        val (next, info) = CloneStateManager.applySession(state, result, now, history)
        sessions.insert(
            SessionEntity(
                exercise = result.exercise,
                reps = result.reps,
                formScore = result.formScore,
                mistakesCsv = result.mistakes.joinToString(","),
                durationSec = result.durationSec,
                timestamp = now
            )
        )
        clones.save(next.toEntity())
        return info
    }

    /** Debug helper for the demo: pretend the last session was days ago. */
    suspend fun shiftLastTrainedBy(days: Int, now: Long = System.currentTimeMillis()) {
        val current = clones.get()
        if (current == null) {
            val fresh = CloneStateManager.fresh().copy(lastTrainedAt = now - days * DecayManager.DAY_MS)
            clones.save(fresh.toEntity())
        } else {
            clones.moveLastTrained(current.lastTrainedAt - days * DecayManager.DAY_MS)
        }
    }

    /**
     * Apply a fight result returned from Unity.
     *
     * A fair win upgrades attack and defence (strength and formMastery) and pays
     * XP; a draw and a loss pay smaller amounts. Abandoned or early-closed fights
     * earn nothing and never count as a win or a loss. Pulse stays the source of
     * truth for progress.
     *
     * @return the reward applied, or null when the result earned nothing.
     */
    suspend fun applyFightResult(fight: FightResult): FightReward? {
        val reward = FightRewards.fromResult(fight) ?: return null
        val stored = clones.get()
        val state = stored?.toState() ?: CloneStateManager.fresh()
        clones.save(CloneStateManager.applyFightReward(state, reward).toEntity())
        return reward
    }

    /**
     * Pure presentation stats for the home and progress screens, built
     * from real stored sessions. The UI never computes these itself.
     */
    fun weeklyStats(): kotlinx.coroutines.flow.Flow<WeeklyStats> =
        sessions.watchRecent(200).map { rows -> WeeklyStatsBuilder.build(rows, System.currentTimeMillis()) }

    private fun CloneEntity.toState(): CloneState {
        return CloneState(
            strength = strength,
            xp = xp,
            level = level,
            consistency = consistency,
            formMastery = formMastery,
            recovery = recovery,
            lastTrainedAt = lastTrainedAt,
            mistakeMemory = StatsCalculator.decodeMemory(mistakeMemory),
            comebackLeft = comebackLeft
        )
    }

    private fun CloneState.toEntity(): CloneEntity {
        return CloneEntity(
            strength = strength,
            xp = xp,
            level = level,
            consistency = consistency,
            formMastery = formMastery,
            recovery = recovery,
            lastTrainedAt = lastTrainedAt,
            mistakeMemory = StatsCalculator.encodeMemory(mistakeMemory),
            comebackLeft = comebackLeft
        )
    }
}
