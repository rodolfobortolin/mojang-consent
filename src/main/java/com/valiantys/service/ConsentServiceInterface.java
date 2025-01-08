package com.valiantys.service;

import javax.validation.constraints.NotNull;
import com.valiantys.model.ConsentStatus;

public interface ConsentServiceInterface {
    void saveConsent(@NotNull String username, boolean consent);
    ConsentStatus getConsentStatus(@NotNull String username);
}