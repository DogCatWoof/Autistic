---
description: Run all unit and integration tests, then fix any failures
subtask: true
---
Run the full test suite and fix any failures.

First run unit tests:
`./gradlew test`

Then run instrumented tests:
`./gradlew connectedAndroidTest`

If any test fails, analyze the failure, fix the code, and re-run only the failing test classes. Repeat until all tests pass. When all tests pass, run `./gradlew test connectedAndroidTest` one final time to confirm.

All instrumented tests run against a clean database (cleared in `@Before`). Unit tests use Mockk — no live network.
