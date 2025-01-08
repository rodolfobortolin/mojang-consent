package com.valiantys.service;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;

@Component
public class ConsentService implements ConsentServiceInterface {
    
    private static final Logger log = LoggerFactory.getLogger(ConsentService.class);
    private final UserManager userManager;
    private final ProjectManager projectManager;
    private final IssueService issueService;
    
    private static final String CONSENT_PROJECT_KEY = "CONSENT";
    private static final String TASK_ISSUE_TYPE = "Task";

    @Inject
    public ConsentService(
            @ComponentImport PluginSettingsFactory pluginSettingsFactory,
            @ComponentImport GroupManager groupManager,
            @ComponentImport UserManager userManager,
            @ComponentImport ProjectManager projectManager,
            @ComponentImport IssueService issueService) {
        this.userManager = userManager;
        this.projectManager = projectManager;
        this.issueService = issueService;
    }

    @Override
    public void saveDetailedConsent(@NotNull String username, String consentType) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }

        try {

            JSONObject status = new JSONObject();
            status.put("username", username);
            status.put("consentType", consentType);
            status.put("consentDate", System.currentTimeMillis());
            

            createConsentIssue(username, consentType);
            
        } catch (Exception e) {
            log.error("Error saving consent for user: {}", username, e);
            throw new RuntimeException("Failed to save consent", e);
        }
    }

    private void createConsentIssue(String username, String consentType) {
        try {
            Project consentProject = projectManager.getProjectByCurrentKey(CONSENT_PROJECT_KEY);
            if (consentProject == null) {
                log.error("Project with key {} not found", CONSENT_PROJECT_KEY);
                throw new RuntimeException("CONSENT project not found");
            }

            ApplicationUser consentingUser = userManager.getUserByName(username);
            if (consentingUser == null) {
                log.error("User not found: {}", username);
                throw new RuntimeException("User not found: " + username);
            }

            ApplicationUser reporter = userManager.getUserByName("rodolfobortolin");
            if (reporter == null) {
                log.error("Reporter user 'rodolfobortolin' not found");
                throw new RuntimeException("Reporter user 'rodolfobortolin' not found");
            }

            String summary = getConsentSummary(username, consentType);
            String description = buildConsentDescription(username, consentType, consentingUser);

            String issueTypeId = getTaskIssueTypeId();

            IssueInputParameters issueInputParameters = issueService.newIssueInputParameters();
            issueInputParameters
                .setProjectId(consentProject.getId())
                .setIssueTypeId(issueTypeId)
                .setSummary(summary)
                .setDescription(description)
                .setReporterId(reporter.getUsername()) 
                .setAssigneeId(reporter.getUsername()); 

            IssueService.CreateValidationResult validationResult = 
                issueService.validateCreate(reporter, issueInputParameters);

            if (!validationResult.isValid()) {
                log.error("Validation errors: {}", validationResult.getErrorCollection());
                log.error("Issue parameters: Project={}, IssueType={}, Reporter={}", 
                    consentProject.getKey(), issueTypeId, reporter.getUsername());
                throw new RuntimeException("Invalid issue creation parameters: " + 
                    validationResult.getErrorCollection().getErrorMessages());
            }

            IssueService.IssueResult issueResult = issueService.create(reporter, validationResult);
            
            if (!issueResult.isValid()) {
                log.error("Issue creation errors: {}", issueResult.getErrorCollection());
                throw new RuntimeException("Failed to create issue: " + 
                    issueResult.getErrorCollection().getErrorMessages());
            }

            log.info("Successfully created consent issue for user: {}", username);

        } catch (Exception e) {
            log.error("Error creating consent issue for user: {}. Error: {}", username, e.getMessage(), e);
        }
    }

    private String getConsentSummary(String username, String consentType) {
        switch (consentType) {
            case "full":
                return String.format("[Full Consent] Data Migration - %s", username);
            case "bugs_only":
                return String.format("[Bugs Only] Data Migration - %s", username);
            case "none":
                return String.format("[No Consent] Data Migration - %s", username);
            default:
                return String.format("[Unknown] Data Migration - %s", username);
        }
    }

    private String buildConsentDescription(String username, String consentType, ApplicationUser user) {
        StringBuilder description = new StringBuilder();
        description.append(String.format("User %s has made a decision regarding data migration consent.\n\n", username));
        
        description.append("*Consent Details:*\n");
        description.append(String.format("* Username: %s\n", username));
        description.append(String.format("* Email: %s\n", user.getEmailAddress()));
        description.append(String.format("* Consent Type: %s\n", getConsentTypeDescription(consentType)));
        description.append(String.format("* Date: %s\n\n", 
            new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date())));

        description.append("*Selected Option:*\n");
        description.append(getDetailedConsentDescription(consentType));
        
        return description.toString();
    }

    private String getConsentTypeDescription(String consentType) {
        switch (consentType) {
            case "full":
                return "Full Consent (Account and Bugs)";
            case "bugs_only":
                return "Bugs Only";
            case "none":
                return "No Consent (Anonymized)";
            default:
                return "Unknown";
        }
    }

    private String getDetailedConsentDescription(String consentType) {
        switch (consentType) {
            case "full":
                return "User has consented to transfer both their account and bugs with preserved identity. "
                     + "All personal information will be migrated to the new system.";
            case "bugs_only":
                return "User has consented to transfer only their bugs with preserved identity. "
                     + "Account information will not be migrated to the new system.";
            case "none":
                return "User has not consented to transfer personal information. "
                     + "Bugs will be transferred with anonymized personal data.";
            default:
                return "Unknown consent type";
        }
    }

    private String getTaskIssueTypeId() {
        return ComponentAccessor.getConstantsManager()
            .getAllIssueTypeObjects()
            .stream()
            .filter(issueType -> TASK_ISSUE_TYPE.equals(issueType.getName()))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Task issue type not found"))
            .getId();
    }
 
}