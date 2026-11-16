package com.uade.skillswap.application.dto.profile;

import com.uade.skillswap.application.common.Page;
import com.uade.skillswap.application.dto.publication.PublicationView;
import com.uade.skillswap.application.dto.rating.RatingView;
import com.uade.skillswap.domain.profile.AccountType;
import com.uade.skillswap.domain.profile.PublicProfileSummary;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

public record PublicProfileView(
        String userId,
        String displayName,
        String bio,
        AccountType accountType,
        double reputation,
        int ratingCount,
        Set<Long> skillIds,
        String availability,
        boolean active,
        Page<PublicationView> activePublications,
        Page<RatingView> recentRatings) {

    public static PublicProfileView from(PublicProfileSummary summary,
                                         Page<PublicationView> activePublications,
                                         Page<RatingView> recentRatings) {
        return new PublicProfileView(
                summary.userId().value(),
                summary.displayName(),
                summary.bio(),
                summary.accountType(),
                summary.reputation(),
                summary.ratingCount(),
                Collections.unmodifiableSet(new TreeSet<>(summary.skillIds())),
                summary.availability().description(),
                summary.active(),
                activePublications,
                recentRatings
        );
    }
}
