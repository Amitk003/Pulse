# Pulse — Train IRL. Grow Your Clone. Fight.

> Android (Kotlin + Compose 2D) hackathon MVP — offline-first, privacy-first fitness game.
> Record a real set → on-device AI counts reps + scores form → clone gains XP → fight bots → top gym boards. Theme reacts to real weather.

**Stack:** Kotlin, Jetpack Compose, CameraX, MediaPipe Pose Landmarker (TFLite), Room + DataStore, WorkManager, OpenWeatherMap (cached + manual override)
**Backend for MVP:** Mock offline-first (no server). `friends.json` + Room only.
**Team:** 5 devs, single `app/` module, parallel branches.

---

## 1. Core Loop (everyone must know this)

```
1. Select exercise (squat / push-up / hinge-deadlift)
2. Record set (CameraX, full-body required)
3. On-device AI → reps + formScore 0-100 + mistakes[]
4. Coach line (references last 10 sessions)
5. Clone += XP (reps * formScore * comebackMultiplier)
6. Optional: weather theme changes, bot fight, gym board
```

**Fairness formula (fights):** `final = 0.6 * clonePower + 0.4 * skillMinigame` + Rookie (<Lv5) / Pro (<Lv15) / Elite brackets.
**Decay:** No loss days 0-7. After day 7: `strength *= 0.995^extraDays`. Comeback: next 2 quality sessions `1.75x XP`.
**Privacy:** Video + landmarks never leave device. Only `reps, score, mistakes` stored.

---

## 2. Architecture

```
UI (Compose): Home | Record | Result | Fight | Board
                ↑        ↑        ↑         ↑       ↑
Domain:   CloneStateManager  CoachEngine  FightEngine  DecayManager  ChallengeManager
                ↑                ↑             ↑            ↑               ↑
ML:       PoseDetector → RepCounter → FormScorer → VisibilityGate
                ↑                                                      ↑
Data:     Room(Session, Clone, Flag) + DataStore(gymCode, theme, settings) + WorkManager(decayWorker)
                ↑
Weather:  Location → WeatherApi → Cache → ThemeMapper → Compose Theme (Sunny/Rain/Snow/Night)
```

### Shared contracts (do not break these)

```kotlin
// P1 → P2/P3
data class SetResult(exercise: String, reps: Int, formScore: Int, mistakes: List<String>, durationSec: Int)
interface PoseCallback { fun onPose(landmarks: List<Landmark>, visibleCount: Int); fun onSetComplete(r: SetResult) }

// P2 exposes to P3/P4
fun CloneStateManager.applySession(r: SetResult): LevelUpInfo // XP = reps * formScore * comebackMult
fun DecayManager.checkDecay(now: Long): DecayInfo
data class Clone(strength: Float, xp: Int, level: Int, consistency: Float, formMastery: Float, recovery: Float, lastTrainedAt: Long, mistakeMemory: List<String>)

// P4 exposes to P3
fun CoachEngine.lineFor(history: List<Session>): String
fun FightEngine.score(clone: Clone, skill: Float): FightResult // 60/40

// P5 exposes to P3
fun WeatherThemeEngine.currentTheme(): AppTheme // SUNNY | RAIN | SNOW | NIGHT
```

### Room schema (P2 owns)

* `Session(id, exercise, reps, formScore, mistakesCsv, durationSec, ts, flagged)`
* `CloneState(id=1, strength, xp, level, consistency, formMastery, recovery, lastTrainedAt, mistakeMemoryJson, comebackLeft)`

---

## 3. Team — 5 Roles Deep Dive

