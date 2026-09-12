package com.pulse.movement

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SetSessionTest {

    private fun jointsFor(knee: Double): Map<String, Point2D> {
        // Build a fake knee angle by placing the ankle on a circle
        // around the knee. Hip stays fixed so the angle changes.
        val rad = Math.toRadians(knee)
        return mapOf(
            "left_hip" to Point2D(0.0, 0.0),
            "left_knee" to Point2D(0.0, 1.0),
            "left_ankle" to Point2D(Math.cos(rad), 1.0 + Math.sin(rad))
        )
    }

    private fun frame(timeMs: Long, knee: Double, visible: Boolean = true): PoseFrame {
        val landmarks = List(33) { Landmark(visibility = if (visible) 0.9f else 0.1f) }
        return PoseFrame(timeMs = timeMs, landmarks = landmarks, joints = jointsFor(knee))
    }

    @Test
    fun fullSetFinishesWithValidResult() {
        val config = MovementConfig(smoothingWindow = 1, minFramesInState = 1)
        val session = SetSession(Exercise.SQUAT, config)
        var time = 0L
        // One full squat: stand, down, stand, stand.
        for (knee in listOf(170.0, 85.0, 170.0, 170.0)) {
            session.onFrame(frame(time, knee))
            time += 500L
        }
        assertEquals(1, session.reps)
        val result = session.finish()
        assertTrue(result.isValid())
        assertEquals("squat", result.exercise)
        assertEquals(1, result.reps)
    }

    @Test
    fun blockedFramesDoNotCount() {
        val config = MovementConfig(smoothingWindow = 1, minFramesInState = 1)
        val session = SetSession(Exercise.SQUAT, config)
        session.onFrame(frame(0L, 170.0, visible = true))
        session.onFrame(frame(500L, 85.0, visible = false))
        session.onFrame(frame(1000L, 170.0, visible = true))
        session.onFrame(frame(1500L, 170.0, visible = true))
        assertEquals(0, session.reps)
        assertTrue(session.blockedFrames > 0)
        assertTrue(session.hint.isNotBlank())
    }
}
