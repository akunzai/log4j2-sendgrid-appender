package com.github.akunzai.log4j;

import org.junit.jupiter.api.Test;

import javax.mail.Message;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SendGridMessageBuilderTest {

    @Test
    public void testMessageBuilderSetFrom() throws AddressException {
        final SendGridMessageBuilder builder = new SendGridMessageBuilder();
        final String address = "testing@example.com";

        assertNull(builder.build().getFrom());

        builder.setFrom(null);
        final InternetAddress localAddress = InternetAddress.getLocalAddress(null);
        if (localAddress != null) {
            assertEquals(localAddress.getAddress(), builder.setFrom(null).build().getFrom().getEmail());
        }

        builder.setFrom(address);
        assertEquals(address, builder.build().getFrom().getEmail());
    }

    @Test
    public void testMessageBuilderSetReplyTo() throws AddressException {
        final SendGridMessageBuilder builder = new SendGridMessageBuilder();
        final String address = "testing@example.com";

        assertNull(builder.build().getReplyto());

        builder.setReplyTo(null);
        assertNull(builder.build().getReplyto());

        builder.setReplyTo(address);
        assertEquals(InternetAddress.parse(address)[0].getAddress(), builder.build().getReplyto().getEmail());
    }

    @Test
    public void testMessageBuilderSetRecipients() throws AddressException {
        final SendGridMessageBuilder builder = new SendGridMessageBuilder();
        final String addresses = "testing1@example.com,testing2@example.com";

        assertNull(builder.build().getPersonalization());

        builder.setRecipients(Message.RecipientType.TO, null);
        assertTrue(builder.build().getPersonalization().get(0).getTos().isEmpty());

        builder.setRecipients(Message.RecipientType.TO, addresses);
        assertArrayEquals(InternetAddress.parse(addresses),
                builder.build().getPersonalization().get(0).getTos().stream().map(to -> {
                    try {
                        return new InternetAddress(to.getEmail());
                    } catch (AddressException e) {
                        throw new RuntimeException(e.getMessage(), e);
                    }
                }).toArray());
    }

    @Test
    public void testMessageBuilderSetSubject() {
        final SendGridMessageBuilder builder = new SendGridMessageBuilder();
        final String subject = "Test Subject";

        assertNull(builder.build().getSubject());

        builder.setSubject(null);
        assertNull(builder.build().getSubject());

        builder.setSubject(subject);
        assertEquals(subject, builder.build().getSubject());
    }
}