### P1 — ML / Camera Owner
**Owns:** `ml/PoseDetector.kt, ml/RepCounter.kt, ml/FormScorer.kt, ml/VisibilityGate.kt, camera/CameraCapture.kt`
**Must deliver:** 15-20fps pose on mid-range phone, rep state machines, mistake list, blocking overlay if body not fully visible.
**Key logic:**
* Squat: knee angle `170° → <95° → 170°` = 1 rep. Flag `shallow` if min >100°, `knee-valgus` if ankle-knee-hip ratio off, `back-round` if shoulder-hip-knee <150° at bottom.
* Push-up: elbow `160° → <90° → 160°`. Flag `sagging`, `half-rep`.
* Hinge: hip angle `170° → <120° → 170°`. Flag `rounded-back`.
* VisibilityGate: require ≥20/33 landmarks with `visibility>0.5`, else `NEED_FULL_BODY`.
**Dependencies:** Provides `SetResult` to P2/P3. Needs nothing else to start.
**Demo moment:** Live squat counting + "move back, full body not visible" gate.
**Risks → fix:** Low fps → 720p + GPU delegate + process every 2nd frame. Deadlift noisy → label beta, score only hinge+back.

### P2 — Clone / Data / Progression Owner
**Owns:** `data/* (AppDatabase, SessionDao, CloneDao, Entities), domain/CloneStateManager.kt, domain/DecayManager.kt, domain/StatsCalculator.kt`
**Must deliver:** Offline persistence, XP/level, soft decay + comeback, secondary stats.
**Formulas (exact):**
* `xpGain = reps * formScore * (if comebackLeft>0 1.75 else 1.0)`; `strength += xpGain * 0.01`; `level = xp/500 + 1`
* Decay worker (WorkManager daily): `if daysSince > 7: strength *= 0.995^(daysSince-7)`
* Comeback: if `daysSince>=7` set `comebackLeft=2` on return. Decrement per quality session (`formScore>=60`).
* `consistency = sessionsLast7d / 5 capped 1.0`; `formMastery = avg(formScore last 10)`; `recovery = clamp(avgGapHours/24, 0..1)`
* Debug: `fun simulateDays(d: Int)` shifts `lastTrainedAt` back for demo.
**Dependencies:** Consumes `SetResult` from P1. Exposes `Clone` flow to P3/P4.
**Demo moment:** "Simulate +8 days → strength drops → next session 1.75x XP comeback" button.
**Risks → fix:** Schema churn → freeze entities at H4, only add columns after.

### P3 — Game UI / Clone Visual / Fight UI Owner
**Owns:** `ui/* (Home, ExerciseSelect, Record, Result, Fight, Board, components/CloneAvatar.kt), navigation`
**Must deliver:** All screens, nav, 2D avatar reacts to stats, XP bar animation, fight minigame.
**Screens:**
* `Home`: CloneAvatar (height = `1+strength/200`, armWidth = `strength`, color shifts by level) + stats bars + last coach line + weather background slot.
* `Record`: Camera preview + rep counter overlay + form cue chips + visibility warning.
* `Result`: reps big number, formScore ring, mistakes list, XP `+128` animation, next-tip button.
* `Fight`: vs-bot screen, tap-timing bar (hit green zone for skill 0-100), result `60/40` breakdown bars.
* `Board`: tabs Global / Gym / Form King.
**Dependencies:** Needs `SetResult` stub from P1 (use fake button `+1 rep` until ML lands), `Clone` flow from P2, `Theme` from P5, `FightResult` from P4.
**Demo moment:** Tapping fight minigame to win despite weaker clone (proves fairness).
**Risks → fix:** Camera + Compose recomposition jank → hoist camera to separate lifecycle, don't recompose preview.

### P4 — Coach Personality / Fight Logic / Social Owner
**Owns:** `domain/CoachEngine.kt, domain/FightEngine.kt, domain/ChallengeManager.kt, data/MockLeaderboard.json`
**Must deliver:** Rule-based coach, hybrid fight scoring, mock social.
**CoachEngine (no LLM in MVP):**
* Keep last 10 `mistakes`. Count frequency. Templates:
  * 3x `rounded-back` → "You rounded your back on the last 3 hinge sets. Today: hips back, chest up."
  * 2x `shallow` → "Last 2 squats were shallow. Aim hips below knees."
  * `formScore>=85` → "Clean set. Best form this week — keep it."
  * Fallback → "Solid volume. Next set focus depth."
