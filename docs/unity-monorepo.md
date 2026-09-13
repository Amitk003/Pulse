# Unity + Android monorepo

This repository now hosts **both** the Pulse Android app and the Unity arena game
so the shared bridge contract can version atomically with its two consumers.

## Layout

```text
Pulse/
  settings.gradle.kts      Gradle root (Android)
  app/ bridge/ data/ movement/ camera/     Android modules
  unity/                   Unity 6000.6.0f1 project (source only)
    Assets/                Scenes, Settings, _AdventureGame (game), asset packs
    Packages/manifest.json
    ProjectSettings/
    FBX (Unity)/           Raw model source (kept for reference, not imported)
  docs/                    Product + integration docs
  tools/                   Build / handoff scripts
```

No path contains a space going forward (the old `My project (1)` folder was
retired) so Gradle and the Unity export pipeline have clean build paths.

## What is tracked

- `unity/Assets`, `unity/Packages`, `unity/ProjectSettings` and **every `.meta`**
  file (they carry the GUIDs that keep Unity references intact).
- Git LFS is used for large binary Unity assets (`*.png`, `*.fbx`, `*.anim`,
  `*.pdf`, ...). Git history stores a small pointer; the real bytes live in the
  Git LFS store (`.gitattributes`).

## What is ignored (`unity/.gitignore`, stays out of git)

- Unity regenerated folders: `Library`, `Temp`, `Logs`, `Build`, `obj`,
  `UserSettings` (per-machine editor state), `PulseUnityExport` (Android export).
- Unity-generated project files (`*.csproj`, `*.sln`, `*.slnx`) and installers
  (`*.unitypackage`, `*.apk`, `*.aab`).

## Opening the projects

- **Android:** open the repo root in Android Studio and let Gradle sync.
- **Unity:** open `unity/` as a Unity project (Unity `6000.6.0f1`). Unity
  regenerates `Library/`, `Temp/`, `Logs/` and the `.csproj`/`.sln` on first open.

## Cloning with LFS

After `git clone`, run `git lfs pull` (or `git lfs install` first) so the
large binary assets are present; otherwise Unity may report missing imports.

## Building the game for Android

- Separate arena APK (two-app bridge, current target): Unity Build Settings →
  Android, package `com.pulse.arena`, then use the `Pulse > Export Android
  Library` editor menu (`Assets/_AdventureGame/Editor/PulseAndroidExport.cs`).
- Embedded single-app (follow-on): copy the export into `Pulse/unity/` output
  and enable `implementation(project(":unity"))` in `app/build.gradle.kts`.

See [unity-integration.md](unity-integration.md) for the data flow and the
roadmap for the staged merge order.