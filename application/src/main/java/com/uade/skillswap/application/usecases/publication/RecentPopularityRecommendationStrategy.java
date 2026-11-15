package com.uade.skillswap.application.usecases.publication;

import com.uade.skillswap.application.ports.in.publication.PublicationSummary;
import com.uade.skillswap.application.ports.in.publication.RecommendationStrategy;
import com.uade.skillswap.application.ports.in.publication.SearchFilters;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class RecentPopularityRecommendationStrategy implements RecommendationStrategy {

    @Override
    public List<PublicationSummary> order(List<PublicationSummary> publications, SearchFilters filters) {
        Objects.requireNonNull(publications, "The publications list is required");
        Comparator<PublicationSummary> comparator = Comparator
                .comparing(PublicationSummary::publishedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(Comparator.comparingDouble(PublicationSummary::authorReputation).reversed())
                .thenComparing(Comparator.comparingLong(PublicationSummary::id).reversed());
        return publications.stream().sorted(comparator).toList();
    }
}
