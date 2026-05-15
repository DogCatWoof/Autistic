# Multi-Module Reorganization Plan

## Context

The app is a 154-file, single-`:app` Gradle module. The goal is to partition code along two axes — **horizontal** (by architectural layer: database, sync, UI) and **vertical** (by feature domain: task, mood, note, etc.) — and move each partition into its own Gradle module. This enables parallel compilation, compile-time isolation, and reduces the code surface area that needs to be loaded when working on any single feature.

---

## Proposed Module Structure (15 modules)

```
:core:common         diagnostics, debug, ExceptionReporter, GlobalErrorHandler
:core:auth           GoogleAuthManager, TokenStore
:core:database       all entities, all DAOs, TaskDatabase, migrations
:core:ui             theme, CommonComponents, ItemColorResolver, ItemIconResolver
:core:notifications  notification channel definitions/registration, channel ID constants,
                     notification permission helpers
:data:sync           SyncOrchestrator, SyncWorker, SyncScheduler, GoogleTasksRemoteSource,
                     GoogleTasksSyncService, CalendarRemoteSource, CalendarSyncService
:data:firestore      FirestoreSource, FirestoreDocuments*, FirestoreSyncService, FirestoreSyncPrefs
:feature:task        TaskRepository, DailyTaskRepository, CalendarRepository,
                     DailyResetWorker, TaskReminderWorker, TaskViewModel, DailyTasksViewModel,
                     TaskListScreen, TaskListItem, TaskListDialogs, DailyTasksSettingsScreen,
                     DailyTaskDialog
:feature:note        NoteRepository, NotesViewModel, NotesScreen
:feature:mood        MoodRepository, MoodCheckInWorker, MoodBroadcastReceiver,
                     MoodViewModel, MoodScreen, showMoodCheckInNotification() (extracted from MainActivity)
:feature:health      HealthConnectRepository, HealthConnectSyncWorker, HealthConnectViewModel,
                     HealthConnectSettingsScreen, HealthConnectDetailDialog, IntegratedHealthScreen
:feature:sequence    SequenceRepository, SequenceRunNotificationManager, SequenceStepReceiver,
                     SequenceViewModel, SequenceRunViewModel, SequenceListScreen
:feature:conversation IntentClassifier, ResponseTemplateRepository, TonePreferencesStore,
                     ConversationViewModel, ConversationScreen
:app                 AutisticApp, MainActivity, AppDrawer, NavBottomSheet, NavPreferencesStore,
                     NavStateStore, NavPreferencesScreen, SettingsScreen, SyncSettingsScreen,
                     QueryLogScreen, all di/*.kt Koin modules
```

---

## Dependency Graph

```
:app → all :feature:*, all :data:*, :core:*
:feature:task → :core:database, :core:auth, :core:common, :core:ui, :core:notifications, :data:sync
:feature:note → :core:database, :core:ui
:feature:mood → :core:database, :core:ui, :core:notifications
:feature:health → :core:database, :core:ui
:feature:sequence → :core:database, :core:common, :core:ui, :core:notifications
:feature:conversation → :core:ui
:data:sync → :core:database, :core:auth
:data:firestore → :core:database, :core:auth
:core:database → (Room only)
:core:auth → :core:common
:core:notifications → :core:common
:core:common → (Android SDK only)
:core:ui → (Compose BOM only)
```

No cycles. `:core:*` never imports `:feature:*` or `:data:*`.

Features using notifications: `:feature:task` (task reminders), `:feature:mood` (mood check-in), `:feature:sequence` (sequence step). Channel registration is centralised in `:core:notifications`; the feature-specific notification content and layouts stay in each feature module.

---

## Key Constraint: TaskDatabase Stays Monolithic

`TaskDatabase` (`data/task/TaskDatabase.kt`) is a single `@Database` class (10 entities from 6 packages). It moves as-is into `:core:database` along with every entity and DAO. The vertical partition for features means their **repositories + ViewModels + UI** are isolated, not their entities.

---

## Required Kotlin Code Changes

### 1. BuildConfig references in library modules
The file `org.meow.autistic.BuildConfig` is only available in `:app`. Fix by injecting API keys via constructor parameters with `""` defaults; pass real values from the Koin module in `:app`:

- `GoogleAuthManager.kt` → add `firebaseWebClientId: String = ""` constructor param

### 2. Fix `MoodBroadcastReceiver` direct DB access
Currently calls `TaskDatabase.getDatabase(context).moodDao()` directly (line 66). Replace with `KoinComponent` injection:
```kotlin
class MoodBroadcastReceiver : BroadcastReceiver(), KoinComponent {
    private val moodDao: MoodDao by inject()
}
```

### 3. Fix `SequenceStepReceiver` direct DB access
Line 28: `val dao = TaskDatabase.getDatabase(context).sequenceDao()`. Same fix — inject `SequenceDao` via `KoinComponent`.

### 4. Fix `SequenceRunNotificationManager` direct DB access (object)
`object SequenceRunNotificationManager` at line 21 calls `TaskDatabase.getDatabase(context)`. Since it's an `object`, pass the DAO as a parameter instead:
```kotlin
object SequenceRunNotificationManager {
    suspend fun update(context: Context, runId: Long, dao: SequenceDao) { ... }
}
```
Update the `SequenceStepReceiver` caller to pass the injected DAO.

