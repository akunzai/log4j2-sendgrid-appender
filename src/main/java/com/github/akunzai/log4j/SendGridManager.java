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
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.impl.MutableLogEvent;
import org.apache.logging.log4j.core.layout.HtmlLayout;
import org.apache.logging.log4j.core.util.CyclicBuffer;
import org.apache.logging.log4j.message.ReusableMessage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

import static org.apache.logging.log4j.core.layout.AbstractStringLayout.Serializer;

/**
 * Manager for sending SendGrid events.
 */
class SendGridManager extends AbstractManager {
    static final SendGridManagerFactory FACTORY = new SendGridManagerFactory();
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
        this.buffer = new CyclicBuffer<>(LogEvent.class, data.bufferSize);
    }

    public void add(LogEvent event) {
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
    public void sendEvents(final Layout<?> layout, final LogEvent appendEvent) {
        try {
            final Mail message = createMailMessage(data, appendEvent);
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

    private static Mail createMailMessage(final FactoryData data, final LogEvent appendEvent) throws AddressException {
        final Mail message = new SendGridMessageBuilder()
                .setFrom(data.from)
                .setReplyTo(data.replyTo)
                .setRecipients(Message.RecipientType.TO, data.to)
                .setRecipients(Message.RecipientType.CC, data.cc)
                .setRecipients(Message.RecipientType.BCC, data.bcc)
                .setSubject(data.subjectSerializer.toSerializable(appendEvent)).build();
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
        final String subject;
        final Serializer subjectSerializer;
        final String host;
        final String apiKey;
        final boolean sandboxMode;
        final int bufferSize;
        final String managerName;


        FactoryData(
                final String to,
                final String cc,
                final String bcc,
                final String from,
                final String replyTo,
                final String subject,
                final Serializer subjectSerializer,
                final String host,
                final String apiKey,
                final boolean sandboxMode,
                final int bufferSize) {
            this.to = to;
            this.cc = cc;
            this.bcc = bcc;
            this.from = from;
            this.replyTo = replyTo;
            this.subject = subject;
            this.subjectSerializer = subjectSerializer;
            this.host = host;
            this.apiKey = apiKey;
            this.sandboxMode = sandboxMode;
            this.bufferSize = bufferSize;
            this.managerName = createManagerName(to, cc, bcc, from, replyTo, subject, host, apiKey, sandboxMode);
        }
    }

    private static String createManagerName(
            final String to,
            final String cc,
            final String bcc,
            final String from,
            final String replyTo,
            final String subject,
            final String host,
            final String apiKey,
            final boolean sandboxMode) {
        final StringBuilder sb = new StringBuilder();
        if (to != null) {
            sb.append(to);
        }
        sb.append(':');
        if (cc != null) {
            sb.append(cc);
        }
        sb.append(':');
        if (bcc != null) {
            sb.append(bcc);
        }
        sb.append(':');
        if (from != null) {
            sb.append(from);
        }
        sb.append(':');
        if (replyTo != null) {
            sb.append(replyTo);
        }
        sb.append(':');
        if (subject != null) {
            sb.append(subject);
        }
        sb.append(':');
        sb.append(host).append(':').append(apiKey);
        sb.append(sandboxMode ? ":sandbox:" : "::");
        return "SendGrid:" + sb;
    }

    static class SendGridManagerFactory implements ManagerFactory<SendGridManager, FactoryData> {

        private Function<String,SendGrid> sendGridFactory = SendGrid::new;

        /**
         * Set the SendGrid factory for testing
         *
         * @param sendGridFactory the SendGrid factory
         */
        public void setSendGridFactory(Function<String,SendGrid> sendGridFactory) {
            this.sendGridFactory = sendGridFactory;
        }

        @Override
        public SendGridManager createManager(final String name, final FactoryData data) {
            final SendGrid sendGrid = sendGridFactory.apply(data.apiKey);
            if (data.host != null && !data.host.isEmpty()) {
                sendGrid.setHost(data.host);
            }
            return new SendGridManager(name, sendGrid, data);
        }
    }
}
