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
    fun fullSetFinishesWithValidAiResult() {
        val config = MovementConfig(smoothingWindow = 1, minFramesInState = 1)
        val session = SetSession(Exercise.SQUAT, config)
        var time = 1000L

        // Supply a valid, fresh AI analysis result
        val aiResult = AiAnalysisResult(
            exercise = "squat",
            personVisible = true,
            bodyVisibility = BodyVisibility.FULL_BODY_VISIBLE,
            cameraUsable = true,
            exerciseMatch = true,
            currentForm = CurrentForm.GOOD,
            formScore = 90,
            repState = AiRepState.COMPLETED,
            repLikelihood = 0.95,
            timestampMs = time
        )
        session.updateAiResult(aiResult, timestampMs = time)

        // One full squat sequence: stand, down, stand, stand.
        for (knee in listOf(170.0, 85.0, 170.0, 170.0)) {
            session.updateAiResult(aiResult, timestampMs = time)
            session.onFrame(frame(time, knee))
            time += 100L
        }

        assertEquals(1, session.reps)
        val result = session.finish()
        assertTrue(result.isValid())
        assertEquals("squat", result.exercise)
        assertEquals(1, result.reps)
    }

    @Test
    fun couchSittingAnomalyRejectsCandidateRep() {
        val config = MovementConfig(smoothingWindow = 1, minFramesInState = 1)
        val session = SetSession(Exercise.SQUAT, config)
        var time = 1000L

        // AI flags anomaly (sitting on couch)
        val anomalyResult = AiAnalysisResult(
            exercise = "squat",
            personVisible = true,
            bodyVisibility = BodyVisibility.FULL_BODY_VISIBLE,
            exerciseMatch = false,
            anomalyDetected = true,
            anomalyType = "couch_sitting",
            repState = AiRepState.INVALID,
            feedback = "Unrelated movement detected",
            timestampMs = time
        )
        session.updateAiResult(anomalyResult, timestampMs = time)

        for (knee in listOf(170.0, 85.0, 170.0, 170.0)) {
            session.updateAiResult(anomalyResult, timestampMs = time)
            session.onFrame(frame(time, knee))
            time += 100L
        }

        // Rep candidate must be REJECTED due to anomaly
        assertEquals(0, session.reps)
        assertTrue(session.hint.contains("couch_sitting") || session.hint.contains("Unrelated movement"))
    }

    @Test
    fun staleAiResultRejectsCandidateRep() {
        val config = MovementConfig(smoothingWindow = 1, minFramesInState = 1, aiResultMaxAgeMs = 500L)
        val session = SetSession(Exercise.SQUAT, config)

        // Stale AI result timestamped 10 seconds ago
        val staleAiResult = AiAnalysisResult(
            exercise = "squat",
            personVisible = true,
            bodyVisibility = BodyVisibility.FULL_BODY_VISIBLE,
            exerciseMatch = true,
            repState = AiRepState.COMPLETED,
            repLikelihood = 0.9,
            timestampMs = 1000L
        )
        session.updateAiResult(staleAiResult, timestampMs = 1000L)

        // Movement frames run at time = 10000L (> 500ms max age)
        var time = 10000L
        for (knee in listOf(170.0, 85.0, 170.0, 170.0)) {
            session.onFrame(frame(time, knee))
            time += 100L
        }

        // Candidate rep must be REJECTED because AI result is stale
        assertEquals(0, session.reps)
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
