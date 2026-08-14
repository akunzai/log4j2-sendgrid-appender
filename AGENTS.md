# log4j2-sendgrid-appender Developer Guidelines

This is a Log4j 2 appender plugin that sends logging events (errors) via the SendGrid service.

## Commands
- Build & verify (compile, SpotBugs, test, JaCoCo): `./gradlew check`
- Run a single test class: `./gradlew test --tests "com.github.akunzai.log4j.SendGridAppenderTest"`
- Run a single test method: `./gradlew test --tests "com.github.akunzai.log4j.SendGridAppenderTest.testDelivery"`
- Run test runner: `./gradlew runTestRunner`

## Pointers
- Core Appender plugin: @src/main/java/com/github/akunzai/log4j/SendGridAppender.java
- SendGrid Manager: @src/main/java/com/github/akunzai/log4j/SendGridManager.java
- Message Builder: @src/main/java/com/github/akunzai/log4j/SendGridMessageBuilder.java
- Gold-standard test: @src/test/java/com/github/akunzai/log4j/SendGridAppenderTest.java
- Build configuration: @build.gradle.kts

## Claude Code Compatibility

`CLAUDE.md` is a symbolic link pointing to `AGENTS.md`. Edit `AGENTS.md` directly.

## Self-Reflection
- **Candidate**: Distill a non-obvious gotcha into ≤ 2 context-tagged bullets. Propose it before writing.
- **Promote**: On confirmation, write it to a dedicated file — merge an existing topic doc, else `docs/<topic>.md`, else `docs/lessons-learned.md`. Add or update one `@path` line under Pointers.
- **Prune**: Drop entries once stale (obsolete version, now enforced, duplicated, or a transcript) — not by a fixed count.
