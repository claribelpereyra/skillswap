package com.uade.skillswap.application.usecases.publication;

import com.uade.skillswap.application.dto.publication.PublicationSummary;
import com.uade.skillswap.application.ports.in.publication.RecommendationStrategy;
import com.uade.skillswap.application.ports.out.publication.RecommendationMetadataQuery;
import com.uade.skillswap.domain.common.UserId;

import java.time.Clock;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class RecentPopularityRecommendationStrategy implements RecommendationStrategy {

    private final RecommendationMetadataQuery metadata;
    private final Clock clock;
    private final Duration window;

    public RecentPopularityRecommendationStrategy(RecommendationMetadataQuery metadata, Clock clock, Duration window) {
        this.metadata = Objects.requireNonNull(metadata, "The recommendation metadata query is required");
        this.clock = Objects.requireNonNull(clock, "The clock is required");
        this.window = Objects.requireNonNull(window, "The popularity window is required");
        if (window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("The popularity window must be positive");
        }
    }

    @Override
    public List<PublicationSummary> order(List<PublicationSummary> publications, UserId activeUserId) {
        Objects.requireNonNull(publications, "The publications list is required");
        Objects.requireNonNull(activeUserId, "The active user is required");
        if (publications.isEmpty()) {
            return List.of();
        }
        Set<Long> publicationIds = publications.stream()
                .map(PublicationSummary::id)
                .collect(Collectors.toUnmodifiableSet());
        Map<Long, Long> counts = metadata.recentBookingCounts(publicationIds, clock.instant().minus(window));
        Comparator<PublicationSummary> comparator = Comparator
                .comparingLong((PublicationSummary publication) -> counts.getOrDefault(publication.id(), 0L))
                .reversed()
                .thenComparing(PublicationSummary::publishedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(Comparator.comparingDouble(PublicationSummary::authorReputation).reversed())
                .thenComparing(Comparator.comparingLong(PublicationSummary::id).reversed());
        return publications.stream().sorted(comparator).toList();
    }
}
