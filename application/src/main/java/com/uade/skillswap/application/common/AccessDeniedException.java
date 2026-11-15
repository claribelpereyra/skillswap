package com.uade.skillswap.application.common;

import com.uade.skillswap.domain.common.UserId;

public class AccessDeniedException extends ApplicationException {

    public AccessDeniedException(UserId user, String operation) {
        super("User " + user + " is not allowed to " + operation);
    }

    public AccessDeniedException(String message) {
        super(message);
    }
}
