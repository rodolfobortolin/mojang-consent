package com.valiantys.servlet;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.atlassian.crowd.embedded.api.Group;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserUtil;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.valiantys.model.ConsentStatus;
import com.valiantys.service.ConsentServiceInterface;

@Component
public class MigrationConsentServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(MigrationConsentServlet.class);

    private final ConsentServiceInterface consentService;
    private final TemplateRenderer templateRenderer;
    private final LoginUriProvider loginUriProvider;
    private final JiraAuthenticationContext authContext;
    private final ApplicationProperties applicationProperties;

    // For adding user to a group
    private final UserUtil userUtil;
    // We use GroupManager instead of calling userUtil.getGroupObject(...)
    private final GroupManager groupManager;

    // Example path to your Velocity template if needed
    private static final String TEMPLATE_PATH = "/templates/migration-consent.vm";

    public MigrationConsentServlet(
            @ComponentImport ConsentServiceInterface consentService,
            @ComponentImport TemplateRenderer templateRenderer,
            @ComponentImport LoginUriProvider loginUriProvider,
            @ComponentImport JiraAuthenticationContext authContext,
            @ComponentImport ApplicationProperties applicationProperties
    ) {
        if (consentService == null 
            || templateRenderer == null 
            || loginUriProvider == null 
            || authContext == null 
            || applicationProperties == null
        ) {
            throw new IllegalArgumentException("Required dependencies cannot be null");
        }

        this.consentService = consentService;
        this.templateRenderer = templateRenderer;
        this.loginUriProvider = loginUriProvider;
        this.authContext = authContext;
        this.applicationProperties = applicationProperties;

        // Retrieve components to manage user/group
        this.userUtil = ComponentAccessor.getUserUtil();
        this.groupManager = ComponentAccessor.getGroupManager();
    }

    /**
     * Provides a fallback if the base URL is null or empty in applicationProperties.
     */
    private String getBaseUrl() {
        try {
            String url = applicationProperties.getString("jira.baseurl");
            if (url == null || url.trim().isEmpty()) {
                // fallback
                url = "http://localhost:8080";
                log.warn("Base URL from applicationProperties was null or empty. Using fallback: {}", url);
            }
            return url;
        } catch (Exception e) {
            log.warn("Could not get base URL from application properties", e);
            return "http://localhost:8080"; // fallback
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        ApplicationUser user = authContext.getLoggedInUser();
        if (user == null) {
            redirectToLogin(req, resp);
            return;
        }

        resp.setContentType("text/html;charset=UTF-8");
        try {
            Map<String, Object> context = createTemplateContext(user);
            // Render the Velocity template if needed
            templateRenderer.render(TEMPLATE_PATH, context, resp.getWriter());
        } catch (Exception e) {
            log.error("Error rendering template for user: {}", user.getUsername(), e);
            handleRenderError(resp);
        }
    }

    /**
     * Used to build the Velocity context if you're rendering a template.
     */
    private Map<String, Object> createTemplateContext(ApplicationUser user) {
        Map<String, Object> context = new HashMap<>();
        context.put("user", user);
        context.put("baseUrl", getBaseUrl());

        try {
            // We assume getConsentStatus(...) returns a ConsentStatus object
            ConsentStatus cs = consentService.getConsentStatus(user.getUsername());
            boolean status = (cs != null) && cs.isHasConsented();
            context.put("consentStatus", status);
        } catch (Exception e) {
            log.error("Error getting consent status for user: {}", user.getUsername(), e);
            context.put("consentStatus", false);
        }

        return context;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        ApplicationUser user = authContext.getLoggedInUser();
        if (user == null) {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication required");
            return;
        }

        try {
            // Grab the "consent" parameter from the form
            String consentParam = req.getParameter("consent");
            // We interpret "true" or "on" as user consent = true
            boolean consent = consentParam != null 
                              && (consentParam.equalsIgnoreCase("true") 
                                  || consentParam.equalsIgnoreCase("on"));

            // Save the consent status (true/false)
            consentService.saveConsent(user.getUsername(), consent);

            //Build JSON response
            JSONObject json = new JSONObject();
            json.put("status", "success");
            json.put("message", consent
                ? "Consent provided successfully. User added to group."
                : "Consent declined."
            );

            sendJsonResponse(resp, HttpServletResponse.SC_OK, json);

        } catch (Exception e) {
            log.error("Error processing consent for user: {}", user.getUsername(), e);

            JSONObject errorJson = new JSONObject();
            errorJson.put("status", "error");
            errorJson.put("message", "Failed to process consent");

            sendJsonResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, errorJson);
        }
    }

    /**
     * Utility to send JSON responses.
     */
    private void sendJsonResponse(HttpServletResponse resp, int status, JSONObject json) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json;charset=UTF-8");
        resp.getWriter().write(json.toString());
    }

    private void handleRenderError(HttpServletResponse resp) throws IOException {
        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        resp.getWriter().write("Error rendering the consent form. Please try again later.");
    }

    /**
     * Redirect to login if user is not authenticated.
     */
    private void redirectToLogin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        URI loginUri = loginUriProvider.getLoginUri(getUri(request));
        response.sendRedirect(loginUri.toString());
    }

    private URI getUri(HttpServletRequest request) {
        StringBuffer builder = request.getRequestURL();
        String query = request.getQueryString();
        if (query != null && !query.isEmpty()) {
            builder.append("?").append(query);
        }
        return URI.create(builder.toString());
    }
}
