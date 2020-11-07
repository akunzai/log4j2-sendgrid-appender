# log4j2-sendgrid-appender

Send [log4j2](https://logging.apache.org/log4j/2.x/) errors via [SendGrid](https://sendgrid.com) service

[![Build Status](https://travis-ci.com/akunzai/log4j2-sendgrid-appender.svg?branch=master)](https://travis-ci.com/akunzai/log4j2-sendgrid-appender)
[![Download](https://api.bintray.com/packages/akunzai/maven/log4j2-sendgrid-appender/images/download.svg)](https://bintray.com/akunzai/maven/log4j2-sendgrid-appender/_latestVersion)

## Requirements

- Java 8 runtime environments
- a SendGrid account with your [API key](https://app.sendgrid.com/settings/api_keys)

## Installation

Currently, this package only host on [jcenter](https://bintray.com/bintray/jcenter).

### Gradle

```groovy
dependencies {
  implementation 'com.github.akunzai:log4j2-sendgrid-appender:2.0.0'
}
```

### Maven

```xml
<dependency>
  <groupId>com.github.akunzai</groupId>
  <artifactId>log4j2-sendgrid-appender</artifactId>
  <version>2.0.0</version>
</dependency>
```

## Usage

### General Usage

The following is the minimum needed configuration for `log4j2.xml` to send an error email

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
