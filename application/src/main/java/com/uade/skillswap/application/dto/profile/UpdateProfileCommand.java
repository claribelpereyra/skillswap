package com.uade.skillswap.application.dto.profile;

import com.uade.skillswap.domain.common.UserId;

import java.time.ZoneId;

public record UpdateProfileCommand(
        UserId userId,
        String displayName,
        String bio,
        ZoneId timeZone,
        String availability) {
}
