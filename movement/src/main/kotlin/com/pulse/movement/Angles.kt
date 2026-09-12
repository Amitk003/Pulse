package com.pulse.movement

import kotlin.math.acos
import kotlin.math.sqrt

/** Simple 2D point in image space. Any scale works as long as all three points share it. */
data class Point2D(val x: Double, val y: Double)

/**
 * Joint angle math for rep counting.
 *
 * All functions are pure. They need no camera, no Android classes,
 * and no stored state, so they are easy to unit test.
 */
object Angles {

    /**
     * Angle at [middle] formed by [first] and [last], in degrees from 0 to 180.
     * Returns 180 when the limb is straight, smaller values when bent.
     * Returns null when any side has zero length.
     */
    fun jointAngle(first: Point2D, middle: Point2D, last: Point2D): Double? {
        val ax = first.x - middle.x
        val ay = first.y - middle.y
        val bx = last.x - middle.x
        val by = last.y - middle.y
        val lengthA = sqrt(ax * ax + ay * ay)
        val lengthB = sqrt(bx * bx + by * by)
        if (lengthA <= 0.0 || lengthB <= 0.0) {
            return null
        }
        val dot = ax * bx + ay * by
        val ratio = (dot / (lengthA * lengthB)).coerceIn(-1.0, 1.0)
        val radians = acos(ratio)
        return Math.toDegrees(radians)
    }

    /**
     * Mean of the last [window] values. Returns null when no valid value exists.
     * Null inputs are skipped so one noisy frame cannot break the signal.
     */
    fun smoothed(values: List<Double?>, window: Int): Double? {
        require(window >= 1) { "window must be at least 1" }
        if (values.isEmpty()) {
            return null
        }
        val recent = values.takeLast(window).filterNotNull()
        if (recent.isEmpty()) {
            return null
        }
        return recent.sum() / recent.size
    }
}
