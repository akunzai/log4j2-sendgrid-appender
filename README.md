# log4j2-sendgrid-appender

[![Build Status][ci-badge]][ci] [![Code Coverage][codecov-badge]][codecov] [![Download][maven-badge]][maven]

[ci]: https://github.com/akunzai/log4j2-sendgrid-appender/actions?query=workflow%3ACI
[ci-badge]: https://github.com/akunzai/log4j2-sendgrid-appender/workflows/CI/badge.svg
[codecov]: https://codecov.io/gh/akunzai/log4j2-sendgrid-appender
[codecov-badge]: https://codecov.io/gh/akunzai/log4j2-sendgrid-appender/branch/main/graph/badge.svg?token=RDIFA6DTUZ
[maven]: https://search.maven.org/artifact/com.github.akunzai/log4j2-sendgrid-appender
[maven-badge]: https://img.shields.io/maven-central/v/com.github.akunzai/log4j2-sendgrid-appender.svg

Send [log4j2](https://logging.apache.org/log4j/2.x/) errors via [SendGrid](https://sendgrid.com) service

## Requirements

- Java 8 runtime environments
- a SendGrid account with your [API key](https://app.sendgrid.com/settings/api_keys)

## Installation

### Gradle

```groovy
dependencies {
  implementation 'com.github.akunzai:log4j2-sendgrid-appender:2.0.1'
}
```

### Maven

```xml
<dependency>
  <groupId>com.github.akunzai</groupId>
  <artifactId>log4j2-sendgrid-appender</artifactId>
  <version>2.0.1</version>
</dependency>
```

## Usage

### General Usage

The following is the minimum configuration needed for `log4j2.xml` to send an error email

> By default, logger events will be buffering with previous 512 messages and filterd by [ThresholdFilter](https://logging.apache.org/log4j/2.x/manual/filters.html#ThresholdFilter), formatted as [HTML](https://logging.apache.org/log4j/2.x/manual/layouts.html#HTMLLayout).

```xml
<Configuration packages="com.github.akunzai.log4j">
    <Appenders>
        <SendGrid name="SendGrid"
            subject="Error Notification from ${sys:hostName}"
            from="${env:LOG_MAIL_FROM}"
            to="${env:LOG_MAIL_TO}"
            apiKey="${env:SENDGRID_API_KEY}">
        </SendGrid>
    </Appenders>
    <Loggers>
        <Root Level="WARN">
            <AppenderRef ref="SendGrid"/>
        </Root>
    </Loggers>
</Configuration>
```

### Sample Usage

Sending error message without buffering, use [PatternLayout](https://logging.apache.org/log4j/2.x/manual/layouts.html#PatternLayout) instead of HtmlLayout, 
and use [CompositeFilter](https://logging.apache.org/log4j/2.x/manual/filters.html#CompositeFilter)(BurstFilter,ThresholdFilter) instead of ThresholdFilter.

```xml
<Configuration packages="com.github.akunzai.log4j">
    <Appenders>
        <SendGrid name="SendGrid"
            subject="Error Notification from ${sys:hostName}"
            from="${sys:LOG_MAIL_FROM:-${env:LOG_MAIL_FROM}}"
            to="${sys:LOG_MAIL_TO:-${env:LOG_MAIL_TO}}"
            apiKey="${env:SENDGRID_API_KEY}"
            bufferSize="0">
            <PatternLayout pattern="%msg%nDate: %date%nLevel: %level%nLogger: %logger%nException: %rException%n" />
            <Filters>
                <BurstFilter level="WARN" rate="0.033" maxBurst="10" />
                <ThresholdFilter level="ERROR" />
            </Filters>
        </SendGrid>
    </Appenders>
    <Loggers>
        <Root Level="WARN">
            <AppenderRef ref="SendGrid"/>
        </Root>
    </Loggers>
</Configuration>
```
