package com.uade.skillswap.domain.profile;

import com.uade.skillswap.domain.common.UserId;

import java.util.Set;

public record PublicProfileSummary(
        UserId userId,
        String displayName,
        String bio,
        AccountType accountType,
        double reputation,
        int ratingCount,
        Set<Long> skillIds,
        Availability availability,
        boolean active
) {

    public PublicProfileSummary {
        skillIds = Set.copyOf(skillIds);
    }
}
