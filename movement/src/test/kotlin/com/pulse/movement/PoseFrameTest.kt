package com.pulse.movement

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PoseFrameTest {

    private fun frameWithKnee(angleHint: String): PoseFrame {
        // Right angle: hip (0,0), knee (0,1), ankle (1,1) gives 90 deg.
        // Straight: hip (0,0), knee (1,0), ankle (2,0) gives 180 deg.
        val joints = if (angleHint == "bent") {
            mapOf(
                "left_hip" to Point2D(0.0, 0.0),
                "left_knee" to Point2D(0.0, 1.0),
                "left_ankle" to Point2D(1.0, 1.0)
            )
        } else {
            mapOf(
                "left_hip" to Point2D(0.0, 0.0),
                "left_knee" to Point2D(1.0, 0.0),
                "left_ankle" to Point2D(2.0, 0.0)
            )
        }
        val landmarks = List(33) { Landmark(visibility = 0.9f) }
        return PoseFrame(timeMs = 0L, landmarks = landmarks, joints = joints)
    }

    @Test
    fun bentKneeGivesSmallAngle() {
        val angle = frameWithKnee("bent").angleFor(Exercise.SQUAT)
        assertNotNull(angle)
        assertTrue(angle!! < 100.0)
    }

    @Test
    fun missingJointsGiveNull() {
        val landmarks = List(33) { Landmark(visibility = 0.9f) }
        val frame = PoseFrame(timeMs = 0L, landmarks = landmarks, joints = emptyMap())
        assertNull(frame.angleFor(Exercise.SQUAT))
    }

    @Test
    fun fakeDetectorReplaysFrames() {
        FakePoseDetector(listOf(frameWithKnee("bent"), null)).use { detector ->
            assertEquals("fake", detector.sourceName())
            assertNotNull(detector.nextFrame())
            assertNull(detector.nextFrame())
        }
    }

    @Test
    fun throttlerDropsFastFrames() {
        val gate = FrameThrottler(minIntervalMs = 50L)
        assertTrue(gate.shouldAccept(0L))
        assertFalse(gate.shouldAccept(10L))
        assertTrue(gate.shouldAccept(60L))
        assertEquals(2, gate.accepted)
        assertEquals(1, gate.dropped)
    }

    @Test
    fun throttlerDropsWhenBusy() {
        val gate = FrameThrottler()
        gate.setBusy(true)
        assertFalse(gate.shouldAccept(1000L))
        assertEquals(1, gate.dropped)
    }
}
