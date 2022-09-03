package com.github.akunzai.log4j;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.MailSettings;
import com.sendgrid.helpers.mail.objects.Setting;
import jakarta.mail.Message;
import jakarta.mail.internet.AddressException;
import org.apache.logging.log4j.LoggingException;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractManager;
import org.apache.logging.log4j.core.appender.ManagerFactory;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.impl.MutableLogEvent;
import org.apache.logging.log4j.core.layout.AbstractStringLayout;
import org.apache.logging.log4j.core.layout.HtmlLayout;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.util.CyclicBuffer;
import org.apache.logging.log4j.core.util.NameUtil;
import org.apache.logging.log4j.message.ReusableMessage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Manager for sending SendGrid events.
 */
class SendGridManager extends AbstractManager {
    private static final SendGridManagerFactory FACTORY = new SendGridManagerFactory();
    private static final MailSettings SANDBOX_MAIL_SETTINGS;

    static {
        final Setting setting = new Setting();
        setting.setEnable(true);
        SANDBOX_MAIL_SETTINGS = new MailSettings();
        SANDBOX_MAIL_SETTINGS.setSandboxMode(setting);
    }

    final SendGrid sendGrid;

    private final CyclicBuffer<LogEvent> buffer;

    private final FactoryData data;

    SendGridManager(final String name, final SendGrid sendGrid, final FactoryData data) {
        super(null, name);
        this.sendGrid = sendGrid;
        this.data = data;
        this.buffer = new CyclicBuffer<>(LogEvent.class, data.numElements);
    }

    static SendGridManager getSendGridManager(final Configuration config,
                                              final String to, final String cc, final String bcc,
                                              final String from, final String replyTo,
                                              final String subject, final String host,
                                              final String apiKey,
                                              final boolean sandboxMode,
                                              final int numElements,
                                              final ManagerFactory<SendGridManager, FactoryData> factory
    ) {
        final String name = "SendGrid:" + NameUtil.md5(host + ':' + apiKey);
        final AbstractStringLayout.Serializer subjectSerializer = PatternLayout.newSerializerBuilder()
                .setConfiguration(config)
                .setPattern(subject)
                .build();

        return getManager(name, factory == null ? FACTORY : factory, new FactoryData(to, cc, bcc, from, replyTo,
                subjectSerializer, host, apiKey, sandboxMode, numElements));
    }

    void add(LogEvent event) {
        if (event instanceof Log4jLogEvent && event.getMessage() instanceof ReusableMessage) {
            ((Log4jLogEvent) event).makeMessageImmutable();
        } else if (event instanceof MutableLogEvent) {
            event = ((MutableLogEvent) event).createMemento();
        }
        buffer.add(event);
    }

    /**
     * Send the contents of the cyclic buffer as an e-mail message.
     *
     * @param layout      The layout for formatting the events.
     * @param appendEvent The event that triggered to send.
     */
    void sendEvents(final Layout<?> layout, final LogEvent appendEvent) {
        try {
            final Mail message = createMailMessage(appendEvent);
            final Content content = new Content();
            if (layout instanceof HtmlLayout) {
                content.setType("text/html");
            } else {
                content.setType("text/plain");
            }
            final StringBuilder stringBuilder = new StringBuilder();
            final byte[] header = layout.getHeader();
            if (header != null) {
                stringBuilder.append(new String(header, StandardCharsets.UTF_8));
            }
            final LogEvent[] priorEvents = buffer.removeAll();
            for (final LogEvent priorEvent : priorEvents) {
                stringBuilder.append(layout.toSerializable(priorEvent));
            }
            stringBuilder.append(layout.toSerializable(appendEvent));
            final byte[] footer = layout.getFooter();
            if (footer != null) {
                stringBuilder.append(new String(footer, StandardCharsets.UTF_8));
            }
            content.setValue(stringBuilder.toString());
            message.addContent(content);
            final Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(message.build());
            sendGrid.api(request);
        } catch (IOException | AddressException e) {
            logError("Caught exception while sending e-mail notification.", e);
            throw new LoggingException("Error occurred while sending email", e);
        }
    }

    private Mail createMailMessage(final LogEvent appendEvent) throws AddressException {
        final Mail message = new SendGridMessageBuilder()
                .setFrom(data.from)
                .setReplyTo(data.replyTo)
                .setRecipients(Message.RecipientType.TO, data.to)
                .setRecipients(Message.RecipientType.CC, data.cc)
                .setRecipients(Message.RecipientType.BCC, data.bcc)
                .setSubject(data.subject.toSerializable(appendEvent)).build();
        if (data.sandboxMode) {
            message.setMailSettings(SANDBOX_MAIL_SETTINGS);
        }
        return message;
    }

    /**
     * Factory data.
     */
    static class FactoryData {
        final String to;
        final String cc;
        final String bcc;
        final String from;
        final String replyTo;
        final AbstractStringLayout.Serializer subject;
        final String host;
        final String apiKey;
        final boolean sandboxMode;
        final int numElements;

        FactoryData(
                final String to,
                final String cc,
                final String bcc,
                final String from,
                final String replyTo,
                final AbstractStringLayout.Serializer subject,
                final String host,
                final String apiKey,
                final boolean sandboxMode,
                final int numElements) {
            this.to = to;
            this.cc = cc;
            this.bcc = bcc;
            this.from = from;
            this.replyTo = replyTo;
            this.subject = subject;
            this.host = host;
            this.apiKey = apiKey;
            this.sandboxMode = sandboxMode;
            this.numElements = numElements;
        }
    }

    static class SendGridManagerFactory implements ManagerFactory<SendGridManager, FactoryData> {
        @Override
        public SendGridManager createManager(final String name, final FactoryData data) {
            final SendGrid sendGrid = new SendGrid(data.apiKey);
            if (data.host != null && !data.host.isEmpty()) {
                sendGrid.setHost(data.host);
            }
            return new SendGridManager(name, sendGrid, data);
        }
    }
}
