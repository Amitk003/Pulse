# Pose and camera

This doc explains the rest of P1 in simple words. It uses MediaPipe Pose Landmarker for body points and CameraX for the camera view.

## Parts

Pure movement code (tested on JVM):

- `movement/.../PoseFrame.kt`
  Small frame model. Holds 33 visibility values plus named joints.
- `movement/.../PoseDetector.kt`
  Contract for any pose source plus a fake for tests.
- `movement/.../FrameThrottler.kt`
  Keeps 15 to 20 analyzed frames per second. Drops extra frames and drops a frame while busy.
- `movement/.../SetSession.kt`
  Wiring from frame to visibility to rep counter to `SetResult`.

Android code (needs a phone):

- `camera/.../MediapipePoseDetector.kt`
  Wraps Pose Landmarker in LIVE_STREAM mode. Uses `pose_landmarker_lite.task` with one pose and 0.5 confidence values. Maps 33 points to `PoseFrame`. Only the first pose is used.
- `camera/.../CameraCapture.kt`
  CameraX preview plus analysis with KEEP_ONLY_LATEST. Rotates each frame from ImageInfo so points match the preview. Sends small updates (reps and hint) to the screen, never full landmarks.
- `camera/build.gradle.kts`
  Lists CameraX 1.3.4 and tasks-vision latest.release. Not included in settings yet so the movement build stays green until the Android SDK is ready.

## Setup

1. Put `pose_landmarker_lite.task` in `app/src/main/assets`.
2. Add `include(":camera")` to `settings.gradle.kts` after the SDK is ready.
3. Ask for camera permission only when recording starts.
4. Start preview and analysis together. Stop both in onPause or onStop.
5. If permission is denied, show help text and do not crash.

## Device checks still open

- One person and two people in frame.
- Front, side, and diagonal views.
- Low light and bright backlight.
- Near and far distance.
- Rotation behavior.
- Analyzer time and dropped frame count on a real phone.
- Airplane mode still counts and saves.
- Proof that no video or landmark file is saved.

## Test commands

Pure tests (need Java 17):

```text
./gradlew :movement:test
```

On Windows use `gradlew.bat :movement:test`.

Camera checks need a real phone and cannot run in a simulator alone.
