package com.pulse.movement

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FormScorerTest {

    @Test
    fun goodSquatScoresHighWithNoMistakes() {
        val result = FormScorer.score(
            Exercise.SQUAT,
            listOf(FormScorer.RepObservation(minAngleDeg = 90.0))
        )
        assertEquals(100, result.score)
        assertTrue(result.mistakes.isEmpty())
    }

    @Test
    fun shallowSquatAddsMistake() {
        val result = FormScorer.score(
            Exercise.SQUAT,
            listOf(FormScorer.RepObservation(minAngleDeg = 130.0))
        )
        assertTrue(result.mistakes.contains("shallow"))
        assertTrue(result.score < 100)
    }

    @Test
    fun emptySetScoresZero() {
        val result = FormScorer.score(Exercise.SQUAT, emptyList())
        assertEquals(0, result.score)
    }
}
