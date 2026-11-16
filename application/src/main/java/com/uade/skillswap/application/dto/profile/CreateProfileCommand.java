package com.uade.skillswap.application.dto.profile;

import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.profile.AccountType;

import java.time.ZoneId;

public record CreateProfileCommand(
        UserId userId,
        String displayName,
        String bio,
        ZoneId timeZone,
        AccountType accountType) {
}
