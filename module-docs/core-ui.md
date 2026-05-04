# :core:ui

## Purpose
Shared Compose design system: theme tokens, reusable UI components, and item presentation helpers used across all feature screens.

## Functional Scope
- App-wide Material 3 theme (colors, typography, shapes)
- Reusable Compose components shared by two or more features
- Color and icon resolution for task/sequence items

## Key Files
- Theme files — `Color.kt`, `Theme.kt`, `Type.kt`
- `CommonComponents` — shared composables (loading indicators, dialogs, etc.)
- `ItemColorResolver` — maps item state/category to display color
- `ItemIconResolver` — maps item type to icon resource

## Dependencies
- Compose BOM + Material 3 only

## Not In This Module
- Screen-level composables (each lives in its feature module)
- Navigation logic (`:app`)
- Any data or business logic
