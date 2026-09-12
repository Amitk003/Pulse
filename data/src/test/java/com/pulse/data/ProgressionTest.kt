package com.pulse.data

import com.pulse.movement.SetResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class ProgressionTest {

    private val day = DecayManager.DAY_MS

    private fun result(reps: Int = 10, score: Int = 80): SetResult {
        return SetResult(
            exercise = "squat",
            reps = reps,
            formScore = score,
            mistakes = listOf("shallow"),
            durationSec = 60
        )
    }

    @Test
    fun firstSessionEarnsPlainXp() {
        val (next, info) = CloneStateManager.applySession(
            CloneStateManager.fresh(), result(), 1_000L, emptyList()
        )
        assertEquals(800, info.xpGained)
        assertEquals(800, next.xp)
        assertEquals(2, next.level)
        assertTrue(info.leveledUp)
        assertFalse(info.comebackApplied)
        assertFalse(info.decayed)
        assertTrue(abs(next.strength - 18f) < 0.001f)
    }

    @Test
    fun shortBreakKeepsStrength() {
        val state = CloneStateManager.fresh().copy(strength = 20f, lastTrainedAt = 1_000L)
        val (next, info) = CloneStateManager.applySession(
            state, result(), 1_000L + 6 * day, emptyList()
        )
        assertFalse(info.decayed)
        assertTrue(next.strength > 20f)
        assertFalse(info.comebackApplied)
    }

    @Test
    fun longBreakDecaysThenArmsComeback() {
        val state = CloneStateManager.fresh().copy(strength = 20f, lastTrainedAt = 1_000L)
        val now = 1_000L + 10 * day
        val (next, info) = CloneStateManager.applySession(state, result(), now, emptyList())
        assertTrue(info.decayed)
        val kept = 20f * 0.995f * 0.995f * 0.995f
        assertTrue(abs(next.strength - (kept + 800 * 1.75f * 0.01f)) < 0.01f)
        assertTrue(info.comebackApplied)
        assertEquals(1400, info.xpGained)
        assertEquals(1, next.comebackLeft)
        assertEquals(now, next.lastTrainedAt)
    }

    @Test
    fun weakSessionKeepsComebackAlive() {
        val state = CloneStateManager.fresh().copy(comebackLeft = 1, lastTrainedAt = 1_000L)
        val (next, info) = CloneStateManager.applySession(
            state, result(score = 40), 2_000L, emptyList()
        )
        assertTrue(info.comebackApplied)
        assertEquals(1, next.comebackLeft)
    }

    @Test
    fun mistakeMemoryKeepsLastTen() {
        var state = CloneStateManager.fresh()
        for (i in 1..12) {
            val (next, _) = CloneStateManager.applySession(
                state, result().copy(mistakes = listOf("m$i")), i * 1_000L, emptyList()
            )
            state = next
        }
        assertEquals(10, state.mistakeMemory.size)
        assertEquals(listOf("m3"), state.mistakeMemory.first())
        assertEquals(listOf("m12"), state.mistakeMemory.last())
    }

    @Test
    fun secondaryStatsFollowHistory() {
        val now = 10 * day
        val history = listOf(
            SessionInput(formScore = 80, timestamp = now - 2 * day),
            SessionInput(formScore = 60, timestamp = now - day)
        )
        val (next, _) = CloneStateManager.applySession(
            CloneStateManager.fresh(), result(), now, history
        )
        assertEquals(3 / 5f, next.consistency, 0.001f)
        assertEquals((80 + 60 + 80) / 3f, next.formMastery, 0.001f)
        assertEquals(1f, next.recovery, 0.001f)
    }

    @Test
    fun backToBackSessionsLowerRecovery() {
        val now = 10 * day
        val history = listOf(SessionInput(formScore = 80, timestamp = now - 3_600_000L))
        val (next, _) = CloneStateManager.applySession(
            CloneStateManager.fresh(), result(), now, history
        )
        assertTrue(next.recovery < 0.1f)
    }

    @Test
    fun memoryTextRoundTrip() {
        val memory = listOf(listOf("shallow"), listOf("knee-valgus", "back-round"))
        val decoded = StatsCalculator.decodeMemory(StatsCalculator.encodeMemory(memory))
        assertEquals(memory, decoded)
        assertTrue(StatsCalculator.decodeMemory("").isEmpty())
    }

    @Test
    fun decayMathAlone() {
        val state = CloneStateManager.fresh().copy(strength = 100f, lastTrainedAt = 0L)
        assertEquals(state, DecayManager.applyDecay(state, 30 * day))
        val trained = state.copy(lastTrainedAt = 1_000L)
        assertEquals(trained, DecayManager.applyDecay(trained, 1_000L + 7 * day))
        val decayed = DecayManager.applyDecay(trained, 1_000L + 9 * day)
        assertTrue(abs(decayed.strength - 100f * 0.995f * 0.995f) < 0.01f)
    }
}
