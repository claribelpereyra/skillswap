package com.uade.skillswap.controllers.profile;

public record CreateProfileForm(
        String displayName,
        String bio,
        String timeZone,
        String accountType) {
}
