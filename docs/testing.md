# Testing guide

## Test levels

### Unit tests

Unit tests should cover angle math, smoothing, visibility rules, repetition states, form scoring, and result validation. These tests should not need a camera, Android device, or network.

### Integration tests

Integration tests should cover the path from a completed `SetResult` to a saved session and updated clone state. Test both a normal result and a result with mistakes.

### Device tests

Device checks are required for camera permission, image rotation, preview behavior, analyzer speed, and the full recording flow. A simulator is useful for screens but is not enough for pose tracking.

## P1 acceptance cases

| Case | Expected result |
| --- | --- |
| Full body in view | The selected exercise can count a complete movement |
| Body partly outside view | Counting pauses and a setup message is shown |
| Half movement | No repetition is added |
| Noisy landmark frame | The state does not jump between phases repeatedly |
| Permission denied | The app explains the problem and does not crash |
| Camera rotated | Landmarks and preview use the same orientation |
| Network disabled | Camera processing and result creation still work |
| App reopened | The saved session and progress are still present |

## Manual test checklist

- [ ] Start and stop a recording.
- [ ] Confirm the full-body message appears when needed.
- [ ] Complete five slow squats and check the count.
- [ ] Complete one shallow squat and check the mistake.
- [ ] Try a side camera view.
- [ ] Deny camera permission and recover from the error.
- [ ] Turn on airplane mode and complete a set.
- [ ] Close and reopen the app after saving.
- [ ] Check that no camera video file was created by the app.

## Test data

Use short local recordings only when a test needs repeatable input. Keep test recordings outside the repository or in an ignored local folder. Do not commit a person's face, name, or other identifying information.

## Failure reporting

Every movement failure report should include:

- Phone model and Android version
- Camera position and distance
- Lighting condition
- Exercise and expected count
- Actual count and score
- Whether the whole body was visible
- Analyzer speed or dropped-frame information, when available

This information makes threshold changes easier to review.
