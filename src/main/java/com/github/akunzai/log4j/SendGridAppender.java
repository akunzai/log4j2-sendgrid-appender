package com.github.akunzai.log4j;

import org.apache.logging.log4j.core.*;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.appender.ManagerFactory;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.*;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.apache.logging.log4j.core.filter.ThresholdFilter;
import org.apache.logging.log4j.core.layout.HtmlLayout;
import org.apache.logging.log4j.core.util.Booleans;

import java.io.Serializable;

/**
 * Send an e-mail when a specific logging event occurs, typically on errors or fatal errors.
 *
 * <p>
 * The number of logging events delivered in this e-mail depend on the value of
 * <b>BufferSize</b> option. The <code>SendGridAppender</code> keeps only the last
 * <code>BufferSize</code> logging events in its cyclic buffer. This keeps
 * memory requirements at a reasonable level while still delivering useful
 * application context.
 * <p>
 * By default, an email message will format as HTML. This can be modified by
 * setting a layout for the appender.
 * <p>
 * By default, an email message will be sent when an ERROR or higher severity
 * message is appended. This can be modified by setting a filter for the
 * appender.
 */
@Plugin(name = "SendGrid", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE, printObject = true)
public class SendGridAppender extends AbstractAppender {

    /**
     * The SendGrid Manager
     */
    final SendGridManager manager;

    private static final int DEFAULT_BUFFER_SIZE = 512;

    private SendGridAppender(final String name, final Filter filter, final Layout<? extends Serializable> layout,
                             final SendGridManager manager,
                             final boolean ignoreExceptions,
                             final Property[] properties) {
        super(name, filter, layout, ignoreExceptions, properties);
        this.manager = manager;
    }

