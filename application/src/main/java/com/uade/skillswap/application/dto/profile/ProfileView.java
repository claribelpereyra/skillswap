package com.uade.skillswap.application.dto.profile;

import com.uade.skillswap.domain.profile.AccountType;
import com.uade.skillswap.domain.profile.Profile;

import java.time.Instant;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

public record ProfileView(
        Long id,
        String userId,
        String displayName,
        String bio,
        String timeZone,
        AccountType accountType,
        double reputation,
        int ratingCount,
        Set<Long> skillIds,
        Set<String> interests,
        String availability,
        boolean active,
        Instant createdAt) {

    public static ProfileView from(Profile profile) {
        Set<Long> sortedSkillIds = new TreeSet<>(profile.getSkillIds());
        Set<String> interestNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        interestNames.addAll(profile.getInterests());
        return new ProfileView(
                profile.getId(),
                profile.getUserId().value(),
                profile.getDisplayName(),
                profile.getBio(),
                profile.getTimeZone().getId(),
                profile.getAccountType(),
                profile.getReputation(),
                profile.getRatingCount(),
                Collections.unmodifiableSet(sortedSkillIds),
                Collections.unmodifiableSet(interestNames),
                profile.getAvailability().description(),
                profile.isActive(),
                profile.getCreatedAt()
        );
    }
}
