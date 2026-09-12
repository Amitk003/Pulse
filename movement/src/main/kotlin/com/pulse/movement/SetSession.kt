package com.pulse.movement

/**
 * One exercise set from first frame to finished result.
 *
 * Wiring: PoseFrame to visibility check to rep counter.
 * Form observations are collected per counted rep from the
 * lowest angle seen in that rep cycle. At the end, [finish]
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
    private var startMs: Long? = null
    private var endMs: Long? = null
    var blockedFrames: Int = 0
        private set

    var latestAiResult: AiAnalysisResult? = null
        private set
    var latestAiResultTimeMs: Long = 0L
        private set

    val reps: Int get() = counter.reps
    val candidateReps: Int get() = counter.candidateReps
    val phase: RepPhase get() = counter.phase

    /** Last setup hint for the screen, empty when the view is fine. */
    var hint: String = ""
        private set

    /**
     * Called when a new AI analysis result is received from Gemini proxy.
     * Keeps track of the result and its timestamp.
     */
    fun updateAiResult(result: AiAnalysisResult, timestampMs: Long = System.currentTimeMillis()) {
        latestAiResult = result.copy(timestampMs = timestampMs)
        latestAiResultTimeMs = timestampMs
        if (result.feedback.isNotBlank()) {
            hint = result.feedback
        }
    }

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

        val angle = frame.angleFor(exercise)
        counter.onFrame(angle, visibility)

        val depth = counter.currentDepth()
        if (depth != null) {
            lastDepth = depth
        }

        // Authoritative Fusion Layer: Check if local tracker produced a candidate rep
        if (counter.hasPendingCandidateRep()) {
            val ai = latestAiResult
            val isFresh = ai?.isFresh(frame.timeMs, config.aiResultMaxAgeMs) == true
            val isValidAi = ai?.isValidForRep(config.minAiRepLikelihood) == true

            if (ai != null && isFresh && isValidAi) {
                // APPROVED: Rep validated by Gemini AI!
                counter.consumePendingCandidateRep()
                counter.incrementAuthoritativeRep()
                observations.add(
                    FormScorer.RepObservation(minAngleDeg = lastDepth ?: angle ?: 180.0)
                )
                lastDepth = null
                hint = if (ai.feedback.isNotBlank()) ai.feedback else ""
            } else {
                // REJECTED: Reject candidate rep due to missing/stale AI data or anomaly
                counter.consumePendingCandidateRep()
                hint = when {
                    ai == null || !isFresh -> "Waiting for AI movement verification..."
                    ai.anomalyDetected -> "Rep rejected: Unrelated movement detected (${ai.anomalyType ?: "anomaly"})."
                    ai.bodyVisibility == BodyVisibility.INSUFFICIENT -> "Rep rejected: Full body not visible."
                    !ai.exerciseMatch -> "Rep rejected: Movement does not match ${exercise.id}."
                    else -> if (ai.feedback.isNotBlank()) ai.feedback else "Rep rejected: Form insufficient."
                }
            }
        } else if (latestAiResult?.feedback?.isNotBlank() == true) {
            hint = latestAiResult!!.feedback
        } else {
            hint = ""
        }

        return counter.phase
    }

    /** Build the final result for storage and progress. */
    fun finish(): SetResult {
        val localForm = FormScorer.score(exercise, observations.toList())
        val aiScore = latestAiResult?.formScore
        val finalFormScore = if (aiScore != null && aiScore > 0) {
            ((localForm.score + aiScore) / 2).coerceIn(0, 100)
        } else {
            localForm.score
        }

        val start = startMs ?: 0L
        val end = endMs ?: start
        val durationSec = ((end - start) / 1000L).toInt().coerceAtLeast(0)
        val result = SetResult(
            exercise = exercise.id,
            reps = counter.reps,
            formScore = finalFormScore,
            mistakes = localForm.mistakes,
            durationSec = durationSec
        )
        check(result.isValid()) { "Built an invalid SetResult" }
        return result
    }
}
