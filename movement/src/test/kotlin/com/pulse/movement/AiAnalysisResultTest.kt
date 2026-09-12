package com.pulse.movement

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiAnalysisResultTest {

    @Test
    fun validResultPassesValidation() {
        val result = AiAnalysisResult(
            exercise = "squat",
            personVisible = true,
            bodyVisibility = BodyVisibility.FULL_BODY_VISIBLE,
            cameraUsable = true,
            exerciseMatch = true,
            repState = AiRepState.COMPLETED,
            repLikelihood = 0.92,
            anomalyDetected = false,
            timestampMs = 1000L
        )
        assertTrue(result.isValidForRep())
        assertTrue(result.isFresh(1200L, maxAgeMs = 1500L))
    }

    @Test
    fun anomalyRejectsValidation() {
        val result = AiAnalysisResult(
            exercise = "squat",
            personVisible = true,
            bodyVisibility = BodyVisibility.FULL_BODY_VISIBLE,
            exerciseMatch = true,
            repState = AiRepState.COMPLETED,
            anomalyDetected = true,
            anomalyType = "couch_sitting",
            timestampMs = 1000L
        )
        assertFalse(result.isValidForRep())
    }

    @Test
    fun insufficientVisibilityRejectsValidation() {
        val result = AiAnalysisResult(
            exercise = "push-up",
            personVisible = true,
            bodyVisibility = BodyVisibility.INSUFFICIENT,
            exerciseMatch = true,
            repState = AiRepState.COMPLETED,
            timestampMs = 1000L
        )
        assertFalse(result.isValidForRep())
    }

    @Test
    fun staleTimestampFailsFreshCheck() {
        val result = AiAnalysisResult(
            exercise = "squat",
            timestampMs = 1000L
        )
        assertFalse(result.isFresh(3000L, maxAgeMs = 1500L))
    }
}
