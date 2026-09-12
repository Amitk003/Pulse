package com.pulse.movement

/**
 * One analyzed camera frame in movement friendly form.
 *
 * The Android MediaPipe wrapper builds this object from a
 * PoseLandmarker result. The movement core only sees this small
 * model, never MediaPipe or CameraX types.
 *
 * @param timeMs frame time in milliseconds, used for throttling.
 * @param landmarks 33 visibility values for the full body gate.
 * @param joints named 2D points used for angle math. Expected keys:
 * left_hip, left_knee, left_ankle, left_shoulder, left_elbow,
 * left_wrist. Any missing key means that joint is unclear.
 */
data class PoseFrame(
    val timeMs: Long,
    val landmarks: List<Landmark>,
    val joints: Map<String, Point2D>
) {
    /**
     * Joint angle for the given exercise, or null when points are missing.
     * Evaluates bilateral sides (left vs right) and selects the side with valid points.
     */
    fun angleFor(exercise: Exercise): Double? {
        val (leftKeys, rightKeys) = when (exercise) {
            Exercise.SQUAT, Exercise.LUNGE -> Pair(
                Triple("left_hip", "left_knee", "left_ankle"),
                Triple("right_hip", "right_knee", "right_ankle")
            )
            Exercise.PUSH_UP -> Pair(
                Triple("left_shoulder", "left_elbow", "left_wrist"),
                Triple("right_shoulder", "right_elbow", "right_wrist")
            )
            Exercise.HINGE -> Pair(
                Triple("left_shoulder", "left_hip", "left_knee"),
                Triple("right_shoulder", "right_hip", "right_knee")
            )
            Exercise.JUMPING_JACK -> Pair(
                Triple("left_hip", "left_shoulder", "left_elbow"),
                Triple("right_hip", "right_shoulder", "right_elbow")
            )
        }

        // Try left side first
        val leftAngle = calculateTripleAngle(leftKeys)
        if (leftAngle != null) {
            return leftAngle
        }
        // Fallback to right side
        return calculateTripleAngle(rightKeys)
    }

    private fun calculateTripleAngle(keys: Triple<String, String, String>): Double? {
        val first = joints[keys.first] ?: return null
        val middle = joints[keys.second] ?: return null
        val last = joints[keys.third] ?: return null
        return Angles.jointAngle(first, middle, last)
    }
}
