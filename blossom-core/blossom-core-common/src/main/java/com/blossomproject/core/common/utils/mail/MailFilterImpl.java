package com.blossomproject.core.common.utils.mail;

import com.google.common.base.Preconditions;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.MimeMessageHelper;

public class MailFilterImpl implements MailFilter {
    private final static Logger LOGGER = LoggerFactory.getLogger(MailFilterImpl.class);
    private final Set<String> filters;
    private final InternetAddress from;

    public MailFilterImpl(Set<String> filters, InternetAddress from) {
        Preconditions.checkNotNull(from);
        this.filters = filters;
        this.from = from;
    }

    @Override
    public MimeMessage filter(MimeMessageHelper mimeMessageHelper) throws MessagingException {
        Map<Message.RecipientType, String[]> recipientsFiltered = this.getFilteredRecipients(mimeMessageHelper.getMimeMessage());

        if (recipientsFiltered.get(Message.RecipientType.TO) != null && recipientsFiltered.get(Message.RecipientType.TO).length > 0) {
            mimeMessageHelper.setTo(recipientsFiltered.get(Message.RecipientType.TO));
            mimeMessageHelper.setFrom(from);

            if (recipientsFiltered.get(Message.RecipientType.BCC) != null && recipientsFiltered.get(Message.RecipientType.BCC).length > 0) {
                mimeMessageHelper.setBcc(recipientsFiltered.get(Message.RecipientType.BCC));
            }else{
                mimeMessageHelper.setBcc(new String[0]);
            }
            if (recipientsFiltered.get(Message.RecipientType.CC) != null && recipientsFiltered.get(Message.RecipientType.CC).length > 0) {
                mimeMessageHelper.setCc(recipientsFiltered.get(Message.RecipientType.CC));
            }else{
                mimeMessageHelper.setCc(new String[0]);
            }

            return mimeMessageHelper.getMimeMessage();
        } else {
            LOGGER.info(
                    "A mail with recipient(s) '{}' and subject '{}' was not sent because the recipient adresses were filtered by mailfilters {}",
                    Arrays.toString(mimeMessageHelper.getMimeMessage().getRecipients(Message.RecipientType.TO)), mimeMessageHelper.getMimeMessage().getSubject(), this.filters);
            throw new MessagingException("Trying to send a mail without recipient!");
        }
    }


    private String[] filterMails(Address[] recipients) {
        if (recipients != null) {
            if (this.filters != null && !this.filters.isEmpty()) {
                return Stream.of(recipients).map(this::getEmailAdress).filter(s -> this.filters.stream().anyMatch(s::matches)).toArray(String[]::new);
            } else {
                return Stream.of(recipients).map(this::getEmailAdress).toArray(String[]::new);
            }
        } else {
            return null;
        }
    }

    private String getEmailAdress(Address recipient) {
        if (!(recipient instanceof InternetAddress)) {
            return recipient.toString();
        } else {
            return ((InternetAddress) recipient).getAddress();
        }
    }

    private Map<Message.RecipientType, String[]> getFilteredRecipients(MimeMessage mimeMessage) throws MessagingException {
        Map<Message.RecipientType, String[]> result = new HashMap<>();

        for (Message.RecipientType recipientType :
                new Message.RecipientType[]{Message.RecipientType.BCC, Message.RecipientType.CC, Message.RecipientType.TO}
                ) {
            result.put(recipientType, filterMails(mimeMessage.getRecipients(recipientType)));
        }
        return result;
    }

}



