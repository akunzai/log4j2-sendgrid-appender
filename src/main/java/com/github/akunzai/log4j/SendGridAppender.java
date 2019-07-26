package com.github.akunzai.log4j;

import org.apache.logging.log4j.core.*;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.appender.ManagerFactory;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
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
 *
 * By default, an email message will formatted as HTML. This can be modified by
 * setting a layout for the appender.
 *
 * By default, an email message will be sent when an ERROR or higher severity
 * message is appended. This can be modified by setting a filter for the
 * appender.
 */
@Plugin(name = "SendGrid", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE, printObject = true)
public class SendGridAppender extends AbstractAppender {

    private static ManagerFactory<SendGridManager, SendGridManager.FactoryData> managerFactory = new SendGridManager.SendGridManagerFactory();

    private static final int DEFAULT_BUFFER_SIZE = 512;

    private final SendGridManager manager;

    private SendGridAppender(final String name, final Filter filter, final Layout<? extends Serializable> layout,
                             final SendGridManager manager,
                             final boolean ignoreExceptions) {
        super(name, filter, layout, ignoreExceptions);
        this.manager = manager;
    }

    // for Unit-Testing
    static void setManagerFactory(ManagerFactory<SendGridManager, SendGridManager.FactoryData> factory){
        if (factory == null) {
            throw new AssertionError("factory is required");
        }
        managerFactory = factory;
    }

    // for Unit-Testing
    SendGridManager getManager(){
        return manager;
    }

    /**
     * Capture all events in CyclicBuffer.
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
     * @param event The Log event.
     */
    @Override
    public void append(final LogEvent event) {
        manager.sendEvents(getLayout(), event);
    }

    /**
     * Create a SendGrid Appender.
     *
     * @param config The logging Configuration
     * @param name The name of the Appender
     * @param to The comma-separated list of recipient email addresses.
     * @param cc The comma-separated list of CC email addresses.
     * @param bcc The comma-separated list of BCC email addresses.
     * @param from The email address of the sender.
     * @param replyTo The comma-separated list of reply-to email addresses.
     * @param subject The subject of the email message.
     * @param host The SendGrid host (defaults to api.sendgrid.com).
     * @param apiKey The SendGrid API Key
     * @param bufferSize How many log events should be buffered for inclusion in the message?
     * @param layout The layout to use (defaults to HtmlLayout).
     * @param filter The Filter or null (defaults to ThresholdFilter, level of ERROR).
     * @param ignore If {@code "true"} (default) exceptions encountered when appending events are logged; otherwise they are propagated to the caller.
     * @return The SendGridAppender
     */
    @PluginFactory
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
                                                  @PluginAttribute("bufferSize") final String bufferSize,
                                                  @PluginElement("Layout") Layout<? extends Serializable> layout,
                                                  @PluginElement("Filter") Filter filter,
                                                  @PluginAttribute("ignoreExceptions") final String ignore) {
        if (name == null || name.isEmpty()) {
            LOGGER.error("No name provided for SendGridAppender");
            return null;
        }
        if (apiKey == null || apiKey.isEmpty()) {
            LOGGER.error("No apiKey provided for SendGridAppender");
            return null;
        }
        if (from == null || from.isEmpty()) {
            LOGGER.error("No from provided for SendGridAppender");
            return null;
        }
        if ((to == null || to.isEmpty()) && (cc == null || cc.isEmpty()) && (bcc == null || bcc.isEmpty())) {
            LOGGER.error("No recipients(to,cc,bcc) provided for SendGridAppender");
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
                configuration, to, cc, bcc, from, replyTo, subject, host, apiKey,
                bufferSize == null ? DEFAULT_BUFFER_SIZE : Integer.parseInt(bufferSize), managerFactory);

        if (manager == null) {
            return null;
        }

        final boolean ignoreExceptions = Booleans.parseBoolean(ignore, true);
        return new SendGridAppender(name, filter, layout, manager, ignoreExceptions);
    }
}
