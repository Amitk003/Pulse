package com.pulse.movement

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VisibilityGateTest {

    private fun landmarks(visible: Int, total: Int = 33): List<Landmark> {
        val out = mutableListOf<Landmark>()
        for (index in 0 until total) {
            if (index < visible) {
                out.add(Landmark(visibility = 0.9f))
            } else {
                out.add(Landmark(visibility = 0.1f))
            }
        }
        return out
    }

    @Test
    fun fullBodyPassesGate() {
        val result = VisibilityGate.check(landmarks(visible = 28))
        assertTrue(result.ok)
        assertEquals(28, result.visibleCount)
    }

    @Test
    fun partialBodyFailsGateWithHelpMessage() {
        val result = VisibilityGate.check(landmarks(visible = 10))
        assertFalse(result.ok)
        assertEquals(VisibilityProblem.BODY_PARTLY_OUTSIDE_VIEW, result.problem)
        assertTrue(result.message.isNotBlank())
    }

    @Test
    fun shortListFailsAsIncompleteData() {
        val result = VisibilityGate.check(landmarks(visible = 5, total = 10))
        assertFalse(result.ok)
        assertEquals(VisibilityProblem.TOO_FEW_LANDMARKS, result.problem)
    }
}
