# Development guide

## Repository state

The repository is being built in focused branches from `main`. Each branch should have one clear purpose and small commits that can be reviewed on their own.

Use branch names such as:

- `feature/p1-camera`
- `feature/p1-rep-counter`
- `feature/p1-form-scoring`
- `feature/p2-local-data`

Do not use internal planning words in branch or commit names. Keep commit messages short and describe the change, for example `Add squat rep state machine`.

## Before source code exists

The Android project setup still needs to be added. When it is added, document the exact Android Studio version, Gradle version, Android SDK level, and Java version in this file.

## Local setup target

The intended setup is:

1. Install Android Studio with the Android SDK and an emulator or connect a test phone.
2. Open the repository in Android Studio.
3. Let Gradle finish its first sync.
4. Add only local configuration values to `local.properties`.
5. Run the app on a device with a working camera.

No secret key should be committed. Optional network features must have a safe local fallback.

## Change rules

- Keep public data contracts small and stable.
- Keep camera code separate from UI code.
- Keep display text out of stored mistake identifiers.
- Add tests with every movement rule.
- Do not add a dependency without recording why it is needed.
- Do not store video or raw landmarks.
- Use ASCII punctuation in source and documentation. Do not add em dashes or emoji characters.

## Useful checks

After source code is added, every branch should run the checks supported by the project:

```text
./gradlew test
./gradlew lint
./gradlew assembleDebug
```

Run the Android checks on Windows with `gradlew.bat` if the shell does not support the Unix wrapper command.

Before committing, inspect the diff, check for secrets, and confirm that generated files are ignored.

## Review handoff

At the end of a branch, provide:

- What changed
- Which checks passed
- Which checks could not run
- Any manual device steps
- Any follow-up work needed

Branches are reviewed before another focused branch is started.
