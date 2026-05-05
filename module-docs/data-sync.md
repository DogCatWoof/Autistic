# :data:sync

## Purpose
Bidirectional sync with Google Tasks and Google Calendar. Orchestrates all sync steps — including Firestore — as a single pipeline invoked by WorkManager.

## Functional Scope
- Push local task changes to Google Tasks API; pull remote changes back into Room
- Pull Google Calendar events into Room
- Orchestrate the full sync pipeline (auth check → Google Tasks push → Google Tasks pull → Calendar pull → Firestore push → Firestore pull)
- Schedule periodic background sync via WorkManager

## Key Files
- `SyncOrchestrator` — coordinates all sync steps in order; Firestore steps skip if not authenticated
- `SyncWorker` — WorkManager worker that calls `SyncOrchestrator`
- `SyncScheduler` — enqueues/cancels the periodic WorkManager job
- `GoogleTasksRemoteSource` — Google Tasks REST API wrapper
- `GoogleTasksSyncService` — push/pull logic for tasks
- `CalendarRemoteSource` — Google Calendar API wrapper
- `CalendarSyncService` — pull logic for calendar events

## Dependencies
- `:core:database`
- `:core:auth`
- `:data:firestore`

## Not In This Module
- Firestore sync logic (`:data:firestore`)
- Google Drive backup (removed from plan — using Firestore sync instead)
- Any UI or ViewModels
