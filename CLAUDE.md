# Autistic — Project Rules

## Testing

- At the end of each prompt run tests in this order:
  1. For every change set, add or update the relevant unit tests (`src/test/`) and instrumented tests (`src/androidTest/`).
  2. ./gradlew test`
  3. ./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=<TestClass>` for each changed file's test class
  4. ./gradlew connectedAndroidTest` for the remaining test classes
  5. If any test needed a fix, re-run only the test classes related to that fix. If all pass, run `./gradlew test connectedAndroidTest` and repeat step 4 if there are further failures.
- All tests must pass before the response is considered complete. Fix any failures before finishing.
