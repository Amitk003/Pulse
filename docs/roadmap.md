# Product roadmap

This roadmap keeps the work small and testable. A later item must not hide a broken earlier item.

## Milestone 1: trusted workout result

Goal: one person can complete one squat set and receive a believable result.

Work:

- Test the camera setup on real Android phones.
- Tune the squat thresholds for distance, light, clothing, and camera angle.
- Show a clear full-body visibility message.
- Count only complete movements.
- Show accepted reps, rejected reps, form score, and next advice.
- Keep camera frames and raw landmarks off disk.
- Add tests for bad views, two people, missing landmarks, and noisy frames.

Done when: a user can complete a squat set in airplane mode and understand the result.

## Milestone 2: useful local progress

Goal: each good workout changes the clone in a clear and fair way.

Work:

- Keep strength, form, consistency, recovery, and game skill separate.
- Add a history screen with recent sets.
- Show why XP changed.
- Keep permanent unlocks safe from short breaks.
- Apply soft readiness decay after a rest period.
- Add a recovery or pause mode.
- Test app close, reopen, and data migration.

Done when: progress is still present after the app is closed and the user can explain how a set changed the clone.

## Milestone 3: three exercise foundation

Goal: support squat, push-up, and hinge with honest feedback.

Work:

- Tune one exercise at a time.
- Keep mistake names stable in stored data.
- Add exercise-specific setup messages.
- Add a small device test set for each exercise.
- Do not call a result medical advice.

Done when: each exercise has documented limits and a tested failure path.

## Milestone 4: mobile arena proof

Goal: run the Unity arena on Android with touch controls.

Work:

- Build the Unity scene as an Android app.
- Add a virtual movement stick.
- Add touch attack, guard, jump, and interact buttons.
- Add mobile quality settings.
- Test pause, resume, back, lock, and unlock.
- Measure memory, battery, load time, and frame rate.
- Remove development-only controls from release builds.

Done when: a person can play one short fight on a mid-range phone without keyboard or mouse.

## Milestone 5: Pulse to Unity bridge

Goal: enter the arena with the current clone snapshot and return a fight result.

Work:

- Start with a separate Unity APK launched by an Android `Intent`.
- Define a versioned `PlayerSnapshot` contract.
- Define a versioned `FightResult` contract.
- Handle cancel, crash, and incomplete fight cases.
- Keep Pulse as the source of truth for progress.
- Move away from Unity-only `PlayerPrefs` progression.

Done when: the same player can train, enter the arena, fight, return, and see the result saved in Pulse.

## Milestone 6: one Android app

Goal: embed Unity into Pulse after the separate build is stable.

Work:

- Export Unity as an Android Library.
- Align Gradle, Android SDK, Java, and NDK versions.
- Add lifecycle and memory tests.
- Add app navigation into and out of Unity.
- Keep release signing outside source control.
- Build an Android App Bundle.

Done when: one signed app can move between Pulse screens and the Unity arena repeatedly.

## Milestone 7: fair competition

Goal: make fights fun without turning exercise into an unfair stat race.

Work:

- Combine fitness progress with real game skill.
- Add a training dummy before online fights.
- Add local challenge rules.
- Add signed or server-checked results before public rankings.
- Separate consistency, form, combat, and improvement rankings.
- Add reporting and account blocking before social features.

Done when: users understand why they won or lost and false scores do not dominate the board.

## Milestone 8: release readiness

Goal: publish a safe, supportable Android app.

Work:

- Add a public project license.
- Review every third-party asset license.
- Add privacy policy and data deletion flow.
- Add crash reporting and useful release logs.
- Meet the current Google Play target SDK requirement.
- Test on low, mid, and high range phones.
- Run signed release builds from a clean machine.
- Review store text, screenshots, permissions, and data safety answers.

Done when: a new user can install, train, play, return later, and delete their data without developer help.
