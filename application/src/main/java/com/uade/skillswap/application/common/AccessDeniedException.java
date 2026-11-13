package com.uade.skillswap.application.common;

import com.uade.skillswap.domain.common.UserId;

public class AccessDeniedException extends RuntimeException {

    public AccessDeniedException(UserId user, String operation) {
        super("User " + user + " is not allowed to " + operation);
    }
}
