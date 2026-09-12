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
 * left_wrist. Optional keys for front-view checks: right_hip,
 * right_knee, right_ankle, right_shoulder, right_elbow, right_wrist.
 * Any missing key means that joint is unclear.
 */
data class PoseFrame(
    val timeMs: Long,
    val landmarks: List<Landmark>,
    val joints: Map<String, Point2D>
) {
    /**
     * Joint angle for the given exercise, or null when points are missing.
     * Squat uses hip-knee-ankle. Push-up uses shoulder-elbow-wrist.
     * Hinge uses shoulder-hip-knee.
     */
    fun angleFor(exercise: Exercise): Double? {
        val triple = when (exercise) {
            Exercise.SQUAT -> Triple("left_hip", "left_knee", "left_ankle")
            Exercise.PUSH_UP -> Triple("left_shoulder", "left_elbow", "left_wrist")
            Exercise.HINGE -> Triple("left_shoulder", "left_hip", "left_knee")
        }
        val first = joints[triple.first] ?: return null
        val middle = joints[triple.second] ?: return null
        val last = joints[triple.third] ?: return null
        return Angles.jointAngle(first, middle, last)
    }
}
