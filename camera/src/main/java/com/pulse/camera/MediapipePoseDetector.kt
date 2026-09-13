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

data class PoseDetection(
    val frame: PoseFrame?,
    val poseCount: Int,
    val errorMessage: String? = null
)

/**
 * MediaPipe Pose Landmarker wrapper for live camera frames.
 *
 * Uses the tasks-vision library with RunningMode LIVE_STREAM and the
 * pose_landmarker_full.task bundle.
 *
 * This class owns only pose mapping. Rep counting and scoring live
 * in the movement module. All processing stays on the device and no
 * frame or landmark is saved or uploaded.
 *
 * Model file: place pose_landmarker_full.task in app/src/main/assets.
 * Dependency: com.google.mediapipe:tasks-vision:0.10.26.1.
 *
 * Note: the full model is larger and more expensive than lite. It is
 * currently the selected model for this app. If device performance or
 * app size becomes a problem, revisit the model choice before changing
 * thresholds.
 */
class MediapipePoseDetector(
    private val minPoseDetectionConfidence: Float = 0.6f,
    private val minPosePresenceConfidence: Float = 0.6f,
    private val minTrackingConfidence: Float = 0.6f
) : AutoCloseable {

    companion object {
    const val MODEL_ASSET_PATH = "pose_landmarker_full.task"
        const val NUM_POSES = 1
        const val LANDMARK_COUNT = 33
    }

    private var landmarker: PoseLandmarker? = null
    private var detectionListener: ((PoseDetection) -> Unit)? = null
    @Volatile
    private var pendingFrameTimeMs: Long = 0L

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
            .setResultListener { result, _ ->
                val poses = result.landmarks()
                detectionListener?.invoke(
                    PoseDetection(
                        frame = toPoseFrame(poses.firstOrNull(), pendingFrameTimeMs),
                        poseCount = poses.size
                    )
                )
            }
            .setErrorListener { error ->
                detectionListener?.invoke(
                    PoseDetection(frame = null, poseCount = 0, errorMessage = error.message)
                )
            }
            .build()
        landmarker?.close()
        landmarker = PoseLandmarker.createFromOptions(context, options)
    }

    fun setDetectionListener(listener: ((PoseDetection) -> Unit)?) {
        detectionListener = listener
    }

    /**
     * Send one camera frame for async detection. Returns at once.
     * The newest [PoseLandmarkerResult] arrives through the listener.
     * Rotation must come from CameraX ImageInfo so landmarks match
     * the preview. Callers must drop a frame while one is in flight.
     */
    fun detectAsync(image: MPImage, frameTimeMs: Long) {
        pendingFrameTimeMs = frameTimeMs
        val current = landmarker
        if (current == null) {
            detectionListener?.invoke(
                PoseDetection(frame = null, poseCount = 0, errorMessage = "Pose detector is not ready.")
            )
            return
        }
        current.detectAsync(image, frameTimeMs)
    }

    /**
     * Convert one MediaPipe result into a [PoseFrame].
     * Returns null when no person is found or data is incomplete.
     * Only the first pose is converted. The caller must check the pose count
     * first and ask the user to keep only one person in frame when needed.
     */
    private fun toPoseFrame(
        pose: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>?,
        frameTimeMs: Long
    ): PoseFrame? {
        if (pose == null) {
            return null
        }
        if (pose.size < LANDMARK_COUNT) {
            return null
        }
        val landmarks = pose.map { mp ->
            // visibility() returns an Optional in every tasks-vision
            // release, so orElse covers landmarks without a value.
            // runCatching keeps this compiling across library versions.
            val visibility = runCatching { mp.visibility().orElse(0f) }.getOrDefault(0f)
            Landmark(visibility = visibility)
        }
        return PoseFrame(
            timeMs = frameTimeMs,
            landmarks = landmarks,
            joints = extractJoints(pose)
        )
    }

    override fun close() {
        landmarker?.close()
        landmarker = null
        pendingFrameTimeMs = 0L
        detectionListener = null
    }

    /**
     * Map MediaPipe indices to the joint names used by PoseFrame.
     * Indices follow the 33 point BlazePose layout. Left side: 11 left
     * shoulder, 13 left elbow, 15 left wrist, 23 left hip, 25 left knee,
     * 27 left ankle. Right side mirrors it: 12, 14, 16, 24, 26, 28.
     * Right-side points feed front-view checks such as knee valgus.
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
            "left_elbow" to point(13),
            "left_wrist" to point(15),
            "left_hip" to point(23),
            "left_knee" to point(25),
            "left_ankle" to point(27),
            "right_shoulder" to point(12),
            "right_elbow" to point(14),
            "right_wrist" to point(16),
            "right_hip" to point(24),
            "right_knee" to point(26),
            "right_ankle" to point(28)
        )
    }
}
