package com.pulse.movement

/**
 * All tunable movement numbers live here.
 *
 * Keep thresholds in this one file so device tests can tune them
 * without changing the state machine or scoring code.
 * All angles use degrees.
 */
data class MovementConfig(
    // Squat knee angles.
    val squatStartDeg: Double = 170.0,
    val squatStartToleranceDeg: Double = 15.0,
    val squatDownDeg: Double = 95.0,
    // Push-up elbow angles.
    val pushUpStartDeg: Double = 160.0,
    val pushUpStartToleranceDeg: Double = 18.0,
    val pushUpDownDeg: Double = 90.0,
    // Hinge hip angles.
    val hingeStartDeg: Double = 170.0,
    val hingeStartToleranceDeg: Double = 15.0,
    val hingeDownDeg: Double = 120.0,
    // Form signal thresholds. Starting values for device tuning.
    val valgusKneeRatio: Double = 0.75,
    val squatBackRoundKneeDeg: Double = 100.0,
    val hingeRoundedBackHipDeg: Double = 105.0,
    val pushUpSagBodyDeg: Double = 155.0,
    // Smoothing and stability.
    val smoothingWindow: Int = 5,
    val minFramesInState: Int = 3,
    // Visibility gate. See VisibilityGate for details.
    val minVisibleLandmarks: Int = 20,
    val minVisibility: Float = 0.5f
) {
    init {
        require(smoothingWindow in 1..15) { "smoothingWindow must be 1..15" }
        require(minFramesInState in 1..30) { "minFramesInState must be 1..30" }
        require(minVisibleLandmarks in 1..33) { "minVisibleLandmarks must be 1..33" }
    }
}
