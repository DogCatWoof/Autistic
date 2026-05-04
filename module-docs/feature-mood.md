# :feature:mood

## Purpose
Mood tracking: scheduled check-in prompts, mood logging, and mood history display.

## Functional Scope
- Log mood ratings with optional notes
- Scheduled mood check-in notifications via WorkManager
- Custom notification with inline mood picker (notification_mood_picker layout)
- View mood history

## Key Files
- `MoodRepository` — CRUD for `MoodEntity`; sets `pendingFirestoreSync` on every write
- `MoodViewModel` — mood history state
- `MoodScreen` — mood history and log UI
- `MoodCheckInWorker` — WorkManager worker that fires mood check-in notification
- `MoodBroadcastReceiver` — handles inline mood selection from notification action
- `MoodNotificationHelper` — builds and posts the mood check-in notification (extracted from `MainActivity.showNotification()`)
- `res/layout/notification_mood_picker.xml` — custom notification layout

## Dependencies
- `:core:database`
- `:core:ui`
- `:core:notifications`

## Not In This Module
- Firestore sync (`:data:firestore`)
- Health Connect data (`:feature:health`)
