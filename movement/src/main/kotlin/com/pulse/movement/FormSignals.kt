package com.pulse.movement

/**
 * Per-frame form signals from joint geometry.
 *
 * All functions are pure and need no camera or Android types.
 * Any missing joint returns false so an unclear frame can never
 * accuse the user of bad form. Thresholds stay in [MovementConfig].
 *
 * Coordinate notes: image space, x right, y down, any single scale.
 * Left and right leg points are only used together, so front, side,
 * and diagonal views behave safely. Side views keep both legs
 * overlapped, which keeps the knee-over-toe ratio near 1.
 */
object FormSignals {

    /** Faults seen in one frame. Only true values are latched by SetSession. */
    data class FrameFaults(
        val kneeValgus: Boolean = false,
        val backRound: Boolean = false,
        val sagging: Boolean = false
    )

    /**
     * Faults for one visible frame of the given exercise.
     * [angleDeg] is the smoothed counting angle, or null when unclear.
     */
    fun faultsFor(
        exercise: Exercise,
        joints: Map<String, Point2D>,
        angleDeg: Double?,
        config: MovementConfig = MovementConfig()
    ): FrameFaults {
        return when (exercise) {
            Exercise.SQUAT -> FrameFaults(
                kneeValgus = kneeValgus(joints, config),
                backRound = squatBackRound(joints, angleDeg, config)
            )
            Exercise.HINGE -> FrameFaults(
                backRound = hingeRoundedBack(joints, angleDeg, config)
            )
            Exercise.PUSH_UP -> FrameFaults(
                sagging = pushUpSagging(joints, config)
            )
        }
    }

    /**
     * Knee valgus from the front view: knees clearly narrower than
     * ankles means the knees cave inward. Needs both legs, so this
     * only runs when the camera sees the full front view.
     */
    fun kneeValgus(joints: Map<String, Point2D>, config: MovementConfig): Boolean {
        val leftKnee = joints["left_knee"] ?: return false
        val rightKnee = joints["right_knee"] ?: return false
        val leftAnkle = joints["left_ankle"] ?: return false
        val rightAnkle = joints["right_ankle"] ?: return false
        val interAnkle = kotlin.math.abs(leftAnkle.x - rightAnkle.x)
        if (interAnkle <= 1e-6) {
            return false
        }
        val interKnee = kotlin.math.abs(leftKnee.x - rightKnee.x)
        return interKnee < config.valgusKneeRatio * interAnkle
    }

    /**
     * Squat back-round proxy: shoulders dropped below the hips while
     * the knees are not deeply bent. A deep rep may lean, so the knee
     * angle guard keeps honest deep squats from flagging.
     */
    fun squatBackRound(
        joints: Map<String, Point2D>,
        kneeAngleDeg: Double?,
        config: MovementConfig
    ): Boolean {
        val shoulder = joints["left_shoulder"] ?: return false
        val hip = joints["left_hip"] ?: return false
        if (kneeAngleDeg == null || kneeAngleDeg.isNaN()) {
            return false
        }
        val collapsed = shoulder.y > hip.y
        return collapsed && kneeAngleDeg > config.squatBackRoundKneeDeg
    }

    /**
     * Hinge rounded-back proxy: the torso went horizontal while the
     * hips barely hinged, so the bend came from the spine instead of
     * the hips. A proper deep hinge keeps a small hip angle and passes.
     */
    fun hingeRoundedBack(
        joints: Map<String, Point2D>,
        hipAngleDeg: Double?,
        config: MovementConfig
    ): Boolean {
        val shoulder = joints["left_shoulder"] ?: return false
        val hip = joints["left_hip"] ?: return false
        if (hipAngleDeg == null || hipAngleDeg.isNaN()) {
            return false
        }
        val torsoHorizontal = shoulder.y > hip.y
        return torsoHorizontal && hipAngleDeg > config.hingeRoundedBackHipDeg
    }

    /**
     * Push-up sagging proxy: the shoulder-hip-knee line should stay
     * near straight. Hips clearly dropped (or piked) break the line.
     * Both break the same angle, so both report as sagging for now.
     */
    fun pushUpSagging(joints: Map<String, Point2D>, config: MovementConfig): Boolean {
        val shoulder = joints["left_shoulder"] ?: return false
        val hip = joints["left_hip"] ?: return false
        val knee = joints["left_knee"] ?: return false
        val bodyAngle = Angles.jointAngle(shoulder, hip, knee) ?: return false
        return bodyAngle < config.pushUpSagBodyDeg
    }
}
