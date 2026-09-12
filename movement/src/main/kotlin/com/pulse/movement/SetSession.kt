package com.pulse.movement

/**
 * One exercise set from first frame to finished result.
 *
 * Wiring: PoseFrame to visibility check to rep counter.
 * Form observations are collected per counted rep from the
 * lowest angle seen in that rep cycle. Geometry faults from
 * FormSignals latch while a rep is active, so one bad frame
 * inside a rep marks that rep. At the end, [finish]
 * scores form and returns one valid [SetResult].
 *
 * Camera and MediaPipe types never enter this class.
 */
class SetSession(
    val exercise: Exercise,
    private val config: MovementConfig = MovementConfig()
) {
    private val counter = RepCounter(exercise, config)
    private val observations = mutableListOf<FormScorer.RepObservation>()
    private var lastDepth: Double? = null
    private var latchedValgus: Boolean = false
    private var latchedBack: Boolean = false
    private var latchedSagging: Boolean = false
    private var startMs: Long? = null
    private var endMs: Long? = null
    var blockedFrames: Int = 0
        private set

    val reps: Int get() = counter.reps
    val phase: RepPhase get() = counter.phase

    /** Last setup hint for the screen, empty when the view is fine. */
    var hint: String = ""
        private set

    fun onFrame(frame: PoseFrame): RepPhase {
        if (startMs == null) {
            startMs = frame.timeMs
        }
        endMs = frame.timeMs

        val visibility = VisibilityGate.check(frame.landmarks, config)
        if (!visibility.ok) {
            blockedFrames += 1
            hint = visibility.message
            counter.onFrame(null, visibility)
            return counter.phase
        }
        hint = ""

        val repsBefore = counter.reps
        val angle = frame.angleFor(exercise)
        counter.onFrame(angle, visibility)

        if (counter.phase != RepPhase.READY) {
            val faults = FormSignals.faultsFor(exercise, frame.joints, angle, config)
            latchedValgus = latchedValgus || faults.kneeValgus
            latchedBack = latchedBack || faults.backRound
            latchedSagging = latchedSagging || faults.sagging
        }

        val depth = counter.currentDepth()
        if (depth != null) {
            lastDepth = depth
        }
        if (counter.reps > repsBefore) {
            observations.add(
                FormScorer.RepObservation(
                    minAngleDeg = lastDepth ?: angle ?: 180.0,
                    kneeValgus = latchedValgus,
                    backRound = latchedBack,
                    sagging = latchedSagging
                )
            )
            lastDepth = null
            clearLatchedFaults()
        } else if (counter.phase == RepPhase.READY) {
            clearLatchedFaults()
        }
        return counter.phase
    }

    private fun clearLatchedFaults() {
        latchedValgus = false
        latchedBack = false
        latchedSagging = false
    }

    /** Build the final result for storage and progress. */
    fun finish(): SetResult {
        val form = FormScorer.score(exercise, observations.toList())
        val start = startMs ?: 0L
        val end = endMs ?: start
        val durationSec = ((end - start) / 1000L).toInt().coerceAtLeast(0)
        val result = SetResult(
            exercise = exercise.id,
            reps = counter.reps,
            formScore = form.score,
            mistakes = form.mistakes,
            durationSec = durationSec
        )
        check(result.isValid()) { "Built an invalid SetResult" }
        return result
    }
}
