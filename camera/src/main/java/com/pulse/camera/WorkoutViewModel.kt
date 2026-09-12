package com.pulse.camera

import android.content.Context
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import com.pulse.movement.Exercise
import com.pulse.movement.MovementConfig
import com.pulse.movement.WorkoutUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Pure Kotlin ViewModel managing Jetpack Compose UI state and workout set recording.
 */
class WorkoutViewModel(
    private val detector: MediapipePoseDetector = MediapipePoseDetector(),
    private val config: MovementConfig = MovementConfig(),
    private val aiAnalyzer: GeminiFormAnalyzer = GeminiFormAnalyzer()
) : ViewModel() {

    private val cameraCapture = CameraCapture(detector, config, aiAnalyzer)

    private val _uiState = MutableStateFlow(WorkoutUiState())
    val uiState: StateFlow<WorkoutUiState> = _uiState.asStateFlow()

    fun selectExercise(exercise: Exercise) {
        if (_uiState.value.isRecording) return
        _uiState.update { it.copy(exercise = exercise) }
    }

    fun startWorkout(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView
    ) {
        val selectedExercise = _uiState.value.exercise
        _uiState.update { it.copy(isRecording = true, reps = 0, candidateReps = 0) }

        cameraCapture.start(
            context = context,
            owner = lifecycleOwner,
            previewView = previewView,
            exercise = selectedExercise
        ) { reps, hint ->
            val aiResult = cameraCapture.stats() // Analyzer stats or session state
            _uiState.update { current ->
                current.copy(
                    reps = reps,
                    hint = hint
                )
            }
        }
    }

    fun stopWorkout() {
        cameraCapture.stop()
        _uiState.update { it.copy(isRecording = false) }
    }

    override fun onCleared() {
        super.onCleared()
        cameraCapture.close()
    }
}
