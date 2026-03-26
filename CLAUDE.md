# Autistic — Project Rules

## Testing

- For every change set, add or update the relevant unit tests (`src/test/`) and instrumented tests (`src/androidTest/`).
- At the end of each prompt, run both test suites:
  1. `./gradlew test` (unit tests)
  2. `./gradlew connectedAndroidTest` (instrumented tests on connected device)
- All tests must pass before the response is considered complete. Fix any failures before finishing.
