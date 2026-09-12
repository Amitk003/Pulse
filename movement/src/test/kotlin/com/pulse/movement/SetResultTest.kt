package com.pulse.movement

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SetResultTest {

    @Test
    fun validResultPasses() {
        val result = SetResult(
            exercise = "squat",
            reps = 10,
            formScore = 85,
            mistakes = listOf("shallow"),
            durationSec = 60
        )
        assertTrue(result.isValid())
    }

    @Test
    fun badValuesFail() {
        val result = SetResult(
            exercise = "run",
            reps = -1,
            formScore = 120,
            mistakes = listOf("BAD"),
            durationSec = -5
        )
        assertFalse(result.isValid())
    }
}
