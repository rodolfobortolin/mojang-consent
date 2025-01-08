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
    
    /**
     * Sends the initial consent request email to a user.
     */
    public void sendConsentEmail(ApplicationUser user) {
        if (user == null || user.getEmailAddress() == null) {
            log.error("Invalid user or email address");
            return;
        }

        try {
            String emailBody = buildConsentEmailBody(user.getDisplayName());
            Email email = new Email(user.getEmailAddress())
                .setFrom(EMAIL_FROM)
                .setSubject("Action Required: Data Migration Consent")
                .setMimeType("text/html")
                .setBody(emailBody);

            SingleMailQueueItem item = new SingleMailQueueItem(email);
            ComponentAccessor.getMailQueue().addItem(item);
            
            log.info("Consent request email sent to user: {}", user.getUsername());
        } catch (Exception e) {
            log.error("Failed to send consent email to user: {}", user.getUsername(), e);
        }
    }

    /**
     * Sends a confirmation email when a user provides consent.
     */
    public void sendConsentConfirmationEmail(ApplicationUser user) {
        if (user == null || user.getEmailAddress() == null) {
            log.error("Invalid user or email address");
            return;
        }

        try {
            String emailBody = buildConsentConfirmationEmailBody(user.getDisplayName());
            Email email = new Email(user.getEmailAddress())
                .setFrom(EMAIL_FROM)
                .setSubject("Confirmation: Data Migration Consent Provided")
                .setMimeType("text/html")
                .setBody(emailBody);

            SingleMailQueueItem item = new SingleMailQueueItem(email);
            ComponentAccessor.getMailQueue().addItem(item);
            
            log.info("Consent confirmation email sent to user: {}", user.getUsername());
        } catch (Exception e) {
            log.error("Failed to send consent confirmation email to user: {}", user.getUsername(), e);
        }
    }

    /**
     * Sends a confirmation email when a user declines consent.
     */
    public void sendConsentDeclinedEmail(ApplicationUser user) {
        if (user == null || user.getEmailAddress() == null) {
            log.error("Invalid user or email address");
            return;
        }

        try {
            String emailBody = buildConsentDeclinedEmailBody(user.getDisplayName());
            Email email = new Email(user.getEmailAddress())
                .setFrom(EMAIL_FROM)
                .setSubject("Confirmation: Data Migration Consent Declined")
                .setMimeType("text/html")
                .setBody(emailBody);

            SingleMailQueueItem item = new SingleMailQueueItem(email);
            ComponentAccessor.getMailQueue().addItem(item);
            
            log.info("Consent declined email sent to user: {}", user.getUsername());
        } catch (Exception e) {
            log.error("Failed to send consent declined email to user: {}", user.getUsername(), e);
        }
    }

    private String buildConsentEmailBody(String userName) {
        String baseUrl = ComponentAccessor.getApplicationProperties().getString("jira.baseurl");
        String consentUrl = baseUrl + "/plugins/servlet/migration-consent";
        
        StringBuilder body = new StringBuilder();
        body.append("<html>")
            .append("<body style=\"font-family: Arial, sans-serif; line-height: 1.6; color: #333;\">")
            .append("<div style=\"max-width: 600px; margin: 0 auto; padding: 20px;\">")
            .append("<h2 style=\"color: #0052CC;\">Important: Data Migration Notice</h2>")
            .append("<p>Hello ").append(userName).append(",</p>")
            .append("<p>We are preparing to migrate our Jira instance. Your consent is required for data migration.</p>")
            .append("<div style=\"margin: 30px 0;\">")
            .append("<a href=\"").append(consentUrl).append("\" style=\"background-color: #0052CC; color: white; padding: 12px 24px; text-decoration: none; border-radius: 3px;\">")
            .append("Review and Provide Consent")
            .append("</a>")
            .append("</div>")
            .append("</div>")
            .append("</body>")
            .append("</html>");
            
        return body.toString();
    }

    private String buildConsentConfirmationEmailBody(String userName) {
        StringBuilder body = new StringBuilder();
        body.append("<html>")
            .append("<body style=\"font-family: Arial, sans-serif; line-height: 1.6; color: #333;\">")
            .append("<div style=\"max-width: 600px; margin: 0 auto; padding: 20px;\">")
            .append("<h2 style=\"color: #0052CC;\">Data Migration Consent Confirmed</h2>")
            .append("<p>Hello ").append(userName).append(",</p>")
            .append("<p>Thank you for providing your consent for the data migration process. ")
            .append("Your data will be migrated with your identity preserved.</p>")
            .append("<p>If you have any questions, please contact your system administrator.</p>")
            .append("</div>")
            .append("</body>")
            .append("</html>");
            
        return body.toString();
    }

    private String buildConsentDeclinedEmailBody(String userName) {
        StringBuilder body = new StringBuilder();
        body.append("<html>")
            .append("<body style=\"font-family: Arial, sans-serif; line-height: 1.6; color: #333;\">")
            .append("<div style=\"max-width: 600px; margin: 0 auto; padding: 20px;\">")
            .append("<h2 style=\"color: #0052CC;\">Data Migration Consent Declined</h2>")
            .append("<p>Hello ").append(userName).append(",</p>")
            .append("<p>We have recorded your decision to decline consent for the data migration process. ")
            .append("Your data will be migrated anonymously.</p>")
            .append("<p>If you wish to change your decision, please contact your system administrator.</p>")
            .append("</div>")
            .append("</body>")
            .append("</html>");
            
        return body.toString();
    }
}