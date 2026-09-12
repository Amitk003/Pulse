# AI Validation Architecture

This document describes the authoritative AI validation layer for exercise recognition, repetition verification, and form analysis in Pulse.

## Overview

Pulse uses a hybrid architecture combining local real-time tracking with periodic Gemini Vision AI validation:

1. **Local Tracking (CameraX + MediaPipe):** Runs at ~20-30 FPS. Handles low-latency local phase estimation, joint angle math, and candidate repetition detection. MediaPipe alone is **not** the final authority for rep counting.
2. **AI Proxy Service (FastAPI + Gemini 3.7 Flash):** Runs asynchronously at ~1-3 FPS. Processes short temporal frame clips (~1-2 seconds) to evaluate exercise identity, body visibility, movement sequence, and anomaly detection (e.g. couch sitting, head movements).
3. **Authoritative Fusion (SetSession):** When local tracking completes a candidate repetition, `SetSession` verifies it against the latest valid AI assessment. If the AI result is fresh (<= 1500 ms), matching, non-anomalous, and sufficiently confident, the repetition is authoritatively counted.

## Security & API Key Privacy

The Gemini API key resides strictly in `backend/.env` on the proxy service and is **never** compiled into the Android application binary.

## Supported Exercises

- `squat`
- `push-up`
- `hinge`
- `lunge`
- `jumping-jack`

## Anomaly & False Positive Protection

Reps are rejected if:
- Unrelated movement is detected (e.g., sitting on a couch, standing up, head movement, walking).
- Required body parts are not visible (`INSUFFICIENT` body visibility).
- Movement does not match the requested exercise.
- AI analysis result is stale (> 1500 ms old).
