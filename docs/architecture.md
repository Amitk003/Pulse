# Architecture

## Current technology

- Kotlin for application code
- Jetpack Compose for screens
- CameraX for camera preview and image frames
- MediaPipe Pose Landmarker for local body landmarks
- Room for workout and progress records
- DataStore for small settings
- Java 17 and Kotlin 2.0
- Gradle multi-module build

The repository has four source modules: `movement` for counting and scoring,
`camera` for pose input, `app` for screens, and `data` for storage and progress.
These boundaries describe the current code and should be updated when code changes.

## Layer boundaries

```text
Screen layer
    -> view models and UI state
Domain layer
    -> repetition, form, progress, and challenge rules
Camera and pose layer
    -> frames, landmarks, visibility, and exercise signals
Data layer
    -> Room records and DataStore settings
Platform layer
    -> permissions, background work, and performance controls
```

The future Unity arena is a separate boundary. Pulse owns the camera, workout
result, player identity, and saved progress. Unity owns the 3D arena and fight
simulation. The two sides exchange versioned snapshots and results. Unity must
not read Room tables directly.

The camera layer must not update Compose state on every camera frame. It should send small pose results to a worker or analyzer, then publish only the state needed by the screen.

## Training flow

```text
Camera frame
    -> pose landmarks
    -> full-body visibility check
    -> exercise rep counter
    -> form scorer
    -> SetResult
    -> local session storage
    -> progress update and coach tip
```

If visibility is not good enough, the flow stops before counting. The user sees a direct instruction such as "Move back so your full body is visible." The app must not silently count uncertain movement.

## Shared result contract

The first implementation should keep one small result model between camera, domain, and data code:

```kotlin
data class SetResult(
    val exercise: String,
    val reps: Int,
    val formScore: Int,
    val mistakes: List<String>,
    val durationSec: Int
)
```

Rules for this contract:

- `exercise` uses a fixed lower-case identifier.
- `reps` cannot be negative.
- `formScore` is limited to 0 through 100.
- `mistakes` contains fixed identifiers, not display text.
- `durationSec` cannot be negative.

Display text belongs in the UI or coach layer so that stored data stays stable.

## Local data

Room should store completed sessions and the current clone state. DataStore should store settings such as exercise preferences, theme choice, and optional gym code. Video frames and raw landmarks should not be stored after processing.

The current data records include:

- Session: exercise, repetitions, score, mistakes, duration, time, and review flag
- Clone state: strength, experience, level, consistency, form mastery, recovery, and last training time

The Unity bridge should use a separate `PlayerSnapshot` and `FightResult`
contract. Those contracts must not expose Room entities or Android classes.

## Offline behavior

The following must work without a network:

- Camera preview
- Pose processing
- Repetition counting
- Form scoring
- Saving a session
- Progress updates
- Coach feedback
- Local challenge screen

An optional weather service can change visual theme later. Its failure must fall back to the last saved theme or a manual choice.

## Privacy boundary

The camera permission is used only while the user is recording a set. Processing stays on the device. The app should request only permissions needed for the current feature and should explain why the camera is needed.