* `ChallengeManager`: `friends.json` (8 fake friends with strength/form), gym filter by `DataStore gymCode` (default `PULSE-GYM-01`), `Form King = MAX(formScore) last 7d` query, challenge card "Beat {name}'s {score}".
**FightEngine:** `power = strength*0.7 + formMastery*0.2 + consistency*0.1 normalized 0-100; final = power*0.6 + skill*0.4; bracket check` — upset possible via skill.
**Dependencies:** Needs Room history from P2, UI slot from P3.
**Demo moment:** Coach line referencing previous set + "Beat my squat score" challenge.
**Risks → fix:** Generic feel → always include 1 specific number/exercise name.

### P5 — Weather Theme / Platform / Anti-cheat / Release Owner
**Owns:** `weather/* (WeatherApi, WeatherCache, ThemeMapper, Theme.kt), util/AntiCheat.kt, workers/DecayWorker.kt, build/release`
**Must deliver:** Dynamic theme that never breaks demo, anti-cheat flags, performance, APK.
**WeatherThemeEngine:**
* `GET api.openweathermap.org/data/2.5/weather?lat,lon&appid=KEY` → map `Clear→SUNNY, Rain/Drizzle→RAIN, Snow→SNOW, else→NIGHT if hour>19`.
* Cache in DataStore 1h. **Manual override dropdown (Sunny/Rain/Snow/Night) always visible in AppBar for demo.** Theme changes: `SUNNY=yellow sky+sun particles, RAIN=slate+rain particles+darker, SNOW=white+flake, NIGHT=dark+stars`. Clone gets raincoat/beanie accessory (simple Compose draw).
* If offline/API fail → cached or manual, never crash.
**AntiCheat:** `flag if visibleCount<20` OR `strengthDelta > 3*stddev` OR face aspect jumps >40% mid-set → `Session.flagged=true`, show "Review needed" chip, still grant XP in MVP.
**Platform:** Thermal throttle (if `fps<12` for 5s → drop to 15fps + 640p), WorkManager daily decay, release APK + screen-record backup.
**Demo moment:** Toggle Rain → whole app + fight arena changes live.
**Risks → fix:** No location/API key at venue → manual toggle saves demo. Request location early, timeout 3s.

---

## 4. Phase-wise Roadmap (each phase has parallel tasks for all 5)

### Phase 0 — Foundation (H0-4) ✅ Exit: app launches offline, nav works
* P1: CameraX permission + preview composable stub, add MediaPipe dep `com.google.mediapipe:tasks-vision:0.10.14`.
* P2: Room + DataStore + seed clone `(strength=10, xp=0, lv1)` + DAOs.
* P3: BottomNav (Home/Record/Fight/Board) + ExerciseSelect + empty CloneAvatar.
* P4: `friends.json` (8 entries) + coach template file + bracket constants.
* P5: Git branches `p1/camera…p5/platform`, `BuildConfig.WEATHER_KEY` placeholder, AppBar theme dropdown stub.
* 🤝 Checkpoint: merge to `main`, tag `v0-shell`.

### Phase 1 — Core Training Loop (H4-12) ✅ Exit: live squat counted on phone
* P1: PoseDetector + VisibilityGate + 3 RepCounters + FormScorer thresholds. Test on 2 devices.
* P2: `applySession()` + Session insert + Clone update flow.
* P3: Wire Record→Result with fake `+1 rep` button first, then real callback; rep overlay + score ring.
* P4: Mistake taxonomy freeze (`shallow, knee-valgus, back-round, half-rep, sagging`).
* P5: Fps logger + frame-skip logic.
* 🤝 Integration #1 (H12): P1+P2+P3 merge, tag `v1-loop`. Test airplane mode.

