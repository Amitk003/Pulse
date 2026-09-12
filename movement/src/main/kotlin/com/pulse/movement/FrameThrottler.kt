package com.pulse.movement

/**
 * Analyzer pacing helper.
 *
 * Target is 15 to 20 analyzed frames per second. The camera may
 * deliver more frames, so this helper decides which frames to keep.
 * It also drops a new frame while the previous one is still running,
 * so no queue can grow. All state stays in plain fields for tests.
 *
 * @param minIntervalMs smallest gap between two analyzed frames.
 * 50ms means at most 20 frames per second. 66ms means at most 15.
 */
class FrameThrottler(private val minIntervalMs: Long = 50L) {

    private var lastAcceptedMs: Long = Long.MIN_VALUE
    private var busy = false
    var accepted: Int = 0
        private set
    var dropped: Int = 0
        private set

    /** Mark the analyzer as busy or idle. Call from the worker thread. */
    fun setBusy(value: Boolean) {
        busy = value
    }

    /**
     * True when this frame should be analyzed. False means drop it
     * and keep the preview smooth. Dropped frames update [dropped].
     */
    fun shouldAccept(nowMs: Long): Boolean {
        if (busy) {
            dropped += 1
            return false
        }
        if (lastAcceptedMs != Long.MIN_VALUE && nowMs - lastAcceptedMs < minIntervalMs) {
            dropped += 1
            return false
        }
        lastAcceptedMs = nowMs
        accepted += 1
        return true
    }

    fun reset() {
        lastAcceptedMs = Long.MIN_VALUE
        busy = false
        accepted = 0
        dropped = 0
    }
}
