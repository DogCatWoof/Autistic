# :feature:sequence

## Purpose
Sequences (step-by-step routines): define multi-step sequences, run them with timed step notifications, and review run history.

## Functional Scope
- Create, edit, and delete sequences and their steps
- Start and progress through a sequence run with per-step notifications
- Step completion via notification action (BroadcastReceiver)
- View sequence list and run history

## Key Files
- `SequenceRepository` — CRUD for `SequenceEntity`, `SequenceStepEntity`, `SequenceRunEntity`
- `SequenceViewModel` — sequence list and edit state
- `SequenceRunViewModel` — active run state and step progression
- `SequenceListScreen` — sequence list and detail UI
- `SequenceRunNotificationManager` — posts and updates the active-run notification (receives injected `SequenceDao`)
- `SequenceStepReceiver` — BroadcastReceiver for step-complete notification actions; injects `SequenceDao` via `KoinComponent`

## Dependencies
- `:core:database`
- `:core:common`
- `:core:ui`
- `:core:notifications`

## Not In This Module
- Task reminders (`:feature:task`)
- Firestore sync (`:data:firestore`)
