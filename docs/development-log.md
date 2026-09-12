# Development log

This is a short public record of repository work. The local `memory/logs` file
keeps the full command history and machine-specific details. The `memory`
folder is ignored and must not be pushed.

## 2026-09-13

### Repository setup

- Cloned `https://github.com/Amitk003/Pulse.git` into the local workspace.
- Confirmed the starting branch was `main`.
- Confirmed the starting commit was `64cddb1`.
- Created and pushed branch `codex/docs-memory-foundation` from `main`.
- No package was installed or uninstalled by the work in this log.

### Baseline checks

- `gradlew.bat test` downloaded the pinned Gradle 8.10.2 wrapper but stopped because no Android SDK path was available.
- `gradlew.bat :movement:test` passed after the wrapper was available.
- The full Android test, lint, and APK checks still need Android Studio and a valid SDK path.

### Documentation changes

- Rewrote the README in simple product language.
- Added the Unity integration boundary and bridge rules.
- Added the public product roadmap.
- Updated the Android development guide for the current modules.
- Added this development log.

### Build reproducibility

- Pinned MediaPipe tasks-vision to `0.10.26.1` in the camera module.
- Updated the camera source and camera document to show the pinned version.

### Camera result flow

- Fixed the live camera path so it processes a `PoseDetection` only from the MediaPipe result callback.
- Removed the old immediate read of the previous pose result after `detectAsync`.
- Added a short idle wait before a set result is built so the in-flight result is not lost.
- The camera module still needs Android SDK compilation and real phone testing.

### Manual work still needed

- Install Android Studio and the Android SDK.
- Set the local SDK path in `local.properties` or Android Studio settings.
- Connect a real Android phone for camera and performance checks.
- Review licenses before public distribution.
