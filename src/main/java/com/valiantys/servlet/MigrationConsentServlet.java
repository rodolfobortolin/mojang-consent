package com.valiantys.servlet;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.atlassian.crowd.embedded.api.Group;
import com.atlassian.jira.bc.user.UserService;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserUtil;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.valiantys.service.ConsentServiceInterface;
import com.valiantys.service.EmailServiceInterface;

@Component
public class MigrationConsentServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(MigrationConsentServlet.class);

    private final ConsentServiceInterface consentService;
    private final EmailServiceInterface emailService;
    private final TemplateRenderer templateRenderer;
    private final LoginUriProvider loginUriProvider;
    private final JiraAuthenticationContext authContext;
    private final ApplicationProperties applicationProperties;
    private final GroupManager groupManager;
    private final UserUtil userUtil;

    private static final String TEMPLATE_PATH = "/templates/migration-consent.vm";
    private static final String SUCCESS_TEMPLATE_PATH = "/templates/consent-success.vm";
    private static final String ERROR_TEMPLATE_PATH = "/templates/error.vm";

    // Group names for different consent options
    private static final String FULL_CONSENT_GROUP = "migration-full-consent";
    private static final String BUGS_ONLY_GROUP = "migration-bugs-only";
    private static final String NO_CONSENT_GROUP = "migration-no-consent";

    public MigrationConsentServlet(
            @ComponentImport ConsentServiceInterface consentService,
            @ComponentImport EmailServiceInterface emailService,
            @ComponentImport TemplateRenderer templateRenderer,
            @ComponentImport LoginUriProvider loginUriProvider,
            @ComponentImport JiraAuthenticationContext authContext,
            @ComponentImport ApplicationProperties applicationProperties,
            @ComponentImport GroupManager groupManager,
            @ComponentImport UserUtil userUtil) {
        
        this.consentService = consentService;
        this.emailService = emailService;
        this.templateRenderer = templateRenderer;
        this.loginUriProvider = loginUriProvider;
        this.authContext = authContext;
        this.applicationProperties = applicationProperties;
        this.groupManager = groupManager;
        this.userUtil = userUtil;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        ApplicationUser user = authContext.getLoggedInUser();
        if (user == null) {
            redirectToLogin(req, resp);
            return;
        }

        resp.setContentType("text/html;charset=UTF-8");
        Map<String, Object> context = createTemplateContext(user);
        templateRenderer.render(TEMPLATE_PATH, context, resp.getWriter());
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        ApplicationUser user = authContext.getLoggedInUser();
        if (user == null) {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication required");
            return;
        }

        try {
            String consentOption = req.getParameter("consentOption");
            if (consentOption == null || consentOption.trim().isEmpty()) {
                throw new IllegalArgumentException("Consent option is required");
            }

            try {
                removeFromMigrationGroups(user);

                switch (consentOption) {
                    case "full":
                        processFullConsent(user);
                        break;
                    case "bugs_only":
                        processBugsOnlyConsent(user);
                        break;
                    case "none":
                        processNoConsent(user);
                        break;
                    default:
                        throw new IllegalArgumentException("Invalid consent option: " + consentOption);
                }
            } catch (Exception e) {
                log.error("Error processing group membership for user: {}", user.getUsername(), e);
            }

            try {
                consentService.saveDetailedConsent(user.getUsername(), consentOption);
            } catch (Exception e) {
                log.error("Error saving consent and creating issue for user: {}", user.getUsername(), e);
            }

            try {
                emailService.sendConsentConfirmationEmail(user);
            } catch (Exception e) {
                log.error("Error sending confirmation email to user: {}", user.getUsername(), e);
            }

            Map<String, Object> successContext = new HashMap<>();
            successContext.put("consentOption", consentOption);
            successContext.put("date", new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new java.util.Date()));
            successContext.put("baseUrl", applicationProperties.getString("jira.baseurl"));

            resp.setContentType("text/html;charset=UTF-8");
            templateRenderer.render(SUCCESS_TEMPLATE_PATH, successContext, resp.getWriter());

        } catch (Exception e) {
            log.error("Critical error processing consent for user: {}", user.getUsername(), e);
            handleError(req, resp, "An error occurred while processing your consent. Please contact support if the problem persists.");
        }
    }

    private void processFullConsent(ApplicationUser user) throws Exception {
        addToGroup(user, FULL_CONSENT_GROUP);
    }

    private void processBugsOnlyConsent(ApplicationUser user) throws Exception {
        addToGroup(user, BUGS_ONLY_GROUP);
    }

    private void processNoConsent(ApplicationUser user) throws Exception {
        addToGroup(user, NO_CONSENT_GROUP);
    }

    private void removeFromMigrationGroups(ApplicationUser user) {
        String[] groups = {FULL_CONSENT_GROUP, BUGS_ONLY_GROUP, NO_CONSENT_GROUP};
        for (String groupName : groups) {
            try {
                Group group = groupManager.getGroup(groupName);
                if (group != null && groupManager.isUserInGroup(user, group)) {
                    userUtil.removeUserFromGroup(group, user);
                }
            } catch (Exception e) {
                log.error("Error removing user {} from group {}", user.getUsername(), groupName, e);
            }
        }
    }

    private void addToGroup(ApplicationUser user, String groupName) {
        try {
            Group group = groupManager.getGroup(groupName);
            if (group == null) {
            	throw new RuntimeException("Failed to add user to group: " + groupName);
            }
            userUtil.addUserToGroup(group, user);
        } catch (Exception e) {
            log.error("Error adding user {} to group {}", user.getUsername(), groupName, e);
            throw new RuntimeException("Failed to add user to group: " + groupName, e);
        }
    }

    private Map<String, Object> createTemplateContext(ApplicationUser user) {
        Map<String, Object> context = new HashMap<>();
        context.put("user", user);
        context.put("baseUrl", applicationProperties.getString("jira.baseurl"));
        return context;
    }

    private void handleError(HttpServletRequest req, HttpServletResponse resp, String errorMessage) throws IOException {
        Map<String, Object> context = new HashMap<>();
        context.put("errorMessage", errorMessage);
        resp.setContentType("text/html;charset=UTF-8");
        templateRenderer.render(ERROR_TEMPLATE_PATH, context, resp.getWriter());
    }

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