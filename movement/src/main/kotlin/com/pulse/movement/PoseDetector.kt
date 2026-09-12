package com.pulse.movement

/**
 * Contract for any pose source.
 *
 * The movement core never talks to MediaPipe directly. The Android
 * wrapper implements this interface and returns small [PoseFrame]
 * objects. Tests use [FakePoseDetector] with fixed frames.
 */
interface PoseDetector : AutoCloseable {

    /** Human readable source name for logs, for example "mediapipe" or "fake". */
    fun sourceName(): String

    /**
     * Next analyzed frame, or null when no new frame is ready.
     * Returning null must not change rep state. The caller keeps
     * polling at the analyzer rate and drops stale data.
     */
    fun nextFrame(): PoseFrame?
}

/** Test helper that replays a fixed list of frames. */
class FakePoseDetector(private val frames: List<PoseFrame?>) : PoseDetector {

    private var index = 0

    override fun sourceName(): String = "fake"

    override fun nextFrame(): PoseFrame? {
        if (index >= frames.size) {
            return null
        }
        val frame = frames[index]
        index += 1
        return frame
    }

    override fun close() {
        // Nothing to release for the fake.
    }
}
