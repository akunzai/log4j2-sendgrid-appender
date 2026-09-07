# log4j2-sendgrid-appender Developer Guidelines

This is a Log4j 2 appender plugin that sends logging events (errors) via the SendGrid service.

## Commands
- Build & verify (compile, SpotBugs, test, JaCoCo): `./gradlew check`
- Run a single test class: `./gradlew test --tests "com.github.akunzai.log4j.SendGridAppenderTest"`
- Run a single test method: `./gradlew test --tests "com.github.akunzai.log4j.SendGridAppenderTest.testDelivery"`
- Run test runner: `./gradlew runTestRunner`

## Pointers
- Core Appender plugin: @src/main/java/com/github/akunzai/log4j/SendGridAppender.java
- Gold-standard test: @src/test/java/com/github/akunzai/log4j/SendGridAppenderTest.java

## Claude Code Compatibility

`CLAUDE.md` is a symbolic link pointing to `AGENTS.md`. Edit `AGENTS.md` directly.

## Prevent Recurrence
- **Candidate**: Name who hits this again, in which file, on what change. No such scenario, nothing to propose.
- **Promote**: Offer the first tier that reaches them and only that one, pending confirmation — enforce it (assert/type/test) with its size quoted, else a comment at that site, else an agent-facing doc (`docs/agents/<topic>.md`, else `docs/agents/lessons-learned.md`) with one `@path` line under Pointers and one sentence on why the tiers above cannot hold it.
- **Prune**: When adding to a file, audit the rest of it in the same pass. Drop entries once stale (obsolete version, now enforced, duplicated, or a transcript) — not by a fixed count.
