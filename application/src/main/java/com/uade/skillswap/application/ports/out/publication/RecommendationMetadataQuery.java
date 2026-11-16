package com.uade.skillswap.application.ports.out.publication;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

public interface RecommendationMetadataQuery {

    Set<String> skillsAndInterestsFor(String userId);

    Map<Long, Long> recentBookingCounts(Set<Long> publicationIds, Instant since);
}