### Phase 2 — Personality + Decay + Secondary Stats (H12-18) ✅ Exit: coach references history, decay demo works
* P2: mistakeMemory (last 10) + DecayWorker + comebackLeft + 3 secondary stats + Simulate+8d button.
* P4: CoachEngine frequency logic + FightEngine formula + unit test `weaker+highSkill beats stronger+lowSkill`.
* P3: Result coach card + XP animation + Home stats bars (Consistency/Form/Recovery).
* P1: Tune angle thresholds from Phase 1 recordings.
* P5: Schedule daily worker + persist comeback state.
* 🤝 Checkpoint: tag `v2-clone`.

### Phase 3 — Weather Themes + Polish (H18-22) ✅ Exit: 4 themes switch live
* P5: WeatherApi + cache + ThemeMapper + particles (Compose Canvas) + accessory draws.
* P3: Apply theme to Home/Fight backgrounds + AppBar switcher.
* P2: Persist theme pref in DataStore.
* P1/P4: Bugfix buffer (pose jank, coach wording).
* ✅ Must: demo works with Wi-Fi off (manual toggle).

### Phase 4 — Fights + Fairness (H22-26) ✅ Exit: offline bot fight playable
* P4: Bot roster (3 difficulties) + bracket enforcement.
* P3: Fight arena UI + timing bar + 60/40 breakdown bars + win/lose animation.
* P2: Expose `powerScore()` + store `FightResult` history.
* P1: Optional: use pose for "power-up squats" before fight (stretch).
* P5: Balance log + ensure <100ms result calc.
* 🤝 Integration #2 (H22-26): tag `v3-fight`.

### Phase 5 — Social + Anti-cheat (H26-30) ✅ Exit: boards work offline
* P4: Gym-code filter + Form King query + Challenge cards + share-text intent.
* P1+P5: Visibility/distance/jump flags → `flagged` column + UI chip.
* P2: Flagged query + leaderboard DAO (`ORDER BY strength`, `ORDER BY formScore`).
* P3: Board tabs Global/Gym/Form King + challenge button.
* 🤝 Checkpoint: tag `v4-social`.

### Phase 6 — Freeze + Demo (H30-32+) ✅ Exit: APK + backup video + slides
* P5: Release APK (`./gradlew assembleDebug`), screen-record full loop, slides (loop diagram, 60/40, privacy note).
* P3: 3-min script rehearsal: record → coach → rain toggle → decay simulate → comeback → fight win → board.
* P1/P2/P4: Bugbash, battery test (1 full set <5% drain target), crash-free run.
* 🛑 Freeze: no new features after H30, only fixes. Tag `vM-present`.

---

## 5. File Ownership Map (avoid conflicts)

```
P1: ml/**, camera/**
P2: data/**, domain/CloneStateManager.kt, domain/DecayManager.kt, domain/StatsCalculator.kt, workers/DecayWorker.kt (with P5)
P3: ui/**, navigation/**, MainActivity.kt (shared — coordinate)
P4: domain/CoachEngine.kt, domain/FightEngine.kt, domain/ChallengeManager.kt, data/MockLeaderboard.json
P5: weather/**, util/AntiCheat.kt, build.gradle.kts (shared — announce changes)
```

Branches: `p1/camera, p2/clone, p3/ui, p4/coach, p5/platform` → PR to `main` only at checkpoints above.

---

## 6. How to Run (MVP)

1. Android Studio Hedgehog+, minSdk 26, targetSdk 34.
2. Add `WEATHER_KEY=your_key` to `local.properties` (optional — manual toggle works without).
3. Grant Camera + Location. `Run > app`. For demo without gym: use `Simulate +8 days` + theme dropdown.
4. Airplane-mode test: record set → result → fight → board must all work.

## 7. Post-Hackathon (not in MVP)

P1: custom TFLite form classifier. P2+P4: Firebase Auth/Firestore sync + server anomaly detection. P3: Filament 3D clone / Unity fight module. P5: Play Internal track + Crashlytics + Remote Config flags. Monetization: cosmetics only, never pay-to-win.
