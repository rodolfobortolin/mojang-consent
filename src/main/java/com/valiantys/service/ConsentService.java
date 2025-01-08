package com.valiantys.service;

import com.atlassian.crowd.embedded.api.Group;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import org.springframework.stereotype.Component;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.JSONObject;
import com.valiantys.model.ConsentStatus;

@Component
public class ConsentService implements ConsentServiceInterface {
    
    private static final Logger log = LoggerFactory.getLogger(ConsentService.class);
    private final PluginSettings pluginSettings;
    private final GroupManager groupManager;
    private final UserManager userManager;

    // Constructor with dependency injection for required services
    @Inject
    public ConsentService(
            @ComponentImport PluginSettingsFactory pluginSettingsFactory,
            @ComponentImport GroupManager groupManager,
            @ComponentImport UserManager userManager) {
        this.pluginSettings = pluginSettingsFactory.createGlobalSettings();
        this.groupManager = groupManager;
        this.userManager = userManager;
    }

    @Override
    public void saveConsent(@NotNull String username, boolean consent) {
        // Input validation
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }

        try {
            // Create a JSON object to store consent information
            JSONObject status = new JSONObject();
            status.put("username", username);
            status.put("hasConsented", consent);
            status.put("consentDate", System.currentTimeMillis());
            
            // Store the consent status in plugin settings
            pluginSettings.put("migration.consent." + username, status.toString());

            
        } catch (Exception e) {
            log.error("Error saving consent for user: {}", username, e);
            throw new RuntimeException("Failed to save consent", e);
        }
    }

    

    @Override
    public ConsentStatus getConsentStatus(@NotNull String username) {
        // Input validation
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }

        try {
            // Retrieve the stored consent status
            String jsonStatus = (String) pluginSettings.get("migration.consent." + username);
            if (jsonStatus == null) {
                return null;
            }

            // Parse the JSON and create a ConsentStatus object
            JSONObject json = new JSONObject(jsonStatus);
            ConsentStatus status = new ConsentStatus();
            status.setUsername(json.getString("username"));
            status.setHasConsented(json.getBoolean("hasConsented"));
            status.setConsentDate(json.getLong("consentDate"));
            
            return status;
        } catch (Exception e) {
            log.error("Error retrieving consent status for user: {}", username, e);
            return null;
        }
    }
}