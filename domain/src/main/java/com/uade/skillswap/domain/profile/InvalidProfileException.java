package com.uade.skillswap.domain.profile;

import com.uade.skillswap.domain.common.BusinessRuleException;

public class InvalidProfileException extends BusinessRuleException {

    public InvalidProfileException(String message) {
        super(message);
    }
}
