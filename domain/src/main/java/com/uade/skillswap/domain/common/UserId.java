package com.uade.skillswap.domain.common;

public record UserId(String value) {

    public UserId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("A userId cannot be blank");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
