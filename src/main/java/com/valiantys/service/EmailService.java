package com.valiantys.service;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.mail.Email;
import com.atlassian.mail.queue.SingleMailQueueItem;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service responsible for handling all email communications related to migration consent.
 */
@Component
public class EmailService implements EmailServiceInterface {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final String EMAIL_FROM = "bugs@mojang.com";

    @Override
    public void sendFullConsentEmail(ApplicationUser user) {
        sendConfirmationEmail(user, "Yes, I consent to the transfer of both my account and my bugs.");
    }

    @Override
    public void sendBugsOnlyConsentEmail(ApplicationUser user) {
        sendConfirmationEmail(user, "Yes, I consent to the transfer of only my bugs and associated personal data. My account will not be transferred.");
    }

    @Override
    public void sendNoConsentEmail(ApplicationUser user) {
        sendConfirmationEmail(user, "No, I do not consent to any transfer of personal information. My bugs will still transfer, but personal data will be removed.");
    }

    private void sendConfirmationEmail(ApplicationUser user, String chosenOption) {
        if (user == null || user.getEmailAddress() == null) {
            log.error("Invalid user or email address");
            return;
        }

        try {
            String emailBody = buildSimpleConfirmationEmail(user.getDisplayName(), chosenOption);
            Email email = new Email(user.getEmailAddress())
                .setFrom(EMAIL_FROM)
                .setSubject("Confirmation: Data Migration Consent Choice")
                .setMimeType("text/html")
                .setBody(emailBody);

            SingleMailQueueItem item = new SingleMailQueueItem(email);
            ComponentAccessor.getMailQueue().addItem(item);
            
            log.info("Confirmation email sent to user: {}", user.getUsername());
        } catch (Exception e) {
            log.error("Failed to send confirmation email to user: {}", user.getUsername(), e);
        }
    }

    private String buildSimpleConfirmationEmail(String userName, String chosenOption) {
        StringBuilder body = new StringBuilder();
        body.append("<html>")
            .append("<body style=\"font-family: Arial, sans-serif; line-height: 1.6; color: #333;\">")
            .append("<div style=\"max-width: 600px; margin: 0 auto; padding: 20px;\">")
            .append("<h2 style=\"color: #0052CC;\">Data Migration Consent Confirmation</h2>")
            .append("<p>Hello ").append(userName).append(",</p>")
            .append("<p>Thank you for your response regarding the data migration.</p>")
            .append("<p><strong>Your choice:</strong></p>")
            .append("<p style=\"background-color: #F4F5F7; padding: 15px; border-radius: 3px;\">")
            .append(chosenOption)
            .append("</p>")
            .append("</div>")
            .append("</body>")
            .append("</html>");
            
        return body.toString();
    }
}