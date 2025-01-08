AJS.$(document).ready(function() {
    AJS.$('#consent-form').submit(function(e) {
        e.preventDefault();
        
        // 1) Determine if the user checked the checkbox
        var consent = AJS.$('#consent-checkbox').is(':checked');
        
        // 2) Send AJAX to the REST endpoint
        AJS.$.ajax({
            url: AJS.contextPath() + '/rest/migration-consent/1.0/consent',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify({ 
                consent: consent,
                // If you need the logged-in username
                username: AJS.params.loggedInUser.name
            }),
            success: function(response) {
                // 3) Show success banner only
                AJS.flag({
                    type: 'success',
                    title: 'Thank You',
                    body: response.message || 'Your consent has been recorded successfully.',
                    close: 'auto'
                });
                // Do NOT redirect anywhere; stay on the same page
            },
            error: function(xhr) {
                var message = (xhr.responseJSON && xhr.responseJSON.message) 
                              ? xhr.responseJSON.message 
                              : 'Failed to save consent';
                AJS.flag({
                    type: 'error',
                    title: 'Error',
                    body: message,
                    close: 'auto'
                });
            }
        });
    });
});