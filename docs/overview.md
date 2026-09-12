# Product overview

## Purpose

Pulse helps a person train with better feedback. It watches one exercise set through the phone camera, counts repetitions, checks basic form, and saves a useful result.

The product should feel simple:

1. Choose an exercise.
2. Set up the camera.
3. Follow the live feedback.
4. Finish the set.
5. Read the result and next tip.
6. See progress change.

## Main user problem

Many people can start a workout, but they do not know if their movement is correct or if their effort is building progress. Pulse gives a quick answer after every set without needing special equipment or a network connection.

## Product rules

- The training loop must work offline.
- Camera video and pose landmarks stay on the device.
- A bad camera view must be shown clearly before counting.
- A result must explain both the score and the next action.
- Network services are optional and cannot block a workout.
- The app must prefer a safe, clear result over a guessed result.

## First supported exercises

| Exercise | Count signal | First form checks |
| --- | --- | --- |
| Squat | Knee angle moves down and back up | Depth, knee alignment, back angle |
| Push-up | Elbow angle moves down and back up | Depth, body line, incomplete rep |
| Hinge | Hip angle moves down and back up | Hip movement, rounded back |

Thresholds are starting values. Device testing must tune them before they are treated as final.

## Result shown to the user

Each completed set produces:

- Exercise name
- Repetition count
- Form score from 0 to 100
- List of detected mistakes
- Set duration
- Short next-step feedback

The result also updates the local progress model. The app does not need to save the video to create this result.

## Out of scope for the first product version

- Cloud video processing
- Required user accounts
- Paid features
- A large exercise library
- A custom 3D avatar system
- A network dependency for counting or saving a set

## Success checks

The first usable version is ready when a user can complete a squat set in airplane mode, see a believable count and form result, close the app, reopen it, and still see the saved progress.
