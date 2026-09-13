package com.pulse.movement

/**
 * All tunable movement numbers live here.
 *
 * Keep thresholds in this one file so device tests can tune them
 * without changing the state machine or scoring code.
 * All angles use degrees.
 *
 * Values were adjusted after switching to pose_landmarker_full.task.
 * They are still starting values, not final device-validated numbers.
 */
data class MovementConfig(
    // Squat knee angles.
    // Tighter tolerance for more accurate rep detection
    val squatStartDeg: Double = 165.0,
    val squatStartToleranceDeg: Double = 12.0,
    val squatDownDeg: Double = 90.0,
    // Push-up elbow angles.
    val pushUpStartDeg: Double = 155.0,
    val pushUpStartToleranceDeg: Double = 15.0,
    val pushUpDownDeg: Double = 85.0,
    // Hinge hip angles.
    val hingeStartDeg: Double = 165.0,
    val hingeStartToleranceDeg: Double = 12.0,
    val hingeDownDeg: Double = 115.0,
    // Form signal thresholds. Tighter for better form scoring.
    val valgusKneeRatio: Double = 0.70,
    val squatBackRoundKneeDeg: Double = 95.0,
    val hingeRoundedBackHipDeg: Double = 100.0,
    val pushUpSagBodyDeg: Double = 150.0,
    // Smoothing and stability. Larger window = smoother but more lag.
    val smoothingWindow: Int = 4,
    val minFramesInState: Int = 2,
    // Visibility gate. Higher = stricter visibility requirements.
    val minVisibleLandmarks: Int = 22,
    val minVisibility: Float = 0.55f
) {
    init {
        require(smoothingWindow in 1..15) { "smoothingWindow must be 1..15" }
        require(minFramesInState in 1..30) { "minFramesInState must be 1..30" }
        require(minVisibleLandmarks in 1..33) { "minVisibleLandmarks must be 1..33" }
    }
}
