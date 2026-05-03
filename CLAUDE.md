# Autistic — Project Rules

## File Editing

- You are the sole editor of all files in this project. Skip conflict checks and re-reads of files already read this session.
- If a file was changed externally, the user will say **"X was updated externally"** — re-read it before proceeding.

## Response Style

- Make code edits silently — do not narrate individual changes inline.
- At the end of each response, list only the files modified on a single line (e.g. `Updated: Foo.kt, Bar.kt`).
- Never show code blocks, diffs, or add/delete counts unless explicitly asked.

## Testing

- At the end of each prompt run tests in this order:
  1. For every change set, add or update the relevant unit tests (`src/test/`) and instrumented tests (`src/androidTest/`).
  2. Run the tests assuming there could be a fail when running the test.  So it doesn't require an extra run to gather failure information
  3. ./gradlew test`
  4. /gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=<TestClass>` for each changed file's test class
  5./gradlew connectedAndroidTest` for the remaining test classes
  6. If any test needed a fix, re-run only the test classes related to that fix. If all pass, run `./gradlew test connectedAndroidTest` and repeat step 4 if there are further failures.
- All tests must pass before the response is considered complete. Fix any failures before finishing.
 
