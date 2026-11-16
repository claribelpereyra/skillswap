package com.uade.skillswap.domain.profile;

public record Availability(String description) {

    public static final Availability UNSPECIFIED = new Availability("Not specified");

    public Availability {
        if (description == null || description.isBlank()) {
            description = "Not specified";
        } else {
            description = description.trim();
        }
    }
}
