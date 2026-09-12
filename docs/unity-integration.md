# Android and Unity integration

This document explains how Pulse should connect to the Unity arena.

## Ownership

Pulse is the main Android app. It owns:

- Camera permission
- CameraX preview
- Pose detection
- Exercise rules
- Workout results
- Clone progress
- User identity
- Online services

Unity owns:

- The 3D clone
- The arena scene
- Combat input
- Enemy behavior
- Game animations
- Fight results

Unity must not read Room tables directly. Pulse sends a small player snapshot to Unity. Unity sends a small fight result back to Pulse.

## Safe build path

Use two steps:

1. Build the Unity game as its own Android app and start it from Pulse.
2. After that works on real phones, export Unity as an Android Library and embed it in Pulse.

The first step finds mobile control and gameplay problems before Gradle integration makes the work harder.

## Data sent into Unity

The first bridge contract should contain only values the arena needs:

The first contract lives in `bridge/src/main/kotlin/com/pulse/bridge`.

```text
PlayerSnapshot
    playerId
    cloneLevel
    strength
    formMastery
    consistency
    recovery
    gameXp
    unlockedAbilities
    updatedAt
```

The bridge should send JSON or another versioned format. Add a `schemaVersion` field before the first release so old and new app versions can be handled safely.

## Data returned from Unity

Unity should return:

The same module owns the return contract.

```text
FightResult
    playerId
    opponentId
    winner
    playerPower
    playerSkill
    durationSec
    createdAt
```

Pulse saves the result. Unity may show a local result first, but Pulse remains the source of truth.

## Camera boundary

Do not pass camera frames or raw pose landmarks into Unity. The camera path is private workout input. It should produce a validated `SetResult`, then the data layer updates the clone.

The game should receive progress values, not a video and not a live stream of body landmarks.

## Mobile game requirements

Before embedding Unity, the game must support:

- A virtual movement stick
- Swipe camera control
- Touch attack and guard buttons
- Touch jump and interact buttons
- Pause and resume
- Android back button behavior
- Screen rotation rules
- Safe areas and small screens
- Low, medium, and high quality settings

Keyboard and mouse controls can stay for Unity Editor testing, but they cannot be the only controls in the Android build.

## Lifecycle rules

The integration must test:

1. Pulse opens Unity.
2. The user leaves Unity and returns to Pulse.
3. Android sends the app to the background during a fight.
4. The phone locks and unlocks.
5. The user opens Unity again after a completed fight.
6. The user repeats this flow several times without a crash or growing memory use.

The bridge must handle cancellation and incomplete fights. A fight that is closed early must not submit a win or loss by accident.

## Save rules

Do not use Unity `PlayerPrefs` as the long-term source for clone progress. During the bridge work, read the existing Unity values only for compatibility. Move final progress to Pulse data and send snapshots into Unity.

## Manual work required

The following steps need a developer with Android Studio and a real phone:

- Install the Android SDK and platform tools.
- Set the local Android SDK path.
- Import and build the Unity Android project.
- Test a Unity Android build on a phone.
- Check Unity and Pulse Gradle versions together.
- Set up signing keys outside the repository.
- Create test builds before enabling online services.
