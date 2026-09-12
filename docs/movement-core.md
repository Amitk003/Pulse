# Movement core

This doc explains the P1 movement code in simple words.

## What it does

The movement core turns body angles into a workout result. It does not need a camera or a phone. It takes small numbers as input and returns a clear result.

Steps:

1. Check that the full body is in view.
2. Smooth the joint angle so noisy frames do not cause false counts.
3. Move through READY, DOWN, and UP states.
4. Count one rep only after a full down and up cycle.
5. Score form from simple rules.
6. Return one `SetResult`.

## Files

- `movement/src/main/kotlin/com/pulse/movement/SetResult.kt`
  Shared result with validation.
- `movement/src/main/kotlin/com/pulse/movement/MovementConfig.kt`
  All numbers in one place for tuning.
- `movement/src/main/kotlin/com/pulse/movement/Angles.kt`
  Joint angle math and smoothing.
- `movement/src/main/kotlin/com/pulse/movement/VisibilityGate.kt`
  Full body check before counting.
- `movement/src/main/kotlin/com/pulse/movement/RepCounter.kt`
  Rep state machine for squat, push-up, and hinge.
- `movement/src/main/kotlin/com/pulse/movement/FormScorer.kt`
  Simple form rules with stable mistake names.

## Rules

- No Android imports in this module.
- No camera types in stored data.
- Visibility failure never advances the counter.
- Half movement never counts.
- Mistake names stay lower-case and stable.

## How to run tests

When Java 17 and Android Studio are ready:

```text
./gradlew :movement:test
```

On Windows use:

```text
gradlew.bat :movement:test
```

Camera, permission, and speed tests still need a real phone. See `docs/testing.md`.
