package com.pulse.movement

/**
 * Immutable UI state model consumed by Jetpack Compose screens.
 */
data class WorkoutUiState(
    val exercise: Exercise = Exercise.SQUAT,
    val reps: Int = 0,
    val candidateReps: Int = 0,
    val phase: RepPhase = RepPhase.READY,
    val formScore: Int = 100,
    val hint: String = "",
    val bodyVisibility: BodyVisibility = BodyVisibility.FULL_BODY_VISIBLE,
    val anomalyWarning: String? = null,
    val isAiConnected: Boolean = true,
    val isRecording: Boolean = false
)
