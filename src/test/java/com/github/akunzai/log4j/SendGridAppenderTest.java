package com.github.akunzai.log4j;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sendgrid.helpers.mail.Mail;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.filter.ThresholdFilter;
import org.apache.logging.log4j.core.layout.HtmlLayout;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SendGridAppenderTest {

    @Test
    public void testDelivery() {
        var subjectKey = getClass().getName();
        var subjectValue = "SubjectValue1";
        ThreadContext.put(subjectKey, subjectValue);
        SendGridManager.FACTORY.setSendGridFactory(MockSendGrid::new);
        var appender = SendGridAppender.newBuilder()
                .setName("SendGrid")
                .setTo("to@example.com")
                .setCc("cc@example.com")
                .setBcc("bcc@example.com")
                .setFrom("from@example.com")
                .setReplyTo("replyTo@example.com")
                .setSubject("Subject Pattern %X{" + subjectKey + "}")
                .setHost("localhost")
                .setApiKey("apiKey")
                .setBufferSize(3)
                .setIgnoreExceptions(true)
                .build();
        assertNotNull(appender);
        appender.start();

        try (var context = Configurator.initialize(
                ConfigurationBuilderFactory.newConfigurationBuilder()
                        .setStatusLevel(Level.OFF)
                        .build())) {
            var logger = context.getLogger("SendGridAppenderTest");
            logger.addAppender(appender);
            logger.setAdditive(false);
            logger.setLevel(Level.DEBUG);

            logger.debug("Debug message #1");
            logger.debug("Debug message #2");
            logger.debug("Debug message #3");
            logger.debug("Debug message #4");
            logger.error("Error with exception", new RuntimeException("Exception message"));
            logger.error("Error message #2");
            var sendGrid = (MockSendGrid) appender.getManager().sendGrid;
            assertEquals(2, sendGrid.getRequests().size());
            var mapper = new ObjectMapper();
            var messages = sendGrid.getRequests().stream().map(req -> {
                try {
                    return mapper.readValue(req.getBody(), Mail.class);
                } catch (IOException e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
            }).toList().iterator();
            var message = messages.next();
            var personalization = message.getPersonalization().get(0);

            assertEquals("to@example.com", personalization.getTos().get(0).getEmail());
            assertEquals("cc@example.com", personalization.getCcs().get(0).getEmail());
            assertEquals("bcc@example.com", personalization.getBccs().get(0).getEmail());
            assertEquals("from@example.com", message.getFrom().getEmail());
            assertEquals("replyTo@example.com", message.getReplyto().getEmail());
            assertEquals("Subject Pattern " + subjectValue, message.getSubject());

            var content = message.getContent().get(0);
            assertEquals("text/html", content.getType());
            var body = content.getValue();
            assertFalse(body.contains("Debug message #1"));
            assertTrue(body.contains("Debug message #2"));
            assertTrue(body.contains("Debug message #3"));
            assertTrue(body.contains("Debug message #4"));
            assertTrue(body.contains("Error with exception"));
            assertTrue(body.contains("RuntimeException"));
            assertTrue(body.contains("Exception message"));
            assertFalse(body.contains("Error message #2"));

            var message2 = messages.next();
            var body2 = message2.getContent().get(0).getValue();
            assertFalse(body2.contains("Debug message #4"));
            assertFalse(body2.contains("Error with exception"));
            assertTrue(body2.contains("Error message #2"));
        }
    }

    @Test
    public void testDefaultLayoutAndFilter() {
        SendGridManager.FACTORY.setSendGridFactory(MockSendGrid::new);
        var appender = SendGridAppender.newBuilder()
                .setName("SendGrid")
                .setTo("to@example.com")
                .setFrom("from@example.com")
                .setApiKey("apiKey-defaults")
                .build();
        assertNotNull(appender);
        // Default layout is HtmlLayout when none is provided
        assertInstanceOf(HtmlLayout.class, appender.getLayout());
        // Default filter is ThresholdFilter when none is provided
        assertInstanceOf(ThresholdFilter.class, appender.getFilter());
    }

    @Test
    public void testPlainTextLayout() throws IOException {
        SendGridManager.FACTORY.setSendGridFactory(MockSendGrid::new);
        var appender = SendGridAppender.newBuilder()
                .setName("SendGrid")
                .setTo("to@example.com")
                .setFrom("from@example.com")
                .setApiKey("apiKey-plaintext")
                .setLayout(PatternLayout.createDefaultLayout())
                .setBufferSize(1)
                .build();
        assertNotNull(appender);
        appender.start();

        try (var context = Configurator.initialize(
                ConfigurationBuilderFactory.newConfigurationBuilder()
                        .setStatusLevel(Level.OFF)
                        .build())) {
            var logger = context.getLogger("testPlainTextLayout");
            logger.addAppender(appender);
            logger.setAdditive(false);
            logger.setLevel(Level.ERROR);

            logger.error("Plain text error");
            var sendGrid = (MockSendGrid) appender.getManager().sendGrid;
            assertEquals(1, sendGrid.getRequests().size());
            var mail = new ObjectMapper().readValue(sendGrid.getRequests().get(0).getBody(), Mail.class);
            assertEquals("text/plain", mail.getContent().get(0).getType());
        }
    }

    @Test
    public void testSandboxMode() throws IOException {
        SendGridManager.FACTORY.setSendGridFactory(MockSendGrid::new);
        var appender = SendGridAppender.newBuilder()
                .setName("SendGrid")
                .setTo("to@example.com")
                .setFrom("from@example.com")
                .setApiKey("apiKey-sandbox")
                .setSandboxMode(true)
                .setBufferSize(1)
                .build();
        assertNotNull(appender);
        appender.start();

        try (var context = Configurator.initialize(
                ConfigurationBuilderFactory.newConfigurationBuilder()
                        .setStatusLevel(Level.OFF)
                        .build())) {
            var logger = context.getLogger("testSandboxMode");
            logger.addAppender(appender);
            logger.setAdditive(false);
            logger.setLevel(Level.ERROR);

            logger.error("Sandbox error");
            var sendGrid = (MockSendGrid) appender.getManager().sendGrid;
            assertEquals(1, sendGrid.getRequests().size());
            var mail = new ObjectMapper().readValue(sendGrid.getRequests().get(0).getBody(), Mail.class);
            assertTrue(mail.getMailSettings().getSandBoxMode().getEnable());
        }
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testCreateAppenderReturnsNullWithoutName() {
        assertNull(SendGridAppender.createAppender(
                null, null, "to@example.com", null, null,
                "from@example.com", null, "Subject", null, "apiKey",
                "false", null, null, null, "true"));
        assertNull(SendGridAppender.createAppender(
                null, "", "to@example.com", null, null,
                "from@example.com", null, "Subject", null, "apiKey",
                "false", null, null, null, "true"));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testCreateAppender() throws IOException {
        SendGridManager.FACTORY.setSendGridFactory(MockSendGrid::new);
        var appender = SendGridAppender.createAppender(
                null, "SendGrid", "to@example.com", "cc@example.com", "bcc@example.com",
                "from@example.com", null, "Subject", null, "apiKey-create",
                "false", "1", null, null, "true");
        assertNotNull(appender);
        appender.start();

        try (var context = Configurator.initialize(
                ConfigurationBuilderFactory.newConfigurationBuilder()
                        .setStatusLevel(Level.OFF)
                        .build())) {
            var logger = context.getLogger("testCreateAppender");
            logger.addAppender(appender);
            logger.setAdditive(false);
            logger.setLevel(Level.ERROR);

            logger.error("Error via createAppender");
            var sendGrid = (MockSendGrid) appender.getManager().sendGrid;
            assertEquals(1, sendGrid.getRequests().size());
            var mail = new ObjectMapper().readValue(sendGrid.getRequests().get(0).getBody(), Mail.class);
            assertEquals("to@example.com", mail.getPersonalization().get(0).getTos().get(0).getEmail());
            assertEquals("cc@example.com", mail.getPersonalization().get(0).getCcs().get(0).getEmail());
            assertEquals("from@example.com", mail.getFrom().getEmail());
        }
    }
}
