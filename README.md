# Pulse

## Train in real life. Watch your progress grow.

Pulse is a private workout companion that turns each real exercise set into useful feedback and visible progress. It counts repetitions, checks movement quality, and grows a personal training clone as the user stays consistent.

Pulse is designed for real places and real conditions:

- Core training works without a network connection.
- Camera data stays on the device.
- Every result is easy to understand.
- Progress rewards good form and regular training.
- A small game layer makes the next workout feel worth doing.

## What makes Pulse useful

Most workout apps record numbers. Pulse explains what those numbers mean.

1. Pick an exercise.
2. Place the whole body inside the camera view.
3. Complete a set.
4. Get repetition, form, and mistake feedback.
5. See the result improve the clone.
6. Use the clone in a short offline challenge.

The result is a simple loop: train, learn, improve, and return.

## Product highlights

### Clear movement feedback

Pulse uses on-device pose tracking to count repetitions and check useful movement points. It can point out issues such as shallow depth, a rounded back, or an incomplete repetition.

### Progress with meaning

The clone grows from training quality, not only from raw volume. Strength, form, consistency, recovery, and level give the user a quick view of progress.

### Helpful coaching

The coach uses recent sessions to give a specific next tip. It does not need a network connection or a cloud language service for the core experience.

### Fair challenges

Challenge results combine clone power and user skill. A user with better timing and form can win, even when the other clone is stronger.

### Private by design

Video and pose landmarks are processed locally. The app stores only the workout result needed for progress, such as repetitions, score, mistakes, and time.

## Product direction

The first product version focuses on three exercises:

- Squat
- Push-up
- Hinge movement

The app is planned as an Android application using Kotlin and Jetpack Compose. CameraX handles the camera view. MediaPipe Pose Landmarker supports local pose tracking. Room and DataStore support local storage. WorkManager supports scheduled local work.

The design keeps the core loop useful when the phone is offline. Network features, such as weather data, are optional additions and must never block training.

## Project documentation

- [Product overview](docs/overview.md)
- [Architecture](docs/architecture.md)
- [P1 movement and camera plan](docs/p1-ml-camera.md)
- [Development guide](docs/development.md)
- [Testing guide](docs/testing.md)

## Current repository state

This repository is being built in small, reviewable changes. The current branch contains the product direction and the technical foundation documents. Source folders will be added behind focused branches as each part is implemented.

## License

No public license has been selected yet. Do not reuse this project until a license is added.
