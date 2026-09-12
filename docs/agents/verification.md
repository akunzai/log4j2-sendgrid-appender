

# Verification

How an agent exercises a change in this repo before it reaches review.
Human setup narrative lives in `README.md`; this file
holds only what an agent needs.

## Starting the environment

```sh
./gradlew check
```

<!-- drift:forge github -->
<!-- drift:entrypoint-cmd ./gradlew check -->

It never prompts. A step needing a human aborts non-zero naming the
prerequisite — see Human prerequisites below.

**Proof it ran**: Gradle task `:check` completes with `BUILD SUCCESSFUL` (exit code 0).

## Checks

Record what the lookup cannot give. A task runner already names and
describes its own tasks, so copy none of them here; say where they live
and which command lists them.

List tasks with `./gradlew tasks`.

| What | Command |
| --- | --- |
| Full verification gate | `./gradlew check` |
| Single test class | `./gradlew test --tests "com.github.akunzai.log4j.SendGridAppenderTest"` |
| Single test method | `./gradlew test --tests "com.github.akunzai.log4j.SendGridAppenderTest.testDelivery"` |
| Async manual test runner | `./gradlew runTestRunner` |

## Human prerequisites

None. mise provisions JDK 25 automatically from `mise.toml`, or the Gradle
Foojay toolchain resolver downloads the required JDK on demand.

## Ports

Not applicable. `log4j2-sendgrid-appender` is a library with no listening service,
so several agents can run the gate in the same clone at once.

## Changes that need a deployed environment

None. All appender logic and SendGrid API interactions are verified locally
using unit and integration tests with `MockSendGrid`.

Agent may deploy to it: **no**.

## Capturing evidence

- Test logs and reports: build console output, `build/reports/tests/test/index.html`, or JaCoCo coverage reports in `build/reports/jacoco/test/html/index.html`.

**This document is where the capture rules live**, and the request
document points here rather than restating them. A capture taken on the
developer's own machine carries their account's data, username, and home
paths as readily as a shared environment does. Assert on the frame, a
marker, or fixture data, and crop or mask what the tool happened to be
showing.

For a change behind a mode switch or feature flag, confirm the far end
received the call. A healthy container and a green build are not
evidence that an integration is wired up.

## Not verified

None. All library features are covered by automated unit and integration tests.

A gap you could have closed is not a gap. Run the check whose dependency
you have already seen running, and report a check you skipped as untried,
rather than recording it here as one this repo cannot run.
