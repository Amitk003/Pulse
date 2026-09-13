package com.pulse.movement

/**
 * Simple explainable form checks for one finished set.
 *
 * Mistake identifiers are fixed lower-case strings so stored data stays stable:
 * squat uses `shallow`, `knee-valgus`, `back-round`;
 * push-up uses `half-rep`, `sagging`, `incomplete`;
 * hinge uses `half-rep`, `rounded-back`.
 *
 * Thresholds were adjusted after switching to pose_landmarker_full.task.
 * They are still starting values, not final device-validated numbers.
 */
object FormScorer {

    /** One per-rep observation collected during a set. */
    data class RepObservation(
        val minAngleDeg: Double,
        val kneeValgus: Boolean = false,
        val backRound: Boolean = false,
        val sagging: Boolean = false
    )

    /** Score plus mistake list for a set. */
    data class FormResult(
        val score: Int,
        val mistakes: List<String>
    )

    fun score(exercise: Exercise, observations: List<RepObservation>): FormResult {
        if (observations.isEmpty()) {
            return FormResult(score = 0, mistakes = listOf("no-reps"))
        }
        val mistakes = mutableSetOf<String>()
        var penalty = 0

        for (rep in observations) {
            when (exercise) {
                Exercise.SQUAT -> {
                    // Tighter threshold: only flag very shallow squats
                    if (rep.minAngleDeg > 105.0) {
                        mistakes.add("shallow")
                        penalty += 15
                    }
                    if (rep.kneeValgus) {
                        mistakes.add("knee-valgus")
                        penalty += 12
                    }
                    if (rep.backRound) {
                        mistakes.add("back-round")
                        penalty += 12
                    }
                }
                Exercise.PUSH_UP -> {
                    if (rep.minAngleDeg > 95.0) {
                        mistakes.add("half-rep")
                        penalty += 15
                    }
                    if (rep.sagging) {
                        mistakes.add("sagging")
                        penalty += 12
                    }
                    // Incomplete: between half-rep and full depth
                    if (rep.minAngleDeg in 85.0..95.0) {
                        mistakes.add("incomplete")
                        penalty += 8
                    }
                }
                Exercise.HINGE -> {
                    if (rep.minAngleDeg > 120.0) {
                        mistakes.add("half-rep")
                        penalty += 15
                    }
                    if (rep.backRound) {
                        mistakes.add("rounded-back")
                        penalty += 12
                    }
                }
            }
        }

        val averagePenalty = penalty / observations.size
        val score = (100 - averagePenalty).coerceIn(0, 100)
        return FormResult(score = score, mistakes = mistakes.sorted())
    }
}
