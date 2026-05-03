# Firebase + Firestore Integration Plan

## Context
The app currently stores all data locally in Room and syncs Tasks/Calendar with Google APIs and Drive for backup. The goal is to add Firestore as a cross-device sync layer (enabling a companion website and future services), add Firebase Auth alongside the existing Google OAuth flow, and make all user-created data available across devices — while keeping the existing Google Tasks, Calendar, and Drive integrations intact.

---

## Prerequisites — Needs Explicit Approval (build.gradle changes)

- [x] Create a Firebase project at console.firebase.google.com, add the Android app, download `google-services.json` → place in `app/`.
- [x] **`build.gradle.kts` (root):** add `google-services` plugin classpath.
- [x] **`app/build.gradle.kts`:** add Firebase BOM + dependencies: `firebase-bom`, `firebase-auth-ktx`, `firebase-firestore-ktx`.
- [x] Apply `google-services` plugin in `app/build.gradle.kts`.

---

## Phase 1 — Firebase Auth Integration

**Goal:** Sign users into Firebase Auth using their existing Google account, so Firestore Security Rules can use `request.auth.uid`. No changes to Google OAuth token flow (Tasks/Calendar/Drive still use `GoogleAuthManager.getValidToken()`).

- [x] `data/auth/GoogleAuthManager.kt`
  - After successful Google sign-in (`handleSignInResult`), call `FirebaseAuth.getInstance().signInWithCredential(GoogleAuthProvider.getCredential(idToken, null))`.
  - `isAuthenticated()`: also check `FirebaseAuth.getInstance().currentUser != null`.
  - `signOut()`: also call `FirebaseAuth.getInstance().signOut()`.
  - Add `getFirebaseUid(): String` — returns `currentUser!!.uid`, throws if not signed in.
- [x] Verify: sign in → `FirebaseAuth.getInstance().currentUser` non-null; sign out → Firebase user cleared.

---

## Phase 2 — Room Entity Sync Fields + Migration

**Goal:** Add Firestore sync metadata to all entities. Room DB version: **17 → 18**.

### Common fields to add to each entity (where not already present):
| Field | Type | Default | Purpose |
|---|---|---|---|
| `firestoreId` | `String?` | `null` | Firestore document ID; null = not yet pushed |
| `lastModifiedAt` | `Instant` | `Instant.now()` | Conflict resolution (last-write-wins) |
| `pendingFirestoreSync` | `Boolean` | `true` | True = needs push to Firestore |
| `isDeleted` | `Boolean` | `false` | Soft delete for cross-device propagation |

### Entities to update:
| Entity | File | Notes |
|---|---|---|
| `TaskEntity` | `data/task/TaskEntity.kt` | Add `firestoreId`, `lastModifiedAt`, `pendingFirestoreSync`. Already has soft-delete via `syncStatus`. |
| `NoteEntity` | `data/note/NoteEntity.kt` | Add `firestoreId`, `pendingFirestoreSync`. Already has `updatedAt` and `isDeleted`. |
| `MoodEntity` | `data/mood/MoodEntity.kt` | Add all 4 fields. |
| `FoodLogItemEntry` | `data/foodlog/FoodLogItemEntry.kt` | Add all 4 fields. Do NOT add to `FoodLogEntry` (derived aggregate). |
| `HealthSnapshotEntity` | `data/health/HealthSnapshotEntity.kt` | Add `firestoreId`, `pendingFirestoreSync`. Already has `lastUpdatedAt`. No `isDeleted` needed (date-keyed). |
| `SequenceEntity` | `data/sequence/SequenceEntity.kt` | Add all 4 fields. |
| `SequenceStepEntity` | `data/sequence/SequenceEntity.kt` | Add all 4 fields. |
| `SequenceRunEntity` | `data/sequence/SequenceEntity.kt` | Add `firestoreId`, `lastModifiedAt`, `pendingFirestoreSync`. |
| `DailyTaskEntity` | `data/task/DailyTaskEntity.kt` | Add `firestoreId`, `lastModifiedAt`, `pendingFirestoreSync`. |

### DAOs to update (add to each affected DAO):
- [x] `getPendingFirestoreSync()` — query where `pendingFirestoreSync = 1 AND isDeleted = 0`
- [x] `getPendingFirestoreDelete()` — query where `pendingFirestoreSync = 1 AND isDeleted = 1`
- [x] `markFirestoreSynced(id, firestoreId)` — clears `pendingFirestoreSync`, sets `firestoreId`

### Migration:
- [x] `data/task/TaskDatabase.kt` — bump version 17 → 18, add `Migration(17, 18)` with `ALTER TABLE` for each affected table. Do NOT use destructive migration.

---

## Phase 3 — Firestore Source + Document Models

**Goal:** Create the Firestore access layer, mirroring the `GoogleTasksRemoteSource` pattern.

