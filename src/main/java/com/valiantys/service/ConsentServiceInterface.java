package com.valiantys.service;

import javax.validation.constraints.NotNull;

public interface ConsentServiceInterface {
	void saveDetailedConsent(@NotNull String username, String consentType);
}