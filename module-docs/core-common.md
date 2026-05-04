# :core:common

## Purpose
Platform-level infrastructure: crash reporting, global error handling, and debug utilities. No business logic, no UI, no database.

## Functional Scope
- Unhandled exception capture and reporting
- Global coroutine error handler
- Debug/diagnostic tooling

## Key Files
- `ExceptionReporter` — reports crashes to external services
- `GlobalErrorHandler` — CoroutineExceptionHandler wired into the app
- Debug utilities and diagnostic helpers

## Dependencies
- Android SDK only

## Not In This Module
- Any feature-domain logic
- UI components
- Database or network access
