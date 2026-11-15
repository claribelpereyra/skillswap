package com.uade.skillswap.application.usecases.publication;

import com.uade.skillswap.application.ports.in.publication.PublicationSummary;
import com.uade.skillswap.application.ports.in.publication.RecommendationStrategy;
import com.uade.skillswap.application.ports.in.publication.SearchFilters;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class SkillAffinityRecommendationStrategy implements RecommendationStrategy {

    @Override
    public List<PublicationSummary> order(List<PublicationSummary> publications, SearchFilters filters) {
        Objects.requireNonNull(publications, "The publications list is required");
        String skill = filters == null ? null : filters.skill();
        Comparator<PublicationSummary> comparator = Comparator
                .comparingInt((PublicationSummary publication) -> hasSkill(publication, skill) ? 0 : 1)
                .thenComparing(Comparator.comparingDouble(PublicationSummary::authorReputation).reversed())
                .thenComparing(Comparator.comparing(PublicationSummary::publishedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .thenComparing(Comparator.comparingLong(PublicationSummary::id).reversed());
        return publications.stream().sorted(comparator).toList();
    }

    private static boolean hasSkill(PublicationSummary publication, String skill) {
        return skill != null && !skill.isBlank() && publication.tags().contains(skill);
    }
}
