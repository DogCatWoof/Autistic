# :core:notifications

## Purpose
Centralised notification infrastructure: channel definitions, channel registration, and permission helpers. Feature modules send notifications but delegate channel setup here.

## Functional Scope
- Declares all notification channel IDs as constants
- Registers all channels with the system on app start
- Provides notification permission check/request helpers

## Key Files
- `NotificationChannels` — channel ID constants and `registerAll(context)` entry point
- Permission helpers for `POST_NOTIFICATIONS`

## Dependencies
- `:core:common`
- Android SDK only (no Compose, no Room)

## Not In This Module
- Notification content, payloads, or layouts (each lives in the feature that sends the notification)
- WorkManager workers (those live in feature modules)
- BroadcastReceivers (those live in feature modules)