### Firestore collection structure:
```
users/{uid}/tasks/{firestoreId}
users/{uid}/notes/{firestoreId}
users/{uid}/moods/{firestoreId}
users/{uid}/foodLogItems/{firestoreId}
users/{uid}/healthSnapshots/{date}       ← date string as document ID
users/{uid}/sequences/{firestoreId}
users/{uid}/sequenceSteps/{firestoreId}
users/{uid}/sequenceRuns/{firestoreId}
users/{uid}/dailyTasks/{firestoreId}
```

- [ ] `data/firestore/FirestoreSource.kt`
  - Root user ref: `firestore.collection("users").document(uid)`
  - Methods: `upsert(collection, docId, data)`, `delete(collection, docId)`, `fetchAll(collection)`, `fetchSince(collection, since: Instant)`
  - All operations on `Dispatchers.IO`

- [ ] `data/firestore/FirestoreDocuments.kt`
  - One `data class` per entity (e.g. `TaskDocument`, `NoteDocument`, …)
  - Conversion functions: `TaskEntity.toDocument()`, `TaskDocument.toEntity()`
  - Fields as Firestore-native types; `lastModifiedAt` as Firestore `Timestamp`

---

## Phase 4a — FirestoreSyncService: Push (Room → Firestore)

**Direction: Room is the source of truth. All writes go to Room first; Firestore is the sync target.**

- [ ] `data/firestore/FirestoreSyncService.kt` — `pushPending(uid: String)`:
  - For each entity type: load `getPendingFirestoreSync()`, upsert each to Firestore, call `markFirestoreSynced()` on success.
  - Load `getPendingFirestoreDelete()`, delete each from Firestore, then hard-delete from Room.
- [ ] `data/firestore/FirestoreModule.kt` — Koin bindings for `FirestoreSource`, `FirestoreSyncService`.
- [ ] Verify: create a task → trigger sync → document appears in Firebase Console under `users/{uid}/tasks/`.

---

## Phase 4b — FirestoreSyncService: Pull (Firestore → Room)

- [ ] `data/firestore/FirestoreSyncService.kt` — `pullAndMerge(uid: String, since: Instant?)`:
  - For each collection: `fetchSince(collection, since)`.
  - For each remote doc: upsert into Room only if remote `lastModifiedAt` > local (local wins offline edits).
- [ ] `data/firestore/FirestoreSyncPrefs.kt` — DataStore key `lastFirestorePullAt: Instant?` for incremental pulls.
- [ ] **Conflict resolution:** local wins if newer; remote wins if newer; ties → local wins.
- [ ] Verify: write a doc in Firestore Console → trigger sync → appears in Room.

---

## Phase 5 — Repository Write-Path Updates

**Goal:** Every local write sets `lastModifiedAt = Instant.now()` and `pendingFirestoreSync = true` automatically.

- [ ] `TaskRepository` — `insertTask()`, `updateTask()`, `deleteTask()`
- [ ] `NoteRepository` — `insertNote()`, `updateNote()`, `deleteNote()`
- [ ] `MoodRepository` — all writes
- [ ] `FoodLogRepository` — `FoodLogItemEntry` writes only (not `FoodLogEntry` aggregate)
- [ ] `HealthSnapshotRepository` — all writes
- [ ] `SequenceRepository` — all entity writes
- [ ] `DailyTaskRepository` — all writes

Note: `TaskEntity.syncStatus` (Google Tasks sync) is unchanged.

---

## Phase 6 — SyncOrchestrator + WorkManager

**Goal:** Wire Firestore push/pull into the existing sync pipeline as steps 5–6.

- [ ] `data/sync/SyncOrchestrator.kt` — extend pipeline:
  ```
  1. Verify auth (existing)
  2. Push pending → Google Tasks (existing)
  3. Pull from Google Tasks (existing)
  4. Pull from Google Calendar (existing)
  5. Push pending → Firestore          ← new
  6. Pull from Firestore (incremental) ← new
  ```
  - Steps 5/6 skip if `FirebaseAuth.currentUser == null`.
  - Firestore errors do NOT abort steps 1–4 (decouple failures).
- [ ] No new WorkManager worker needed — `SyncScheduler` already triggers `SyncOrchestrator`.

---

## Phase 7 — Firestore Security Rules

Apply in Firebase Console (no code change):

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId}/{document=**} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

- [ ] Rules applied in Firebase Console.

---

## New Files Summary

| File | Purpose |
|---|---|
| `data/firestore/FirestoreSource.kt` | Firestore SDK wrapper |
| `data/firestore/FirestoreDocuments.kt` | Entity ↔ Firestore document models |
| `data/firestore/FirestoreSyncService.kt` | Push + pull sync logic |
| `data/firestore/FirestoreModule.kt` | Koin DI bindings |
| `data/firestore/FirestoreSyncPrefs.kt` | DataStore: last pull timestamp |