### 5. Extract `showNotification()` from MainActivity
`showNotification()` is a top-level function at `MainActivity.kt:319`. It is imported by:
- `MoodCheckInWorker` (moving to `:feature:mood`)
- `SettingsScreen.kt` (staying in `:app`)

Move it to a new file `MoodNotificationHelper.kt` in `:feature:mood`, renamed `showMoodCheckInNotification()`. Also move `R.layout.notification_mood_picker` XML to `:feature:mood`'s `res/layout/`. Both callers update their import.

### 6. No package-name changes
Kotlin package names (`org.meow.autistic.data.task`, etc.) stay unchanged. Only the Gradle module membership changes. This eliminates import churn across the codebase.

### 7. `internal` visibility
`MIGRATION_17_18` and `MIGRATION_18_19` in `TaskDatabase.kt` are already `internal`. They stay `internal` within `:core:database`. No other `internal` members cross module boundaries.

---

## Build Conventions

Create `build-logic/` as a Gradle included build (preferred over `buildSrc` as it can access the parent version catalog):

```
build-logic/
  settings.gradle.kts   # empty or minimal
  build.gradle.kts      # kotlin-dsl plugin
  src/main/kotlin/
    autistic.android-library.gradle.kts          # base: android-library + kotlin, compileSdk/minSdk/jvmToolchain
    autistic.android-library-compose.gradle.kts  # extends base + compose BOM + material3
    autistic.android-library-room.gradle.kts     # extends base + KSP + Room
```

In `settings.gradle.kts` (root): `includeBuild("build-logic")`.

Each library module's `build.gradle.kts` then becomes 5–15 lines. `:app` keeps its full hand-written build file (applicationId, BuildConfig fields, google-services plugin).

`settings.gradle.kts` adds all 14 new module paths to `include(...)`.

---

## Migration Order (Low Risk → High Risk)

### Phase 1 — Build scaffolding (no file moves)
1. Create `build-logic/` with 3 convention plugins
2. Update `settings.gradle.kts` to include all 15 module paths
3. Verify `:app` still builds before any moves

### Phase 2 — Horizontal cores (pure moves, no logic changes)
4. `:core:common` — move diagnostics + debug files; zero cross-imports
5. `:core:ui` — move theme + CommonComponents + ItemColorResolver + ItemIconResolver
6. `:core:database` — move all entities, DAOs, TaskDatabase (excluding ProductDatabase); **run all 14 instrumented DAO tests after this step**
7. `:core:auth` — move auth files; apply BuildConfig fix (change #1)
8. `:core:notifications` — extract channel ID constants and channel registration from MainActivity/workers; no feature-specific content

### Phase 3 — Data modules
9. `:data:sync` — move sync orchestration and Google API sync classes
10. `:data:firestore` — move firestore files

### Phase 4 — Feature modules (simplest first)
11. `:feature:conversation` — zero DB, zero auth; pure move
12. `:feature:note` — move NoteRepository + UI; trivial
13. `:feature:health` — move health files; update AndroidManifest for HealthPermissionsRationaleActivity
14. `:feature:mood` — apply fixes #2 and #5 first, then move; move `notification_mood_picker.xml` resource
15. `:feature:sequence` — apply fixes #3 and #4, then move
16. `:feature:task` — last; most dependencies; move repositories, workers, all task UI

---

## Critical Files

- `app/src/main/java/org/meow/autistic/data/task/TaskDatabase.kt` — moves to `:core:database`
- `app/src/main/java/org/meow/autistic/data/mood/MoodBroadcastReceiver.kt` — fix #2 before moving
- `app/src/main/java/org/meow/autistic/data/sequence/SequenceStepReceiver.kt` — fix #3 before moving
- `app/src/main/java/org/meow/autistic/data/sequence/SequenceRunNotificationManager.kt` — fix #4 before moving
- `app/src/main/java/org/meow/autistic/MainActivity.kt` — extract `showNotification()` (#5)
- `app/src/main/java/org/meow/autistic/data/auth/GoogleAuthManager.kt` — fix #1 before moving
- `app/build.gradle.kts` — source of BuildConfig fields that move to constructor injection
- `settings.gradle.kts` — add all 14 new `include()` paths
- `gradle/libs.versions.toml` — add `android-library` plugin alias for use in build-logic

---

## Verification

After each Phase 2–4 step:
1. `./gradlew :app:compileDebugKotlin` — must be error-free
2. `./gradlew test` — all unit tests pass
3. After Phase 2 step 6 (`:core:database`): `./gradlew connectedAndroidTest` — all 14 DAO tests pass
4. After Phase 4 completes: `./gradlew connectedAndroidTest` — full instrumented suite

Final check: `./gradlew :app:assembleDebug` with a clean build cache to confirm no implicit dependency leakage between modules.

---

## Scope Note

This is a large structural refactoring: ~15 new `build.gradle.kts` files, updates to `settings.gradle.kts`, and moving Kotlin files across module directories. The Kotlin code itself changes minimally. Execute one phase at a time with a passing test suite before proceeding.
