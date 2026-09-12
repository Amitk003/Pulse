package com.pulse.movement

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SetSessionTest {

    private fun jointsFor(knee: Double): Map<String, Point2D> {
        // Build a fake knee angle at the knee joint. Hip stays fixed and
        // the ankle is placed so the hip-knee-ankle angle equals [knee].
        // Vector knee->hip is (0, -1), so ankle offset (sin, -cos) gives
        // cos(angle) = -dy / len and the exact wanted angle.
        val rad = Math.toRadians(knee)
        return mapOf(
            "left_hip" to Point2D(0.0, 0.0),
            "left_knee" to Point2D(0.0, 1.0),
            "left_ankle" to Point2D(Math.sin(rad), 1.0 - Math.cos(rad))
        )
    }

    private fun frame(timeMs: Long, knee: Double, visible: Boolean = true): PoseFrame {
        val landmarks = List(33) { Landmark(visibility = if (visible) 0.9f else 0.1f) }
        return PoseFrame(timeMs = timeMs, landmarks = landmarks, joints = jointsFor(knee))
    }

    private fun frontFrame(timeMs: Long, joints: Map<String, Point2D>): PoseFrame {
        val landmarks = List(33) { Landmark(visibility = 0.9f) }
        return PoseFrame(timeMs = timeMs, landmarks = landmarks, joints = joints)
    }

    /**
     * Front-view squat joints with both legs. Standing frames are
     * straight and symmetric. The bent frame keeps a 90 degree left
     * knee while both knees cave far inside the ankles.
     */
    private fun frontSquatJoints(bent: Boolean): Map<String, Point2D> {
        return if (!bent) {
            mapOf(
                "left_hip" to Point2D(0.0, 0.0),
                "left_knee" to Point2D(-0.15, 1.0),
                "left_ankle" to Point2D(-0.15, 1.98),
                "right_knee" to Point2D(0.15, 1.0),
                "right_ankle" to Point2D(0.15, 1.98)
            )
        } else {
            mapOf(
                "left_hip" to Point2D(0.0, 0.0),
                "left_knee" to Point2D(0.1, 1.0),
                "left_ankle" to Point2D(1.095, 0.9),
                "right_knee" to Point2D(-0.1, 1.0),
                "right_ankle" to Point2D(-1.095, 0.9)
            )
        }
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
    fun blockedFramesDoNotCount() {        val config = MovementConfig(smoothingWindow = 1, minFramesInState = 1)
        val session = SetSession(Exercise.SQUAT, config)
        session.onFrame(frame(0L, 170.0, visible = true))
        session.onFrame(frame(500L, 85.0, visible = false))
        // The setup hint shows while the view is bad ...
        assertTrue(session.blockedFrames > 0)
        assertTrue(session.hint.isNotBlank())
        // ... and clears once the view is fine, without counting a rep.
        session.onFrame(frame(1000L, 170.0, visible = true))
        session.onFrame(frame(1500L, 170.0, visible = true))
        assertEquals(0, session.reps)
        assertTrue(session.blockedFrames > 0)
    }

    @Test
    fun cavedKneesFlowIntoResultMistakes() {
        val config = MovementConfig(smoothingWindow = 1, minFramesInState = 1)
        val session = SetSession(Exercise.SQUAT, config)
        var time = 0L
        // One full squat whose bent frame caves both knees inward.
        for (bent in listOf(false, true, false, false)) {
            session.onFrame(frontFrame(time, frontSquatJoints(bent)))
            time += 500L
        }
        assertEquals(1, session.reps)
        val result = session.finish()
        assertTrue(result.isValid())
        assertEquals(listOf("knee-valgus"), result.mistakes)
    }
}
