# Pulse

## Train in real life. Build your clone.

Pulse is an Android fitness app that turns a real workout into clear feedback and visible progress. The phone camera watches one exercise set, counts valid repetitions, checks basic form, and helps the user improve over time.

The result is more than a workout counter. Each good set helps build a personal training clone. The clone can later enter an offline arena where fitness progress and player skill work together.

## Why Pulse is useful

- Works without a network for the core workout loop.
- Uses on-device pose tracking for camera feedback.
- Explains why a repetition or form score was accepted or rejected.
- Saves small workout results instead of raw video or pose data.
- Turns steady training into simple, visible progress.
- Leaves room for a fair game layer without making the workout feel like a spreadsheet.

## The user journey

1. Choose an exercise.
2. Place the phone so the full body is visible.
3. Follow the live setup and form hints.
4. Complete a set.
5. Review repetitions, form score, mistakes, and next advice.
6. See the clone update from the result.
7. Enter a short challenge when the game layer is enabled.

## Current product foundation

The repository hosts the Android app **and** the Unity arena game in one
monorepo (Unity lives in `unity/`). See [docs/unity-monorepo.md](docs/unity-monorepo.md).

The Android side has four focused modules:

- `movement` contains platform-free pose rules, visibility checks, rep counters, form checks, and unit tests.
- `camera` connects CameraX to the local MediaPipe Pose Landmarker and sends small pose results to the movement module.
- `data` stores finished sets and clone progress with Room and settings with DataStore.
- `app` provides the Android screens for exercise selection, recording, and results.

The `bridge` module owns the shared `PlayerSnapshot` / `FightResult` contract
that Pulse sends to, and receives from, the Unity game.

The first supported exercises are squat, push-up, and hinge movement. Their thresholds are starting values and must be checked on real phones before they are treated as final.

## Privacy by design

The core flow processes camera input on the device. Pulse stores workout results such as exercise, repetitions, form score, mistakes, duration, and time. It does not need to store the video or raw pose landmarks to update progress.

The app must ask for camera access only when recording starts, explain why it needs the camera, and give a clear result when the camera view is not reliable.

## Build and test

Requirements:

- Android Studio with a working Android SDK.
- Java 17.
- A phone or emulator with a camera for camera tests.

Open the repository in Android Studio, let Gradle sync, and run the app on a device.

On Windows, run the checks with:

```text
gradlew.bat :movement:test
gradlew.bat test
gradlew.bat lint
gradlew.bat assembleDebug
```

The movement tests do not need a phone. Camera, permission, storage, and full app checks need Android SDK tools. See [docs/development.md](docs/development.md) and [docs/testing.md](docs/testing.md).

## Project documents

- [Product overview](docs/overview.md)
- [Architecture](docs/architecture.md)
- [Movement core](docs/movement-core.md)
- [Pose and camera](docs/pose-camera.md)
- [Local data](docs/local-data.md)
- [Android and Unity integration](docs/unity-integration.md)
- [Unity + Android monorepo](docs/unity-monorepo.md)
- [Roadmap](docs/roadmap.md)
- [Development guide](docs/development.md)
- [Testing guide](docs/testing.md)
- [Development log](docs/development-log.md)

## Project status

The current repository is an early working foundation. Local movement rules, camera input, local storage, clone progression, and the main training screens are in place. Coaching, the Unity arena, account sync, fights, and public leaderboards still need focused implementation and device testing.

The product is being built in small branches with small commits. Each change should be easy to review, test, and hand over.

## License

Add and review a public license before reusing or distributing this project. Also review the licenses for every third-party model, library, sound, and other asset used by the final app.
