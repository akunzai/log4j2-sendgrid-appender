# log4j2-sendgrid-appender

Send log4j2 errors via SendGrid service

## Requirements

- Java 8 runtime environments
- an SendGrid account with your [API key](https://app.sendgrid.com/settings/api_keys)

## Installation

Gradle

```groovy
repositories {
  jcenter()
}
dependencies {
    implementation 'com.github.akunzai:log4j2-sendgrid-appender:1.0-SNAPSHOT'
}
```

## Usage

### General Usage

The following is the minimum needed configuration for `log4j2.xml` to send an error email

> By default, logger events will be buffered (defaults: 512 messages) and triggered by ERROR level message,
> formatted as HTMLLayout.

```xml
<Configuration monitorInterval="5" status="WARN"
    packages="com.github.akunzai.log4j">
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

### Pattern Layout Usage

Sending error message without buffers, and use CompositeFilter(BurstFilter,ThresholdFilter) instead of ThresholdFilter.

```xml
<Configuration monitorInterval="5" status="WARN"
    packages="com.github.akunzai.log4j">
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