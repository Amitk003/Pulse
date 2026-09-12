# Development guide

## Repository state

The repository is being built in focused branches from `main`. Each branch should have one clear purpose and small commits that can be reviewed on their own.

Use branch names such as:

- `feature/p1-camera`
- `feature/p1-rep-counter`
- `feature/p1-form-scoring`
- `feature/p2-local-data`

Do not use internal planning words in branch or commit names. Keep commit messages short and describe the change, for example `Add squat rep state machine`.

## Current setup

The repository now contains Android and JVM modules. The intended local setup is:

- Android Studio with the Android SDK and platform tools.
- Java 17.
- A phone or emulator for camera checks.
- Network access for the first Gradle dependency download.

The exact Android Studio version and local SDK path belong in the local handover
memory. Do not commit `local.properties`.

## Local setup

The intended setup is:

1. Install Android Studio with the Android SDK and an emulator or connect a test phone.
2. Open the repository in Android Studio.
3. Set the SDK path when Android Studio asks for it.
4. Let Gradle finish its first sync.
5. Add only local configuration values to `local.properties`.
6. Run the app on a device with a working camera.

No secret key should be committed. Optional network features must have a safe local fallback.

## Change rules

- Keep public data contracts small and stable.
- Keep camera code separate from UI code.
- Keep display text out of stored mistake identifiers.
- Add tests with every movement rule.
- Do not add a dependency without recording why it is needed.
- Do not store video or raw landmarks.
- Use ASCII punctuation in source and documentation. Do not add em dashes or emoji characters.
- Keep the Unity bridge separate from Room and Android UI code.
- Keep development buttons and debug shortcuts out of release builds.
- Pin dependency versions. Do not use dynamic versions such as `latest.release`.

## Useful checks

After source code is added, every branch should run the checks supported by the project:

```text
./gradlew test
./gradlew lint
./gradlew assembleDebug
```

Run the Android checks on Windows with `gradlew.bat` if the shell does not support the Unix wrapper command.

Before committing, inspect the diff, check for secrets, and confirm that generated files are ignored.

If a check cannot run because a local SDK or device is missing, record the exact
reason in `docs/development-log.md` and in the local `memory/logs` file.

## Review handoff

At the end of a branch, provide:

- What changed
- Which checks passed
- Which checks could not run
- Any manual device steps
- Any follow-up work needed

Branches are reviewed before another focused branch is started.
