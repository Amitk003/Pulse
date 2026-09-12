package com.pulse.camera

import com.pulse.movement.AiAnalysisResult
import com.pulse.movement.AiRepState
import com.pulse.movement.BodyVisibility
import com.pulse.movement.CurrentForm
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Asynchronous network client communicating with the Pulse FastAPI proxy backend.
 * Sends frame clips at ~1 to 3 FPS for Gemini Vision AI analysis.
 */
class GeminiFormAnalyzer(
    private val backendUrl: String = "http://10.0.2.2:8000/analyze-form",
    private val connectTimeoutMs: Int = 2000,
    private val readTimeoutMs: Int = 3000
) : AutoCloseable {

    private val executor = Executors.newSingleThreadExecutor()
    private val isBusy = AtomicBoolean(false)

    fun analyzeAsync(
        exercise: String,
        framesBase64: List<String>,
        onResult: (AiAnalysisResult) -> Unit
    ) {
        if (framesBase64.isEmpty()) return
        if (!isBusy.compareAndSet(false, true)) {
            // Latest-window-wins: Skip request if previous network request is still in-flight
            return
        }

        executor.execute {
            val nowMs = System.currentTimeMillis()
            try {
                val jsonPayload = JSONObject().apply {
                    put("exercise", exercise)
                    put("timestamp_ms", nowMs)
                    put("frames_base64", JSONArray(framesBase64))
                }

                val url = URL(backendUrl)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")
                    connectTimeout = connectTimeoutMs
                    readTimeout = readTimeoutMs
                    doOutput = true
                }

                OutputStreamWriter(conn.outputStream).use { writer ->
                    writer.write(jsonPayload.toString())
                    writer.flush()
                }

                val responseCode = conn.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val stream = conn.inputStream
                    val responseText = BufferedReader(InputStreamReader(stream)).use { it.readText() }
                    val result = parseJsonResponse(responseText, nowMs)
                    onResult(result)
                } else {
                    onResult(
                        AiAnalysisResult(
                            exercise = exercise,
                            cameraUsable = true,
                            cameraWarning = "AI Proxy HTTP $responseCode",
                            errors = listOf("HTTP_$responseCode"),
                            timestampMs = nowMs
                        )
                    )
                }
            } catch (e: Exception) {
                onResult(
                    AiAnalysisResult(
                        exercise = exercise,
                        cameraUsable = true,
                        cameraWarning = "AI Proxy unreachable: ${e.localizedMessage}",
                        errors = listOf(e.javaClass.simpleName),
                        timestampMs = nowMs
                    )
                )
            } finally {
                isBusy.set(false)
            }
        }
    }

    private fun parseJsonResponse(jsonStr: String, timestampMs: Long): AiAnalysisResult {
        return try {
            val json = JSONObject(jsonStr)
            val errorsArray = json.optJSONArray("errors")
            val errorsList = mutableListOf<String>()
            if (errorsArray != null) {
                for (i in 0 until errorsArray.length()) {
                    errorsList.add(errorsArray.getString(i))
                }
            }

            AiAnalysisResult(
                exercise = json.optString("exercise", ""),
                exerciseConfidence = json.optDouble("exercise_confidence", 0.0),
                personVisible = json.optBoolean("person_visible", false),
                bodyVisibility = BodyVisibility.fromString(json.optString("body_visibility", "UNKNOWN")),
                cameraUsable = json.optBoolean("camera_usable", true),
                cameraWarning = json.optString("camera_warning", null),
                movementDetected = json.optBoolean("movement_detected", false),
                movementPhase = json.optString("movement_phase", "UNKNOWN"),
                exerciseMatch = json.optBoolean("exercise_match", false),
                exerciseMatchConfidence = json.optDouble("exercise_match_confidence", 0.0),
                currentForm = CurrentForm.fromString(json.optString("current_form", "UNKNOWN")),
                formScore = json.optInt("form_score", 0),
                formConfidence = json.optDouble("form_confidence", 0.0),
                repState = AiRepState.fromString(json.optString("rep_state", "INVALID")),
                repLikelihood = json.optDouble("rep_likelihood", 0.0),
                repComplete = json.optBoolean("rep_complete", false),
                anomalyDetected = json.optBoolean("anomaly_detected", false),
                anomalyType = json.optString("anomaly_type", null),
                errors = errorsList,
                feedback = json.optString("feedback", ""),
                timestampMs = timestampMs
            )
        } catch (e: Exception) {
            AiAnalysisResult(
                cameraUsable = true,
                cameraWarning = "JSON parse failed",
                errors = listOf("JSON_PARSE_ERROR"),
                timestampMs = timestampMs
            )
        }
    }

    override fun close() {
        executor.shutdown()
    }
}
