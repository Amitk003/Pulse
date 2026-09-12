package com.pulse.movement

import org.junit.Assert.assertEquals
import org.junit.Test

class RepCounterTest {

    private fun visible(): VisibilityResult {
        return VisibilityResult(
            ok = true,
            visibleCount = 30,
            problem = VisibilityProblem.NONE,
            message = ""
        )
    }

    private fun blocked(): VisibilityResult {
        return VisibilityResult(
            ok = false,
            visibleCount = 5,
            problem = VisibilityProblem.BODY_PARTLY_OUTSIDE_VIEW,
            message = "Move back so your full body is visible."
        )
    }

    private fun feed(counter: RepCounter, angles: List<Double>) {
        for (angle in angles) {
            counter.onFrame(angle, visible())
        }
    }

    @Test
    fun fullSquatCountsOneRep() {
        val counter = RepCounter(Exercise.SQUAT, MovementConfig(smoothingWindow = 1, minFramesInState = 1))
        feed(counter, listOf(170.0, 90.0, 170.0, 170.0))
        assertEquals(1, counter.reps)
    }

    @Test
    fun halfSquatDoesNotCount() {
        val counter = RepCounter(Exercise.SQUAT, MovementConfig(smoothingWindow = 1, minFramesInState = 1))
        feed(counter, listOf(170.0, 130.0, 170.0, 170.0))
        assertEquals(0, counter.reps)
    }

    @Test
    fun blockedVisibilityDoesNotCount() {
        val counter = RepCounter(Exercise.SQUAT, MovementConfig(smoothingWindow = 1, minFramesInState = 1))
        counter.onFrame(170.0, visible())
        counter.onFrame(90.0, blocked())
        counter.onFrame(170.0, visible())
        counter.onFrame(170.0, visible())
        assertEquals(0, counter.reps)
    }

    @Test
    fun noisyFrameDoesNotDoubleCount() {
        val config = MovementConfig(smoothingWindow = 3, minFramesInState = 2)
        val counter = RepCounter(Exercise.SQUAT, config)
        feed(counter, listOf(170.0, 170.0, 90.0, 92.0, 88.0, 170.0, 170.0, 170.0, 170.0))
        assertEquals(1, counter.reps)
    }

    @Test
    fun fullPushUpCountsOneRep() {
        val counter = RepCounter(Exercise.PUSH_UP, MovementConfig(smoothingWindow = 1, minFramesInState = 1))
        feed(counter, listOf(160.0, 85.0, 160.0, 160.0))
        assertEquals(1, counter.reps)
    }
}
