# Autistic — Project Rules

## Testing

- For every change set, add or update the relevant unit tests (`src/test/`) and instrumented tests (`src/androidTest/`).
- At the end of each prompt, first verify the tests for changed code still match the new behaviour, then run the full suites:
  1. `./gradlew test` (unit tests)
  2. `./gradlew connectedAndroidTest` (instrumented tests on connected device)
- When a test fails and is fixed, re-run only that test class first (e.g. `./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=org.meow.autistic.FooTest`), confirm it passes, then run the full suite.
- All tests must pass before the response is considered complete. Fix any failures before finishing.
