package com.uade.skillswap.domain.profile;

import com.uade.skillswap.domain.common.BusinessRuleException;

public class InactiveProfileException extends BusinessRuleException {

    public InactiveProfileException(String message) {
        super(message);
    }
}
