package com.pulse.movement

/** One body landmark with a visibility score from 0 to 1. */
data class Landmark(val visibility: Float)

/** Why the camera view is not good enough for counting. */
enum class VisibilityProblem {
    NONE,
    TOO_FEW_LANDMARKS,
    BODY_PARTLY_OUTSIDE_VIEW
}

/** Result of the full-body check. Counting may continue only when [ok] is true. */
data class VisibilityResult(
    val ok: Boolean,
    val visibleCount: Int,
    val problem: VisibilityProblem,
    val message: String
)

/**
 * Full-body visibility gate.
 *
 * Rule: at least [MovementConfig.minVisibleLandmarks] of 33 landmarks
 * must have visibility above [MovementConfig.minVisibility].
 * When the gate fails, the caller must not advance the rep counter.
 */
object VisibilityGate {

    const val TOTAL_LANDMARKS = 33

    fun check(landmarks: List<Landmark>, config: MovementConfig = MovementConfig()): VisibilityResult {
        if (landmarks.size < TOTAL_LANDMARKS) {
            return VisibilityResult(
                ok = false,
                visibleCount = 0,
                problem = VisibilityProblem.TOO_FEW_LANDMARKS,
                message = "Pose data is incomplete. Hold still and try again."
            )
        }
        var visible = 0
        for (item in landmarks) {
            if (item.visibility >= config.minVisibility) {
                visible += 1
            }
        }
        if (visible >= config.minVisibleLandmarks) {
            return VisibilityResult(
                ok = true,
                visibleCount = visible,
                problem = VisibilityProblem.NONE,
                message = ""
            )
        }
        return VisibilityResult(
            ok = false,
            visibleCount = visible,
            problem = VisibilityProblem.BODY_PARTLY_OUTSIDE_VIEW,
            message = "Move back so your full body is visible."
        )
    }
}
