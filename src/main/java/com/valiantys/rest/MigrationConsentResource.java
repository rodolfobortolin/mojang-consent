package com.valiantys.rest;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.user.ApplicationUser;
import com.valiantys.service.ConsentServiceInterface;
import com.valiantys.service.EmailServiceInterface;

@Path("/consent")
@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
public class MigrationConsentResource {
    private final ConsentServiceInterface consentService;
    private final EmailServiceInterface emailService;
    private static final Logger log = LoggerFactory.getLogger(MigrationConsentResource.class);


    public MigrationConsentResource(ConsentServiceInterface consentService, 
                                  EmailServiceInterface emailService) {
        this.consentService = consentService;
        this.emailService = emailService;
    }

    @POST
    public Response saveConsent(ConsentRequest request) {
        try {
            // Validate request
            if (request == null || request.getUsername() == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "Invalid request parameters"))
                    .build();
            }

            // Save the consent
            consentService.saveConsent(request.getUsername(), request.isConsent());

            // Get the user and send appropriate email notification
            ApplicationUser user = ComponentAccessor.getUserManager().getUserByName(request.getUsername());
            if (user != null) {
                if (request.isConsent()) {
                    // Send confirmation email for providing consent
                    emailService.sendConsentConfirmationEmail(user);
                } else {
                    // Send notification for declining consent
                    emailService.sendConsentDeclinedEmail(user);
                }
            } else {
                log.warn("User not found for username: {}", request.getUsername());
            }

            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Consent saved successfully");
            return Response.ok(response).build();

        } catch (Exception e) {
            return Response.serverError()
                .entity(Map.of("message", "Failed to save consent: " + e.getMessage()))
                .build();
        }
    }
}

class ConsentRequest {
    private String username;
    private boolean consent;
    
    // Getters and setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public boolean isConsent() { return consent; }
    public void setConsent(boolean consent) { this.consent = consent; }
}