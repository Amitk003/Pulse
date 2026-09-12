package com.pulse.app.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.SportsGymnastics
import androidx.compose.ui.graphics.vector.ImageVector
import com.pulse.app.ui.theme.PulseColors
import com.pulse.movement.Exercise

/** Presentational metadata per exercise. No stats are invented here. */
data class ExerciseVisual(
    val exercise: Exercise,
    val title: String,
    val category: String,
    val target: String,
    val accent: androidx.compose.ui.graphics.Color,
    val icon: ImageVector
)

object ExerciseVisuals {

    val squat = ExerciseVisual(
        exercise = Exercise.SQUAT,
        title = "Squat",
        category = "Lower body · Beginner",
        target = "Legs + Glutes",
        accent = PulseColors.Green,
        icon = Icons.Filled.SportsGymnastics
    )

    val pushUp = ExerciseVisual(
        exercise = Exercise.PUSH_UP,
        title = "Push-up",
        category = "Upper body · Beginner",
        target = "Chest + Arms",
        accent = PulseColors.Blue,
        icon = Icons.Filled.FitnessCenter
    )

    val hinge = ExerciseVisual(
        exercise = Exercise.HINGE,
        title = "Hinge",
        category = "Posterior chain · Beginner",
        target = "Hamstrings + Back",
        accent = PulseColors.Orange,
        icon = Icons.Filled.SelfImprovement
    )

    val all = listOf(squat, pushUp, hinge)

    fun forExercise(exercise: Exercise): ExerciseVisual =
        all.first { it.exercise == exercise }

    fun categoryFilter(label: String): List<ExerciseVisual> = when (label) {
        "Lower Body" -> all.filter { it.exercise == Exercise.SQUAT }
        "Upper Body" -> all.filter { it.exercise == Exercise.PUSH_UP }
        "Core" -> emptyList()
        else -> all
    }
}
