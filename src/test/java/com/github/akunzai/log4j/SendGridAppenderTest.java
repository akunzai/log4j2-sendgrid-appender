package com.github.akunzai.log4j;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sendgrid.Content;
import com.sendgrid.Mail;
import com.sendgrid.Personalization;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.Iterator;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class SendGridAppenderTest {

    @BeforeClass
    public static void setupClass() {
        SendGridAppender.setManagerFactory(new MockSendGridManagerFactory());
    }

    @Test
    public void testDelivery() {
        final String subjectKey = getClass().getName();
        final String subjectValue = "SubjectValue1";
        ThreadContext.put(subjectKey, subjectValue);
        final SendGridAppender appender = SendGridAppender.createAppender(null, "Test",
            "to@example.com", "cc@example.com", "bcc@example.com", "from@example.com", "replyTo@example.com",
            "Subject Pattern %X{" + subjectKey + "}", null, "apiKey",
            "3", null, null, "true");
        assertNotNull(appender);
        appender.start();

        final LoggerContext context = Configurator.initialize(
            ConfigurationBuilderFactory.newConfigurationBuilder()
            .setStatusLevel(Level.OFF)
            .build());
        final Logger logger = context.getLogger("SendGridAppenderTest");
        logger.addAppender(appender);
        logger.setAdditive(false);
        logger.setLevel(Level.DEBUG);

        logger.debug("Debug message #1");
        logger.debug("Debug message #2");
        logger.debug("Debug message #3");
        logger.debug("Debug message #4");
        logger.error("Error with exception", new RuntimeException("Exception message"));
        logger.error("Error message #2");
        final MockSendGrid sendGrid = (MockSendGrid) appender.getManager().getSendGrid();
        assertEquals(2, sendGrid.getRequests().size());
        final ObjectMapper mapper = new ObjectMapper();
        final Iterator<Mail> messages = sendGrid.getRequests().stream().map(req -> {
            try {
                return mapper.readValue(req.getBody(), Mail.class);
            } catch (IOException e) {
                throw new RuntimeException(e.getMessage(),e);
            }
        }).collect(Collectors.toList()).iterator();
        final Mail message = messages.next();
        final Personalization personalization = message.getPersonalization().get(0);

        assertEquals("to@example.com", personalization.getTos().get(0).getEmail());
        assertEquals("cc@example.com", personalization.getCcs().get(0).getEmail());
        assertEquals("bcc@example.com", personalization.getBccs().get(0).getEmail());
        assertEquals("from@example.com", message.getFrom().getEmail());
        assertEquals("replyTo@example.com", message.getReplyto().getEmail());
        assertEquals("Subject Pattern " + subjectValue, message.getSubject());

        final Content content = message.getContent().get(0);
        assertEquals("text/html", content.getType());
        final String body = content.getValue();
        assertFalse(body.contains("Debug message #1"));
        assertTrue(body.contains("Debug message #2"));
        assertTrue(body.contains("Debug message #3"));
        assertTrue(body.contains("Debug message #4"));
        assertTrue(body.contains("Error with exception"));
        assertTrue(body.contains("RuntimeException"));
        assertTrue(body.contains("Exception message"));
        assertFalse(body.contains("Error message #2"));

        final Mail message2 = messages.next();
        final String body2 = message2.getContent().get(0).getValue();
        assertFalse(body2.contains("Debug message #4"));
        assertFalse(body2.contains("Error with exception"));
        assertTrue(body2.contains("Error message #2"));
    }
}