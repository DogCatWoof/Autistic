# :app

## Purpose
Application shell: entry point, navigation, global settings, and Koin dependency injection wiring. Orchestrates feature modules but contains no domain logic.

## Functional Scope
- App entry point and Application class (Koin initialisation)
- Bottom sheet and drawer navigation between feature screens
- Navigation state and preferences persistence
- Global settings screen (sync settings, app preferences)
- Koin DI module definitions for all layers
- Sync settings UI
- Query log screen (diagnostic)

## Key Files
- `AutisticApp` — `Application` subclass; starts Koin with all modules
- `MainActivity` — hosts the Compose NavHost and navigation scaffolding
- `AppDrawer` — navigation drawer composable
- `NavBottomSheet` — bottom sheet nav composable
- `NavPreferencesStore` — DataStore: persists nav UI preferences
- `NavStateStore` — DataStore: persists navigation state across restarts
- `NavPreferencesScreen` — nav settings UI
- `SettingsScreen` — top-level settings screen
- `SyncSettingsScreen` — sync configuration UI
- `QueryLogScreen` — diagnostic log of recent DB/sync queries
- `di/DatabaseModule.kt`, `di/AuthModule.kt`, `di/RepositoryModule.kt`, etc. — all Koin module definitions

## Dependencies
- All `:feature:*`, all `:data:*`, all `:core:*`

## Not In This Module
- Any domain logic (lives in feature or data modules)
- Database schema or entities (`:core:database`)
