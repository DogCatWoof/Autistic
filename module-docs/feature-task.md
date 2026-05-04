# :feature:task

## Purpose
Everything related to tasks and daily tasks: creation, editing, completion, deletion, reminders, daily reset, and calendar event display.

## Functional Scope
- Create, edit, complete, and delete tasks
- Google Tasks sync integration (via `:data:sync`)
- Daily tasks: configure recurring daily task lists, track completion per day
- Task reminder notifications at scheduled times
- Daily reset worker: resets daily task completion at midnight
- Calendar event browsing alongside tasks

## Key Files
- `TaskRepository` — CRUD and sync-state management for tasks
- `DailyTaskRepository` — CRUD for daily task definitions and completions
- `CalendarRepository` — reads calendar events from Room (written by `:data:sync`)
- `TaskViewModel` — task list state, filter, completion actions
- `DailyTasksViewModel` — daily task list and settings state
- `TaskListScreen` — main task list UI
- `TaskListItem` — individual task row composable
- `TaskListDialogs` — add/edit/delete/reminder dialog composables
- `DailyTasksSettingsScreen` — daily task configuration UI
- `DailyTaskDialog` — add/edit daily task dialog
- `TaskReminderWorker` — fires notification at task due time
- `DailyResetWorker` — resets daily task completions at midnight

## Dependencies
- `:core:database`, `:core:auth`, `:core:common`, `:core:ui`, `:core:notifications`
- `:data:sync`

## Not In This Module
- Firestore sync logic (`:data:firestore`, invoked by `:data:sync` orchestrator)
- Google Drive backup (removed; Firestore sync is the cross-device strategy)
- Note, mood, food, or sequence features
