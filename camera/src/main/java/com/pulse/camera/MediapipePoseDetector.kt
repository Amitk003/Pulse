package com.pulse.camera

import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import com.pulse.movement.Landmark
import com.pulse.movement.Point2D
import com.pulse.movement.PoseFrame

/**
 * MediaPipe Pose Landmarker wrapper for live camera frames.
 *
 * Uses the tasks-vision library with RunningMode LIVE_STREAM and the
 * pose_landmarker_lite.task bundle. The lite bundle is picked first
 * because it is fast enough for 15 to 20 analyzed frames per second
 * on a mid range phone. Full and heavy bundles stay as later options.
 *
 * This class owns only pose mapping. Rep counting and scoring live
 * in the movement module. All processing stays on the device and no
 * frame or landmark is saved or uploaded.
 *
 * Model file: place pose_landmarker_lite.task in app/src/main/assets.
 * Dependency: com.google.mediapipe:tasks-vision:latest.release.
 */
class MediapipePoseDetector(
    private val minPoseDetectionConfidence: Float = 0.5f,
    private val minPosePresenceConfidence: Float = 0.5f,
    private val minTrackingConfidence: Float = 0.5f
) : AutoCloseable {

    companion object {
        const val MODEL_ASSET_PATH = "pose_landmarker_lite.task"
        const val NUM_POSES = 1
        const val LANDMARK_COUNT = 33
    }

    private var landmarker: PoseLandmarker? = null
    private var lastResult: PoseLandmarkerResult? = null
    private var lastError: String? = null

    /**
     * Create the landmarker. Call once before the first frame.
     * context is an Android Context kept only during setup.
     */
    fun setup(context: android.content.Context) {
        val base = BaseOptions.builder()
            .setModelAssetPath(MODEL_ASSET_PATH)
            .setDelegate(Delegate.CPU)
            .build()
        val options = PoseLandmarker.PoseLandmarkerOptions.builder()
            .setBaseOptions(base)
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setNumPoses(NUM_POSES)
            .setMinPoseDetectionConfidence(minPoseDetectionConfidence)
            .setMinPosePresenceConfidence(minPosePresenceConfidence)
            .setMinTrackingConfidence(minTrackingConfidence)
            .setResultListener { result, _ -> lastResult = result }
            .setErrorListener { error -> lastError = error.message }
            .build()
        landmarker?.close()
        landmarker = PoseLandmarker.createFromOptions(context, options)
    }

    /**
     * Send one camera frame for async detection. Returns at once.
     * The newest [PoseLandmarkerResult] arrives through the listener.
     * Rotation must come from CameraX ImageInfo so landmarks match
     * the preview. Callers must drop a frame while one is in flight.
     */
    fun detectAsync(image: MPImage, frameTimeMs: Long) {
        landmarker?.detectAsync(image, frameTimeMs)
    }

    /**
     * Convert the latest MediaPipe result into a [PoseFrame].
     * Returns null when no person is found or data is incomplete.
     * Only the first pose is used. When two people are in view the
     * screen must ask the user to keep only one person in frame.
     */
    fun latestPoseFrame(nowMs: Long): PoseFrame? {
        val result = lastResult ?: return null
        if (result.landmarks().isEmpty()) {
            return null
        }
        val pose = result.landmarks()[0]
        if (pose.size < LANDMARK_COUNT) {
            return null
        }
        val landmarks = pose.map { mp ->
            val visibility = if (mp.hasVisibility()) mp.visibility().orElse(0f) else 0f
            Landmark(visibility = visibility)
        }
        return PoseFrame(
            timeMs = nowMs,
            landmarks = landmarks,
            joints = extractJoints(pose)
        )
    }

    fun lastErrorMessage(): String? = lastError

    override fun close() {
        landmarker?.close()
        landmarker = null
        lastResult = null
    }

    /**
     * Map MediaPipe indices to the joint names used by PoseFrame.
     * Indices follow the 33 point BlazePose layout: 11 left shoulder,
     * 13 left elbow, 15 left wrist, 23 left hip, 25 left knee, 27 left ankle.
     */
    private fun extractJoints(
        pose: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>
    ): Map<String, Point2D> {
        fun point(index: Int): Point2D {
            val item = pose[index]
            return Point2D(x = item.x().toDouble(), y = item.y().toDouble())
        }
        return mapOf(
            "left_shoulder" to point(11),
            "right_shoulder" to point(12),
            "left_elbow" to point(13),
            "right_elbow" to point(14),
            "left_wrist" to point(15),
            "right_wrist" to point(16),
            "left_hip" to point(23),
            "right_hip" to point(24),
            "left_knee" to point(25),
            "right_knee" to point(26),
            "left_ankle" to point(27),
            "right_ankle" to point(28)
        )
    }
}
