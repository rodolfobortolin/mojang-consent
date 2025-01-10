package com.valiantys.service;

import com.atlassian.jira.user.ApplicationUser;

public interface EmailServiceInterface {
    void sendFullConsentEmail(ApplicationUser user);
    void sendBugsOnlyConsentEmail(ApplicationUser user);
    void sendNoConsentEmail(ApplicationUser user);
}