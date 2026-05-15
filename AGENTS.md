# Autistic — Project AGENTS.md

Android app with Room local storage, Google Tasks/Calendar/Drive sync, and Firestore cross-device sync. 15 Gradle modules, Kotlin 2.2.10, AGP 9.0.1, JVM 21.

## File Editing

- You are the sole editor. Skip conflict checks and re-reads of files already read this session.
- If a file was changed externally, the user will say **"X was updated externally"** — re-read it first.
- Make code edits silently — no inline narration, no code blocks, no diffs in responses.
- At the end of each response, list only the files modified on a single line (e.g. `Updated: Foo.kt, Bar.kt`).
- Never show code blocks, diffs, or add/delete counts unless explicitly asked.

## Developer Commands

```
./gradlew test                                          # all unit tests
./gradlew :app:testDebugUnitTest --tests "*ClassName*"  # single unit test class
./gradlew connectedAndroidTest                          # all instrumented tests
./gradlew :app:compileDebugKotlin                       # fast compile check
./gradlew run                                          # build + install on device
```

Configuration cache has known issues with `google-services` plugin — safe to ignore those warnings.

## Testing Workflow

At the end of each prompt:

1. Add/update relevant unit tests (`src/test/`) and instrumented tests (`src/androidTest/`)
2. `./gradlew test`
3. `./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=<TestClass>` for each changed test class
4. If any test fails, fix and re-run only related test classes. If all pass, run `./gradlew test connectedAndroidTest`
5. All tests must pass before the response is complete

All instrumented tests run against a clean database (cleared in `@Before`). Unit tests use Mockk — no live network.

## Multi-Module Architecture

### Module Layout (15 modules)

```
:core:common         diagnostics, debug, ExceptionReporter, GlobalErrorHandler
:core:auth           GoogleAuthManager, TokenStore
:core:database       all entities, all DAOs, TaskDatabase, migrations (13 entities, monolithic)
:core:ui             theme, CommonComponents, ItemColorResolver, ItemIconResolver
:core:notifications  notification channels, permission helpers
:data:sync           SyncOrchestrator, SyncWorker, SyncScheduler, GoogleTasks/Calendar sync
:data:firestore      FirestoreSource, FirestoreDocuments*, FirestoreSyncService, FirestoreSyncPrefs
:feature:task        TaskRepository, DailyTaskRepository, CalendarRepository, workers, VMs, UI
:feature:note        NoteRepository, NotesViewModel, NotesScreen
:feature:mood        MoodRepository, MoodCheckInWorker, MoodBroadcastReceiver, VM, Screen
:feature:health      HealthConnectRepository, HealthConnectSyncWorker, VM, Screens
:feature:sequence    SequenceRepository, SequenceRunNotificationManager, SequenceStepReceiver, VMs, Screen
:feature:conversation IntentClassifier, ResponseTemplateRepository, TonePreferencesStore, VM, Screen
:app                 AutisticApp, MainActivity, navigation, settings, all di/*.kt Koin modules
```

### Dependency Graph

```
:app → all :feature:*, all :data:*, :core:*
:feature:task → :core:database, :core:auth, :core:common, :core:ui, :core:notifications, :data:sync
:data:sync → :core:database, :core:auth, :data:firestore
:data:firestore → :core:database, :core:auth
:core:database → (Room only)
:core:auth → :core:common
:core:notifications → :core:common
:core:common → (Android SDK only)
:core:ui → (Compose BOM only)
```

No cycles. `:core:*` never imports `:feature:*` or `:data:*`.

### Build Conventions

`build-logic/` with 3 plugins:
- `autistic.android-library` — base: android-library + kotlin, compileSdk 36, minSdk 24, JVM 21
- `autistic.android-library-compose` — extends base + Compose BOM + Material3
- `autistic.android-library-room` — extends base + KSP + Room

## Sync Pipeline

Six-step sequence (abort with retry if no valid token):

1. Validate authentication token
2. Push pending → Google Tasks
3. Pull + merge remote tasks into Room
4. Pull + merge Google Calendar events (read-only)
5. Push pending → Firestore (**non-fatal**, skipped if no Firebase user)
6. Pull incremental Firestore → Room (**non-fatal**, skipped if no Firebase user)

Firestore failures are caught and logged — they never abort steps 1–4. See `SyncOrchestrator.kt`.

### Sync Scheduling

- Wi-Fi: periodic sync every 15 min (WorkManager, UNMETERED)
- Cellular: periodic sync every 1 hour (WorkManager, CONNECTED)
- Manual: `triggerImmediate()` — one-shot

## Key Constraints

- **`TaskDatabase` is monolithic** in `:core:database` — all entities and DAOs.
- **Kotlin package names never change** — only Gradle module membership changes during modularization.
- **Google Tasks notes field must never be shown in UI** — `extraPropertiesJson` is internal metadata.
- **Room is the source of truth** — all writes go to Room first, Firestore is sync target.
- **Firestore conflict resolution**: local wins if newer; remote wins if newer; ties → local wins.
- **Secrets live in `local-only/`** — `local.properties` (API keys) and `google-services.json`. Copied into `app/` at build time. Never committed.

## Module Documentation

Each module has a documentation file in `module-docs/<module-name>.md` (e.g. `module-docs/data-sync.md`). When modifying a module's contents, update its corresponding doc file to reflect the changes.

## DI Pattern

All dependencies via constructor injection — no global state or static singletons. Koin modules in `app/src/main/java/org/meow/autistic/di/`. Each feature/data/core module's services are wired in `di/SyncModule.kt`, `di/FirestoreModule.kt`, etc.

## Detailed Docs

- `requirements.md` — full functional + technical requirements
- `plan.md` — Firebase/Firestore integration plan (Phases 1-7)
- `modularization-plan.md` — 15-module migration plan with verification steps
- `web.md` — companion web interface requirements
- `module-docs/` — per-module documentation

## Modularization Verification

After each phase of module moves:
1. `./gradlew :app:compileDebugKotlin` — must be error-free
2. `./gradlew test` — all unit tests pass
3. After `:core:database` move: `./gradlew connectedAndroidTest` — all DAO tests pass
