package com.valiantys.service;

import com.atlassian.jira.user.ApplicationUser;

public interface EmailServiceInterface {
    void sendConsentEmail(ApplicationUser user);
    void sendConsentConfirmationEmail(ApplicationUser user);
    void sendConsentDeclinedEmail(ApplicationUser user);
}