package com.pulse.camera

import android.graphics.Bitmap
import java.io.ByteArrayOutputStream
import java.util.ArrayDeque
import java.util.Base64

/**
 * Thread-safe rolling window buffer for camera frames.
 * Captures downsampled JPEG frames spanning ~1 to 2 seconds to supply temporal
 * context for Gemini Vision AI sequence analysis.
 */
class RepClipBuffer(
    private val maxFrames: Int = 4,
    private val frameQuality: Int = 60,
    private val targetWidth: Int = 320
) {
    private val buffer = ArrayDeque<String>()

    @Synchronized
    fun addFrame(bitmap: Bitmap) {
        try {
            val scale = targetWidth.toFloat() / bitmap.width.coerceAtLeast(1)
            val height = (bitmap.height * scale).toInt().coerceAtLeast(1)
            val scaled = Bitmap.createScaledBitmap(bitmap, targetWidth, height, true)

            val outputStream = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, frameQuality, outputStream)
            val bytes = outputStream.toByteArray()
            val base64 = Base64.getEncoder().encodeToString(bytes)

            if (buffer.size >= maxFrames) {
                buffer.removeFirst()
            }
            buffer.addLast(base64)
            if (scaled != bitmap) {
                scaled.recycle()
            }
        } catch (e: Exception) {
            // Ignore compression errors for frame buffer
        }
    }

    @Synchronized
    fun getClip(): List<String> {
        return buffer.toList()
    }

    @Synchronized
    fun clear() {
        buffer.clear()
    }
}
