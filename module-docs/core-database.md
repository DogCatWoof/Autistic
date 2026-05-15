# :core:database

## Purpose
All Room persistence: entities, DAOs, the single `TaskDatabase` instance, and schema migrations. Every feature module reads and writes data through this module.

## Functional Scope
- Defines the schema for all user data: tasks, daily tasks, notes, moods, health snapshots, sequences, sequence steps, sequence runs
- Provides DAOs for each entity type
- Manages Room database versioning and migrations

## Key Files
- `TaskDatabase` — single `@Database` class, holds all 10 entities; version-managed with explicit migrations
- `TaskEntity` / `TaskDao` — tasks and Google Tasks sync state
- `DailyTaskEntity` / `DailyTaskDao`
- `NoteEntity` / `NoteDao`
- `MoodEntity` / `MoodDao`
- `HealthSnapshotEntity` / `HealthSnapshotDao`
- `SequenceEntity`, `SequenceStepEntity`, `SequenceRunEntity` / `SequenceDao`
- `MIGRATION_*` objects — all schema migrations

## Dependencies
- Room runtime + KSP only

## Not In This Module
- Any repository, ViewModel, or UI
- Business logic of any kind
