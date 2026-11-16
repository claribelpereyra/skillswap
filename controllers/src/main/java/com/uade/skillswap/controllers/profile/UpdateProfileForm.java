package com.uade.skillswap.controllers.profile;

public record UpdateProfileForm(
        String displayName,
        String bio,
        String timeZone,
        String availability) {
}
