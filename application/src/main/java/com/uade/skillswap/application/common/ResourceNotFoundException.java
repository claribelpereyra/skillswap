package com.uade.skillswap.application.common;

public class ResourceNotFoundException extends ApplicationException {

    public ResourceNotFoundException(String resource, Object id) {
        super(resource + " not found with id " + id);
    }
}
