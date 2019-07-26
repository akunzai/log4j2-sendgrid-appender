package com.github.akunzai.log4j;

import com.sendgrid.Email;
import com.sendgrid.Mail;
import com.sendgrid.Personalization;
import org.apache.logging.log4j.core.util.Builder;

import javax.mail.Message;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.util.*;

/**
 * Builder for {@link Mail} instances.
 */
public class SendGridMessageBuilder implements Builder<Mail> {
    private final Mail message;

    public SendGridMessageBuilder() {
        this.message = new Mail();
    }

    public SendGridMessageBuilder setFrom(final String from) throws AddressException {
        final Email email = parseEmail(from);
        if (email != null) {
            message.setFrom(email);
        }else{
            InternetAddress address = InternetAddress.getLocalAddress(null);
            if (address != null){
                if (address.getPersonal() == null || address.getPersonal().isEmpty()) {
                    message.setFrom(new Email(address.getAddress()));
                }
                else{
                    message.setFrom(new Email(address.getAddress(),address.getPersonal()));
                }
            }
        }
        return this;
    }

    public SendGridMessageBuilder setReplyTo(final String replyTo) throws AddressException {
        final Email email = parseEmail(replyTo);
        if (email != null) {
            message.setReplyTo(email);
        }
        return this;
    }

    public SendGridMessageBuilder setRecipients(final Message.RecipientType recipientType,
                                                final String recipients) throws AddressException {
        final List<Personalization> personalizationList = message.getPersonalization();
        final Personalization personalization = personalizationList == null ? new Personalization() : personalizationList
            .get(0);
        final Collection<Email> emails = parseEmails(recipients);
        if (recipientType == Message.RecipientType.TO) {
            for (final Email email : emails) {
                personalization.addTo(email);
            }
        } else if (recipientType == Message.RecipientType.CC) {
            for (final Email email : emails) {
                personalization.addCc(email);
            }
        } else if (recipientType == Message.RecipientType.BCC) {
            for (final Email email : emails) {
                personalization.addBcc(email);
            }
        }
        if (personalizationList == null) {
            message.addPersonalization(personalization);
        }
        return this;
    }

    public SendGridMessageBuilder setSubject(final String subject){
        if (subject != null){
            message.setSubject(subject);
        }
        return this;
    }

    @Override
    public Mail build() {
        return message;
    }

    private static Email parseEmail(final String email) throws AddressException {
        if (email == null || email.isEmpty()) return null;
        final InternetAddress address = new InternetAddress(email);
        if (address.getPersonal() == null || address.getPersonal().isEmpty()) {
            return new Email(address.getAddress());
        }
        return new Email(address.getAddress(), address.getPersonal());
    }

    private static Collection<Email> parseEmails(final String email) throws AddressException {
        if (email == null || email.isEmpty()) return Collections.emptySet();
        final InternetAddress[] addresses = InternetAddress.parse(email, true);
        final Collection<Email> emails = new ArrayList<>();
        for (InternetAddress address : addresses) {
            if (address.getPersonal() == null || address.getPersonal().isEmpty()) {
                emails.add(new Email(address.getAddress()));
            } else {
                emails.add(new Email(address.getAddress(), address.getPersonal()));
            }
        }
        return emails;
    }
}
