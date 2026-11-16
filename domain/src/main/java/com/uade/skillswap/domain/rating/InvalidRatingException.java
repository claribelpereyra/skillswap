package com.uade.skillswap.domain.rating;

import com.uade.skillswap.domain.common.BusinessRuleException;

public class InvalidRatingException extends BusinessRuleException {

    public InvalidRatingException(String message) {
        super(message);
    }
}
