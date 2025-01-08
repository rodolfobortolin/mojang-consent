package com.valiantys.admin;

import com.atlassian.crowd.embedded.api.Group;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.jira.permission.GlobalPermissionKey;
import com.atlassian.jira.security.GlobalPermissionManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.valiantys.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MigrationConsentAdminAction extends JiraWebActionSupport {

	private static final long serialVersionUID = 1L;

	private static final Logger log = LoggerFactory.getLogger(MigrationConsentAdminAction.class);
    
    private final GlobalPermissionManager permissionManager;
    private final JiraAuthenticationContext authContext;
    private final EmailService emailService;
    private final UserManager userManager;
    private final GroupManager groupManager;  

    
    private String selectedGroup;
    private boolean emailsSent;
    private String errorMessage;
    
    public MigrationConsentAdminAction(
            @ComponentImport GlobalPermissionManager permissionManager,
            @ComponentImport JiraAuthenticationContext authContext,
            @ComponentImport UserManager userManager,
            @ComponentImport GroupManager groupManager,  // Add this
            EmailService emailService) {
        this.permissionManager = permissionManager;
        this.authContext = authContext;
        this.userManager = userManager;
        this.groupManager = groupManager;  // Add this
        this.emailService = emailService;
    }
    
    @Override
    public String doDefault() {
        if (!hasAdminPermission()) {
            return "error";
        }
        return INPUT;
    }
    
    public String doSendEmails() {
        if (!hasAdminPermission()) {
            return "error";
        }
        
        try {
            int emailCount = 0;
            @SuppressWarnings("deprecation")
			Iterable<ApplicationUser> users = userManager.getAllApplicationUsers();
            
            // If a group is selected, get the Group object once before the loop
            Group targetGroup = null;
            if (selectedGroup != null) {
                // Here we have two options: getGroup() or getGroupObject()
                // Let's use getGroupObject() as it seems more explicit
                targetGroup = userManager.getGroupObject(selectedGroup);
                if (targetGroup == null) {
                    log.error("Selected group not found: {}", selectedGroup);
                    errorMessage = "Selected group does not exist";
                    return ERROR;
                }
            }

            for (ApplicationUser user : users) {
                if (user == null || user.getEmailAddress() == null) {
                    continue;
                }

                // Since we don't have a direct isUserInGroup method, 
                // we need to find a different way to check group membership
                // Looking at the available methods, we should check with your
                // GroupManager instead of UserManager for this functionality
                
                // If no group is selected, or if group membership check passes
                if (selectedGroup == null || groupManager.isUserInGroup(user, targetGroup)) {
                    emailService.sendConsentEmail(user);
                    emailCount++;
                }
            }
            
            addErrorMessage("Successfully sent " + emailCount + " consent emails");
            emailsSent = true;
            
        } catch (Exception e) {
            log.error("Error sending consent emails", e);
            errorMessage = "Failed to send emails: " + e.getMessage();
            return ERROR;
        }
        
        return SUCCESS;
    }

    
    private boolean hasAdminPermission() {
        ApplicationUser user = authContext.getLoggedInUser();
        return permissionManager.hasPermission(GlobalPermissionKey.SYSTEM_ADMIN, user);
    }
    
    // Getters and setters
    public String getSelectedGroup() { return selectedGroup; }
    public void setSelectedGroup(String selectedGroup) { this.selectedGroup = selectedGroup; }
    public boolean isEmailsSent() { return emailsSent; }
    public String getErrorMessage() { return errorMessage; }
}