# :feature:health

## Purpose
Health Connect integration: reads health metrics (steps, heart rate, sleep, etc.) from Health Connect, stores snapshots, and displays an integrated health dashboard.

## Functional Scope
- Request and manage Health Connect permissions
- Sync health data from Health Connect into Room health snapshots
- Display health metrics and trends
- Settings screen for Health Connect configuration

## Key Files
- `HealthConnectRepository` — reads from Health Connect SDK, writes `HealthSnapshotEntity`
- `HealthConnectSyncWorker` — periodic WorkManager job for health data sync
- `HealthConnectViewModel` — health data state for UI
- `HealthConnectSettingsScreen` — permission management and sync config UI
- `HealthConnectDetailDialog` — detail view for a single health metric
- `IntegratedHealthScreen` — main health dashboard composable

## Dependencies
- `:core:database`
- `:core:ui`
- Health Connect SDK

## Not In This Module
- Mood data (`:feature:mood`)
- Firestore sync (`:data:firestore`)
