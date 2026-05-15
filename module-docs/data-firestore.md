# :data:firestore

## Purpose
Bidirectional Room ↔ Firestore sync for all user data collections. Enables cross-device data availability and powers a future companion website.

## Functional Scope
- Push pending local changes (new, updated, soft-deleted records) to Firestore
- Pull Firestore changes since last sync and merge into Room using last-write-wins
- Persist the last-pull timestamp for incremental syncs
- Document model conversions (entity ↔ Firestore map)

## Key Files
- `FirestoreSource` — Firestore SDK wrapper; `upsert`, `delete`, `fetchAll`, `fetchSince`
- `FirestoreSyncService` — `pushPending(uid)` and `pullAndMerge(uid, since)` for all 9 collections
- `FirestoreDocuments.kt` — document models and entity ↔ document conversions for tasks, notes, moods
- `FirestoreHealthDocuments.kt` — health snapshot document model
- `FirestoreSequenceDocuments.kt` — sequence, step, run document models
- `FirestoreSyncPrefs` — DataStore: persists `lastFirestorePullAt`

## Collection Structure
```
users/{uid}/tasks, notes, moods,
            healthSnapshots, sequences, sequenceSteps,
            sequenceRuns, dailyTasks
```

## Dependencies
- `:core:database`
- `:core:auth`
- Firebase Firestore SDK

## Not In This Module
- Google Tasks / Calendar sync (`:data:sync`)
- Any UI or ViewModels
