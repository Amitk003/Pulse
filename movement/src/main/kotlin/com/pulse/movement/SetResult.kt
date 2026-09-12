package com.pulse.movement

/**
 * Small shared result for one finished exercise set.
 *
 * This model moves between the camera, domain, and data layers.
 * It uses fixed lower-case strings so stored data stays stable.
 * Display text belongs in the UI layer, not here.
 */
data class SetResult(
    val exercise: String,
    val reps: Int,
    val formScore: Int,
    val mistakes: List<String>,
    val durationSec: Int
) {
    /**
     * Check that all fields hold safe values.
     * Returns a list of problems. An empty list means the result is valid.
     */
    fun validate(): List<String> {
        val problems = mutableListOf<String>()
        if (exercise != "squat" && exercise != "push-up" && exercise != "hinge") {
            problems.add("bad-exercise")
        }
        if (reps < 0) {
            problems.add("bad-reps")
        }
        if (formScore < 0 || formScore > 100) {
            problems.add("bad-score")
        }
        if (durationSec < 0) {
            problems.add("bad-duration")
        }
        for (mistake in mistakes) {
            if (mistake.isBlank() || mistake != mistake.trim().lowercase()) {
                problems.add("bad-mistake")
                break
            }
        }
        return problems
    }

    /** True when [validate] finds no problems. */
    fun isValid(): Boolean = validate().isEmpty()
}
