package com.github.akunzai.log4j;

import com.sendgrid.SendGrid;
import org.apache.logging.log4j.core.appender.ManagerFactory;

public class MockSendGridManagerFactory implements ManagerFactory<SendGridManager, SendGridManager.FactoryData> {
    @Override
    public SendGridManager createManager(final String name, final SendGridManager.FactoryData data) {
        final SendGrid sendGrid = new MockSendGrid(data.apiKey);
        return new SendGridManager(name, sendGrid, data);
    }
}
