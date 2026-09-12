package com.pulse.movement

enum class BodyVisibility {
    FULL_BODY_VISIBLE,
    PARTIALLY_VISIBLE,
    INSUFFICIENT,
    UNKNOWN;

    companion object {
        fun fromString(value: String?): BodyVisibility {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: UNKNOWN
        }
    }
}

enum class CurrentForm {
    GOOD,
    WARNING,
    BAD,
    UNKNOWN;

    companion object {
        fun fromString(value: String?): CurrentForm {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: UNKNOWN
        }
    }
}

enum class AiRepState {
    CANDIDATE,
    COMPLETED,
    INVALID;

    companion object {
        fun fromString(value: String?): AiRepState {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: INVALID
        }
    }
}

/**
 * Authoritative analysis result received from the Gemini AI backend.
 */
data class AiAnalysisResult(
    val exercise: String = "",
    val exerciseConfidence: Double = 0.0,
    val personVisible: Boolean = false,
    val bodyVisibility: BodyVisibility = BodyVisibility.UNKNOWN,
    val cameraUsable: Boolean = true,
    val cameraWarning: String? = null,
    val movementDetected: Boolean = false,
    val movementPhase: String = "UNKNOWN",
    val exerciseMatch: Boolean = false,
    val exerciseMatchConfidence: Double = 0.0,
    val currentForm: CurrentForm = CurrentForm.UNKNOWN,
    val formScore: Int = 0,
    val formConfidence: Double = 0.0,
    val repState: AiRepState = AiRepState.INVALID,
    val repLikelihood: Double = 0.0,
    val repComplete: Boolean = false,
    val anomalyDetected: Boolean = false,
    val anomalyType: String? = null,
    val errors: List<String> = emptyList(),
    val feedback: String = "",
    val timestampMs: Long = 0L
) {
    /**
     * True if the AI result is within the acceptable age threshold.
     */
    fun isFresh(nowMs: Long, maxAgeMs: Long = 1500L): Boolean {
        if (timestampMs <= 0L) return false
        return (nowMs - timestampMs) in 0..maxAgeMs
    }

    /**
     * Evaluates if the AI result supports validating a repetition.
     * Core Rule: Person visible, body sufficiently visible, matching exercise,
     * no anomaly (e.g., couch sitting), and valid rep state / likelihood.
     */
    fun isValidForRep(minLikelihood: Double = 0.70): Boolean {
        if (!personVisible || !cameraUsable) return false
        if (bodyVisibility == BodyVisibility.INSUFFICIENT) return false
        if (!exerciseMatch) return false
        if (anomalyDetected) return false
        if (repState == AiRepState.INVALID) return false
        
        return (repComplete || repState == AiRepState.COMPLETED || repLikelihood >= minLikelihood)
    }
}
