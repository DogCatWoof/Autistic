# Autistic — Project Rules

## Testing

- For every change set, add or update the relevant unit tests (`src/test/`) and instrumented tests (`src/androidTest/`).
- At the end of each prompt run tests in this order:
  1. `./gradlew test` (all unit tests)
  2. `./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=<ChangedClass>Test` for each changed file
  3. `./gradlew connectedAndroidTest` (full instrumented suite)
- When a test fails and is fixed:
  4. Re-run only the fixed test class first to confirm it passes, then run the full suite (`./gradlew test connectedAndroidTest`). Repeat this step if new failures appear.
- All tests must pass before the response is considered complete. Fix any failures before finishing.
