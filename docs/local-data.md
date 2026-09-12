# Local data

This doc explains where Pulse stores training and progress. Simple words only.

## Parts

- `data/.../SessionEntity.kt` - one finished set: exercise, reps, score, mistakes, time, date, review flag.
- `data/.../CloneEntity.kt` - the clone. Only one row. Strength, XP, level, three extra stats, last train date, last 10 mistake lists, comeback count left.
- `data/.../SessionDao.kt`, `CloneDao.kt` - read and write functions for the tables.
- `data/.../PulseDatabase.kt` - the Room database, version 1.
- `data/.../DecayManager.kt` - soft strength loss. Nothing for 7 days, then times 0.995 per extra day.
- `data/.../StatsCalculator.kt` - consistency (sets in last 7 days out of 5), form mastery (last 10 scores), recovery (rest gaps), plus mistake text packing.
- `data/.../CloneStateManager.kt` - turns one `SetResult` into XP and a new clone state. Order: decay first, then comeback check, then XP. Comeback pays 1.75 times XP for 2 quality sets (score 60 or more) after a break of 7 or more days. Level is XP divided by 500 plus 1.
- `data/.../PulseRepository.kt` - the only door screens use. Save a set, watch the clone as a Flow, shift the last train date for the demo.
- `data/.../PulseSettings.kt` - small settings in DataStore: gym code and theme choice for later work.

## Rules

- Video and landmarks are never stored. Only small results go in.
- Mistake names stay fixed lower-case ids, joined by comma. UI text stays out.
- Progress math has no Android code, so plain unit tests cover it.
- The app asks the repository to save right after Finish, then shows what the set earned.

## Checks

- `gradlew.bat :data:testDebugUnitTest` - math tests, no phone needed.
- Real phone test `PulseRepositoryTest` saves sets on the device and checks XP, decay, and comeback.
- Demo: train a set, close the app, open it again, progress is still there. Tap the debug button to jump 8 days ahead, then train and see the comeback bonus.

## What is next

- The coach (P4) reads the stored mistake lists for personal tips.
- Fights (P4) read strength, level, and form mastery.
- A daily worker can apply decay in the background later. Today decay runs when the app opens and saves.
