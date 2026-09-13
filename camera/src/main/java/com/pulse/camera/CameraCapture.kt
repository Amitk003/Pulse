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
import com.pulse.movement.SetResult
import com.pulse.movement.SetSession
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeUnit
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
    private val throttler = FrameThrottler(minIntervalMs = 40L)
    private val acceptingResults = AtomicBoolean(false)
    private val idleLock = Object()
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
        acceptingResults.set(true)
        detector.setDetectionListener { detection ->
            if (!acceptingResults.get()) {
                inFlight.set(false)
                return@setDetectionListener
            }
            try {
                worker.execute {
                    try {
                        if (!acceptingResults.get()) {
                            return@execute
                        }
                        when {
                            detection.poseCount > 1 -> {
                                onUpdate(session?.reps ?: 0, "Keep only one person in frame.")
                            }
                            detection.frame != null -> {
                                session?.onFrame(detection.frame)
                                onUpdate(
                                    session?.reps ?: 0,
                                    session?.hint ?: ""
                                )
                            }
                            !detection.errorMessage.isNullOrBlank() -> {
                                onUpdate(
                                    session?.reps ?: 0,
                                    "Camera analysis failed. Try again."
                                )
                            }
                        }
                    } finally {
                        inFlight.set(false)
                        synchronized(idleLock) { idleLock.notifyAll() }
                    }
                }
            } catch (_: RejectedExecutionException) {
                inFlight.set(false)
            }
        }
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
                } finally {
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
            awaitIdle()
        }
    }

    fun stats(): AnalyzerStats {
        return AnalyzerStats(accepted = throttler.accepted, dropped = throttler.dropped)
    }

    /**
     * Finish the current set and build its result.
     * Returns null when no set ran or the result is invalid.
     * Safe to call on the main thread. Call after [stop].
     */
    fun finish(): SetResult? {
        awaitIdle()
        acceptingResults.set(false)
        return try {
            session?.finish()
        } catch (e: IllegalStateException) {
            null
        }
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
        acceptingResults.set(false)
        detector.close()
        worker.shutdown()
    }

    private fun awaitIdle() {
        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(500)
        synchronized(idleLock) {
            while (inFlight.get()) {
                val remaining = deadline - System.nanoTime()
                if (remaining <= 0L) break
                val millis = TimeUnit.NANOSECONDS.toMillis(remaining).coerceAtLeast(1L)
                try {
                    idleLock.wait(millis)
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                    break
                }
            }
        }
    }
}
