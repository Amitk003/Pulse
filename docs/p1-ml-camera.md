# P1 movement and camera plan

## Role goal

P1 owns the camera input and the first movement analysis path. The goal is a stable local result for squat, push-up, and hinge sets.

The output is not a medical assessment. It is workout guidance. The app must use careful wording and show when the camera view is not reliable.

## Ownership

P1 owns these planned areas:

- `ml/PoseDetector.kt`
- `ml/RepCounter.kt`
- `ml/FormScorer.kt`
- `ml/VisibilityGate.kt`
- `camera/CameraCapture.kt`

P1 provides `SetResult` to the progress and UI parts of the app. P1 should keep camera-specific types out of the data layer.

## Frame pipeline

1. Request camera permission when the user starts recording.
2. Start a CameraX preview and image analysis use case.
3. Convert each selected frame to the pose model input format.
4. Run local pose detection.
5. Check landmark count and visibility.
6. Run the selected exercise state machine.
7. Score form from the complete set.
8. Return one `SetResult` when the user ends the set.

The analyzer should process a bounded frame rate. It should drop a new frame when the previous frame is still being processed instead of building a queue.

## Visibility gate

The first gate should require at least 20 of 33 body landmarks with visibility above `0.5`. The threshold is a starting rule, not a promise that it works for every room or camera.

When the gate fails:

- Do not advance the repetition state machine.
- Show a short setup message.
- Keep the last valid count visible.
- Record a diagnostic reason for development logs only.

The gate must be tested with different distances, lighting, clothing, camera angles, and partial occlusion.

## Rep state machines

Each exercise should use a small state machine with `READY`, `DOWN`, and `UP` states. A repetition is counted only after a complete down and up movement.

Starting angle ranges:

| Exercise | Start | Down | Complete |
| --- | --- | --- | --- |
| Squat | Knee near 170 degrees | Knee below 95 degrees | Knee returns near 170 degrees |
| Push-up | Elbow near 160 degrees | Elbow below 90 degrees | Elbow returns near 160 degrees |
| Hinge | Hip near 170 degrees | Hip below 120 degrees | Hip returns near 170 degrees |

Use a small smoothing window and a minimum state duration to reduce double counts caused by noisy landmarks. Keep these values in one configuration object so they can be tuned without changing the state machine.

## Form checks

The first checks are simple and explainable:

- Squat: shallow depth, knee alignment, and back angle
- Push-up: incomplete depth, sagging body line, and half repetition
- Hinge: incomplete hip movement and rounded back

Mistake identifiers should stay stable and use lower-case names such as `shallow`, `knee-valgus`, `back-round`, `half-rep`, `sagging`, and `rounded-back`.

## Performance target

The first target is a stable 15 to 20 analyzed frames per second on a mid-range Android phone. If processing falls behind, the analyzer should skip frames and keep the preview responsive. The UI must not render every landmark update as a full screen recomposition.

Measure performance on a real device before changing thresholds. A fast but unstable counter is not an improvement.

## Test plan for P1

- Unit test joint-angle calculation with known points.
- Unit test every state transition.
- Unit test incomplete movement does not count.
- Unit test visibility failure blocks counting.
- Test one person and two people in the frame.
- Test front, side, and diagonal camera positions.
- Test low light and bright backlight.
- Test camera rotation and permission denial.
- Record analyzer time and dropped-frame count on a real device.

## Safety and privacy notes

The app should not claim that a form score proves an exercise is safe. The user can stop a set at any time. Camera frames and raw landmarks are temporary processing data and must not be uploaded or saved by the first implementation.