    /**
     * The Plugin Builder for SendGridAppender
     */
    public static class Builder extends AbstractAppender.Builder<Builder>
            implements org.apache.logging.log4j.core.util.Builder<SendGridAppender> {
        @PluginBuilderAttribute
        private String to;

        @PluginBuilderAttribute
        private String cc;

        @PluginBuilderAttribute
        private String bcc;

        @PluginBuilderAttribute
        private String from;

        @PluginBuilderAttribute
        private String replyTo;

        @PluginBuilderAttribute
        private String subject;

        @PluginBuilderAttribute
        private String host;

        @PluginBuilderAttribute(sensitive = true)
        private String apiKey;

        @PluginBuilderAttribute
        private boolean sandboxMode;

        @PluginBuilderAttribute
        private int bufferSize = DEFAULT_BUFFER_SIZE;

        private ManagerFactory<SendGridManager, SendGridManager.FactoryData> factory;

        /**
         * The Plugin Builder for SendGridAppender
         */
        public Builder() {
        }

        /**
         * Set the recipient email addresses
         *
         * @param to Comma-separated list of recipient email addresses.
         * @return Builder
         */
        public Builder setTo(final String to) {
            this.to = to;
            return this;
        }

        /**
         * Set the CC email addresses
         *
         * @param cc Comma-separated list of CC email addresses.
         * @return Builder
         */
        public Builder setCc(final String cc) {
            this.cc = cc;
            return this;
        }

        /**
         * Set the BCC email addresses
         *
         * @param bcc Comma-separated list of BCC email addresses.
         * @return Builder
         */
        public Builder setBcc(final String bcc) {
            this.bcc = bcc;
            return this;
        }

        /**
         * Set the email address of the sender
         *
         * @param from Email address of the sender.
         * @return Builder
         */
        public Builder setFrom(final String from) {
            this.from = from;
            return this;
        }

        /**
         * Set the Reply-To email addresses
         *
         * @param replyTo Comma-separated list of Reply-To email addresses.
         * @return Builder
         */
        public Builder setReplyTo(final String replyTo) {
            this.replyTo = replyTo;
            return this;
        }

        /**
         * Set the email subject
         *
         * @param subject Subject template for the email messages.
         * @return Builder
         * @see org.apache.logging.log4j.core.layout.PatternLayout
         */
        public Builder setSubject(final String subject) {
            this.subject = subject;
            return this;
        }

        /**
         * Set the SendGrid host
         *
         * @param host The SendGrid host. By defaults, use api.sendgrid.com.
         * @return Builder
         */
        public Builder setHost(final String host) {
            this.host = host;
            return this;
        }

        /**
         * Set the SendGrid API Key
         *
         * @param apiKey The SendGrid API Key
         * @return Builder
         */
        public Builder setApiKey(final String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        /**
         * Set the SendGrid Sandbox mode
         *
         * @param sandboxMode Enable the SendGrid <a href="https://sendgrid.com/docs/for-developers/sending-email/sandbox-mode/">Sandbox mode</a>?
         * @return Builder
         */
        public Builder setSandboxMode(final boolean sandboxMode) {
            this.sandboxMode = sandboxMode;
            return this;
        }

        /**
         * Set the customized SendGridManager factory for testing
         *
         * @param factory The SendGridManager factory
         * @return Builder
         */
        public Builder setFactory(final ManagerFactory<SendGridManager, SendGridManager.FactoryData> factory) {
            this.factory = factory;
            return this;
        }

        /**
         * Set the buffer size before sending an email
         *
         * @param bufferSize Number of log events to buffer before sending an email. Defaults to {@value #DEFAULT_BUFFER_SIZE}.
         * @return Builder
         */
        public Builder setBufferSize(int bufferSize) {
            this.bufferSize = bufferSize;
            return this;
        }

        /**
         * Set the email body layout
         *
         * @param layout Specifies the layout used for the email message body. By default, this uses the {@linkplain HtmlLayout#createDefaultLayout() default HTML layout}.
         * @return Builder
         */
        @Override
        public Builder setLayout(Layout<? extends Serializable> layout) {
            return super.setLayout(layout);
        }

        /**
         * Set the filter for this appender
         *
         * @param filter Specifies the filter used for this appender. By default, uses a {@link ThresholdFilter} with a level of ERROR.
         * @return Builder
         */
        @Override
        public Builder setFilter(Filter filter) {
            return super.setFilter(filter);
        }

        @Override
        public SendGridAppender build() {
            if (getLayout() == null) {
                setLayout(HtmlLayout.createDefaultLayout());
            }
            if (getFilter() == null) {
                setFilter(ThresholdFilter.createFilter(null, null, null));
            }
            final Configuration configuration = getConfiguration();
            final SendGridManager manager = SendGridManager.getSendGridManager(
                    configuration, to, cc, bcc, from, replyTo, subject, host, apiKey, sandboxMode,
                    bufferSize, factory);
            return new SendGridAppender(getName(),
                    getFilter(),
                    getLayout(),
                    manager,
                    isIgnoreExceptions(),
                    getPropertyArray());
        }
    }

    /**
     * Create a Plugin Builder for SendGridAppender
     *
     * @return Builder
     */
    @PluginBuilderFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * Create a SendGrid Appender.
     *
     * @param config      The logging Configuration
     * @param name        The name of the Appender
     * @param to          The comma-separated list of recipient email addresses.
     * @param cc          The comma-separated list of CC email addresses.
     * @param bcc         The comma-separated list of BCC email addresses.
     * @param from        The email address of the sender.
     * @param replyTo     The comma-separated list of reply-to email addresses.
     * @param subject     The subject of the email message.
     * @param host        The SendGrid host (defaults to api.sendgrid.com).
     * @param apiKey      The SendGrid API Key
     * @param sandboxMode Enable The SendGrid <a href="https://sendgrid.com/docs/for-developers/sending-email/sandbox-mode/">Sandbox Mode</a>?
     * @param bufferSize  How many log events should be buffered for inclusion in the message?
     * @param layout      The layout to use (defaults to HtmlLayout).
     * @param filter      The Filter or null (defaults to ThresholdFilter, level of ERROR).
     * @param ignore      If {@code "true"} (default) exceptions encountered when appending events are logged; otherwise they are propagated to the caller.
     * @return The SendGridAppender
     * @see Builder
     * @deprecated Use {@link #newBuilder()} to create and configure a {@link SendGridAppender.Builder} instance.
     */
    @Deprecated
    public static SendGridAppender createAppender(@PluginConfiguration final Configuration config,
                                                  @PluginAttribute("name") @Required final String name,
                                                  @PluginAttribute("to") final String to,
                                                  @PluginAttribute("cc") final String cc,
                                                  @PluginAttribute("bcc") final String bcc,
                                                  @PluginAttribute("from") final String from,
                                                  @PluginAttribute("replyTo") final String replyTo,
                                                  @PluginAttribute("subject") final String subject,
                                                  @PluginAttribute("host") final String host,
                                                  @PluginAttribute(value = "apiKey", sensitive = true) final String apiKey,
                                                  @PluginAttribute("sandboxMode") final String sandboxMode,
                                                  @PluginAttribute("bufferSize") final String bufferSize,
                                                  @PluginElement("Layout") Layout<? extends Serializable> layout,
                                                  @PluginElement("Filter") Filter filter,
                                                  @PluginAttribute("ignoreExceptions") final String ignore) {
        if (name == null || name.isEmpty()) {
            LOGGER.error("No name provided for SendGridAppender");
            return null;
        }
        if (layout == null) {
            layout = HtmlLayout.createDefaultLayout();
        }
        if (filter == null) {
            filter = ThresholdFilter.createFilter(null, null, null);
        }

        final Configuration configuration = config != null ? config : new DefaultConfiguration();
        final SendGridManager manager = SendGridManager.getSendGridManager(
                configuration, to, cc, bcc, from, replyTo, subject, host, apiKey, Boolean.parseBoolean(sandboxMode),
                bufferSize == null ? DEFAULT_BUFFER_SIZE : Integer.parseInt(bufferSize), null);

        if (manager == null) {
            return null;
        }

        final boolean ignoreExceptions = Booleans.parseBoolean(ignore, true);
        return new SendGridAppender(name, filter, layout, manager, ignoreExceptions, null);
    }

    /**
     * Capture all events in CyclicBuffer.
     *
     * @param event The Log event.
     * @return true if the event should be filtered.
     */
    @Override
    public boolean isFiltered(final LogEvent event) {
        final boolean filtered = super.isFiltered(event);
        if (filtered) {
            manager.add(event);
        }
        return filtered;
    }

    /**
     * Perform SendGridAppender specific appending actions, mainly adding the event
     * to a cyclic buffer and checking if the event triggers an e-mail to be
     * sent.
     *
     * @param event The Log event.
     */
    @Override
    public void append(final LogEvent event) {
        manager.sendEvents(getLayout(), event);
    }
}
