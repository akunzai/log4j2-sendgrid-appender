package com.github.akunzai.log4j;

import com.sendgrid.helpers.mail.objects.Email;
import jakarta.mail.Message;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SendGridMessageBuilderTest {

    @Test
    public void testMessageBuilderSetFrom() throws AddressException {
        var builder = new SendGridMessageBuilder();
        var address = "testing@example.com";

        assertNull(builder.build().getFrom());

        builder.setFrom(null);
        var localAddress = InternetAddress.getLocalAddress(null);
        if (localAddress != null) {
            assertEquals(localAddress.getAddress(), builder.setFrom(null).build().getFrom().getEmail());
        }

        builder.setFrom(address);
        assertEquals(address, builder.build().getFrom().getEmail());
    }

    @Test
    public void testMessageBuilderSetFromWithName() throws AddressException {
        var builder = new SendGridMessageBuilder();
        var address = new InternetAddress("Name <testing@example.com>");

        builder.setFrom(address.toString());

        var from = builder.build().getFrom();
        assertEquals(address.getPersonal(), from.getName());
        assertEquals(address.getAddress(), from.getEmail());
    }

    @Test
    public void testMessageBuilderSetReplyTo() throws AddressException {
        var builder = new SendGridMessageBuilder();
        var address = "testing@example.com";

        assertNull(builder.build().getReplyto());

        builder.setReplyTo(null);
        assertNull(builder.build().getReplyto());

        builder.setReplyTo(address);
        assertEquals(InternetAddress.parse(address)[0].getAddress(), builder.build().getReplyto().getEmail());
    }

    @Test
    public void testMessageBuilderSetRecipients() throws AddressException {
        var builder = new SendGridMessageBuilder();
        var addresses = "testing1@example.com,testing2@example.com";

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
        var builder = new SendGridMessageBuilder();
        var subject = "Test Subject";

        assertNull(builder.build().getSubject());

        builder.setSubject(null);
        assertNull(builder.build().getSubject());

        builder.setSubject(subject);
        assertEquals(subject, builder.build().getSubject());
    }
}
