package com.pulse.movement

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AnglesTest {

    @Test
    fun straightLineGives180Degrees() {
        val angle = Angles.jointAngle(
            Point2D(0.0, 0.0),
            Point2D(1.0, 0.0),
            Point2D(2.0, 0.0)
        )
        assertNotNull(angle)
        assertEquals(180.0, angle!!, 0.5)
    }

    @Test
    fun rightAngleGives90Degrees() {
        val angle = Angles.jointAngle(
            Point2D(0.0, 0.0),
            Point2D(0.0, 1.0),
            Point2D(1.0, 1.0)
        )
        assertNotNull(angle)
        assertEquals(90.0, angle!!, 0.5)
    }

    @Test
    fun zeroLengthSideGivesNull() {
        val angle = Angles.jointAngle(
            Point2D(1.0, 1.0),
            Point2D(1.0, 1.0),
            Point2D(2.0, 2.0)
        )
        assertNull(angle)
    }

    @Test
    fun smoothingSkipsNullValues() {
        val result = Angles.smoothed(listOf(170.0, null, 168.0), 3)
        assertNotNull(result)
        assertEquals(169.0, result!!, 0.001)
    }

    @Test
    fun smoothingEmptyListGivesNull() {
        assertNull(Angles.smoothed(emptyList(), 3))
    }
}
