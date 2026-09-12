package com.pulse.camera

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.pulse.movement.Exercise
import com.pulse.movement.FrameThrottler
import com.pulse.movement.MovementConfig
import com.pulse.movement.SetSession
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/** Simple analyzer stats for the device test log. */
data class AnalyzerStats(
    val accepted: Int,
    val dropped: Int
)

/**
 * CameraX capture for one exercise set.
 *
 * Pipeline: Preview plus ImageAnalysis to MediaPipe LIVE_STREAM to
 * PoseFrame to SetSession. Only small PoseFrame results leave the
 * analyzer. Compose state is updated with reps, hint, and stats,
 * never with full landmarks.
 *
 * Rules kept here:
 * - KEEP_ONLY_LATEST backpressure so no frame queue can grow.
 * - Drop a frame while the previous one is still running.
 * - Target 15 to 20 analyzed frames per second through FrameThrottler.
 * - Rotation comes from ImageInfo so landmarks match the preview.
 * - No frame bitmap and no raw landmark is saved to disk.
 */
class CameraCapture(
    private val detector: MediapipePoseDetector,
    private val config: MovementConfig = MovementConfig()
) {
    private val worker = Executors.newSingleThreadExecutor()
    private val inFlight = AtomicBoolean(false)
    private val throttler = FrameThrottler(minIntervalMs = 50L)
    private var session: SetSession? = null
    private var cameraProvider: ProcessCameraProvider? = null

    /** Start preview plus analysis for the given exercise. */
    fun start(
        context: Context,
        owner: LifecycleOwner,
        previewView: androidx.camera.view.PreviewView,
        exercise: Exercise,
        onUpdate: (reps: Int, hint: String) -> Unit
    ) {
        detector.setup(context)
        session = SetSession(exercise, config)
        throttler.reset()
        inFlight.set(false)

        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            val provider = future.get()
            cameraProvider = provider
            provider.unbindAll()

            val preview = Preview.Builder().build()
            preview.setSurfaceProvider(previewView.surfaceProvider)

            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()

            analysis.setAnalyzer(worker) { imageProxy ->
                try {
                    val nowMs = System.currentTimeMillis()
                    if (!throttler.shouldAccept(nowMs)) {
                        return@setAnalyzer
                    }
                    if (!inFlight.compareAndSet(false, true)) {
                        return@setAnalyzer
                    }
                    val rotation = imageProxy.imageInfo.rotationDegrees
                    val bitmap = imageProxy.toBitmap()
                    val rotated = rotateBitmap(bitmap, rotation)
                    val mpImage = BitmapImageBuilder(rotated).build()
                    detector.detectAsync(mpImage, nowMs)
                    val frame = detector.latestPoseFrame(nowMs)
                    if (frame != null) {
                        session?.onFrame(frame)
                        val reps = session?.reps ?: 0
                        val hint = session?.hint ?: ""
                        onUpdate(reps, hint)
                    }
                } finally {
                    inFlight.set(false)
                    imageProxy.close()
                }
            }

            provider.bindToLifecycle(
                owner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                analysis
            )
        }, ContextCompat.getMainExecutor(context))
    }

    /** Stop analysis and release the camera. Call from onPause or onStop. */
    fun stop() {
        try {
            cameraProvider?.unbindAll()
        } finally {
            cameraProvider = null
            worker.execute {
                inFlight.set(false)
            }
        }
    }

    fun stats(): AnalyzerStats {
        return AnalyzerStats(accepted = throttler.accepted, dropped = throttler.dropped)
    }

    /**
     * Rotate the frame so landmarks match the preview.
     * Rotation comes from CameraX ImageInfo and is 0, 90, 180, or 270.
     */
    private fun rotateBitmap(source: android.graphics.Bitmap, degrees: Int): android.graphics.Bitmap {
        if (degrees == 0) {
            return source
        }
        val matrix = android.graphics.Matrix()
        matrix.postRotate(degrees.toFloat())
        return android.graphics.Bitmap.createBitmap(
            source, 0, 0, source.width, source.height, matrix, true
        )
    }

    fun close() {
        stop()
        detector.close()
        worker.shutdown()
    }
}
