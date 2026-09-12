package com.pulse.movement

/** Supported exercises. Identifiers match [SetResult.exercise]. */
enum class Exercise(val id: String) {
    SQUAT("squat"),
    PUSH_UP("push-up"),
    HINGE("hinge");

    companion object {
        fun fromId(id: String): Exercise? = entries.firstOrNull { it.id == id }
    }
}

/** Small rep state machine. A rep counts only after a full down and up cycle. */
enum class RepPhase {
    READY,
    DOWN,
    UP
}

/**
 * Pure Kotlin rep counter for one exercise set.
 *
 * How to use:
 * 1. Create one counter per set with the selected exercise.
 * 2. For each analyzed frame, call [onFrame] with the joint angle and visibility result.
 * 3. When visibility fails, the state does not move and no rep is added.
 * 4. Read [reps] for the live count.
 *
 * The class keeps no Android types so it can run in plain unit tests.
 */
class RepCounter(
    val exercise: Exercise,
    private val config: MovementConfig = MovementConfig()
) {
    var phase: RepPhase = RepPhase.READY
        private set

    var reps: Int = 0
        private set

    var minAngleSeen: Double = Double.MAX_VALUE
        private set

    private val recentAngles = mutableListOf<Double?>()
    private var framesInPhase = 0

    fun startAngle(): Double = when (exercise) {
        Exercise.SQUAT -> config.squatStartDeg
        Exercise.PUSH_UP -> config.pushUpStartDeg
        Exercise.HINGE -> config.hingeStartDeg
    }

    fun startTolerance(): Double = when (exercise) {
        Exercise.SQUAT -> config.squatStartToleranceDeg
        Exercise.PUSH_UP -> config.pushUpStartToleranceDeg
        Exercise.HINGE -> config.hingeStartToleranceDeg
    }

    fun downAngle(): Double = when (exercise) {
        Exercise.SQUAT -> config.squatDownDeg
        Exercise.PUSH_UP -> config.pushUpDownDeg
        Exercise.HINGE -> config.hingeDownDeg
    }

    /**
     * Feed one analyzed frame. Returns the current phase after this frame.
     * A null angle means the pose was unclear, so the state is held.
     */
    fun onFrame(angleDeg: Double?, visibility: VisibilityResult): RepPhase {
        if (!visibility.ok) {
            return phase
        }
        if (angleDeg == null || angleDeg.isNaN()) {
            return phase
        }

        recentAngles.add(angleDeg)
        if (recentAngles.size > config.smoothingWindow) {
            recentAngles.removeAt(0)
        }
        val smooth = Angles.smoothed(recentAngles, config.smoothingWindow) ?: return phase
        if (smooth < minAngleSeen) {
            minAngleSeen = smooth
        }

        framesInPhase += 1
        val stable = framesInPhase >= config.minFramesInState
        val nearStart = smooth >= (startAngle() - startTolerance())
        val isDown = smooth <= downAngle()

        when (phase) {
            RepPhase.READY -> {
                if (isDown) {
                    if (stable) {
                        phase = RepPhase.DOWN
                        framesInPhase = 0
                    }
                } else if (!nearStart) {
                    framesInPhase = 0
                }
            }
            RepPhase.DOWN -> {
                if (nearStart && stable) {
                    phase = RepPhase.UP
                    framesInPhase = 0
                }
            }
            RepPhase.UP -> {
                if (nearStart && stable) {
                    reps += 1
                    phase = RepPhase.READY
                    framesInPhase = 0
                    minAngleSeen = Double.MAX_VALUE
                } else if (isDown && stable) {
                    phase = RepPhase.DOWN
                    framesInPhase = 0
                }
            }
        }
        return phase
    }

    /** Lowest smoothed angle seen during the current rep cycle, or null when no rep is active. */
    fun currentDepth(): Double? {
        if (minAngleSeen == Double.MAX_VALUE) {
            return null
        }
        return minAngleSeen
    }

    fun reset() {
        phase = RepPhase.READY
        reps = 0
        minAngleSeen = Double.MAX_VALUE
        recentAngles.clear()
        framesInPhase = 0
    }
}
