package com.uade.skillswap.application.usecases.publication;

import com.uade.skillswap.application.dto.publication.PublicationSummary;
import com.uade.skillswap.application.dto.publication.SearchFilters;
import com.uade.skillswap.application.ports.out.publication.RecommendationMetadataQuery;
import com.uade.skillswap.application.ports.in.publication.RecommendationStrategy;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class SkillAffinityRecommendationStrategy implements RecommendationStrategy {

    private final RecommendationMetadataQuery metadata;

    public SkillAffinityRecommendationStrategy(RecommendationMetadataQuery metadata) {
        this.metadata = Objects.requireNonNull(metadata, "The recommendation metadata query is required");
    }

    @Override
    public List<PublicationSummary> order(List<PublicationSummary> publications, SearchFilters filters) {
        Objects.requireNonNull(publications, "The publications list is required");
        Objects.requireNonNull(filters, "The search filters are required");
        Set<String> profileSkills = filters.activeUserId() == null || filters.activeUserId().isBlank()
                ? Set.of()
                : metadata.skillsAndInterestsFor(filters.activeUserId()).stream()
                .map(SkillAffinityRecommendationStrategy::normalize)
                .filter(skill -> !skill.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
        Comparator<PublicationSummary> comparator = Comparator
                .comparingInt((PublicationSummary publication) -> -affinity(publication, profileSkills))
                .thenComparing(Comparator.comparingDouble(PublicationSummary::authorReputation).reversed())
                .thenComparing(Comparator.comparing(PublicationSummary::publishedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .thenComparing(Comparator.comparingLong(PublicationSummary::id).reversed());
        return publications.stream().sorted(comparator).toList();
    }

    private static int affinity(PublicationSummary publication, Set<String> profileSkills) {
        return (int) publication.tags().stream()
                .map(SkillAffinityRecommendationStrategy::normalize)
                .filter(profileSkills::contains)
                .distinct()
                .count();
    }

    private static String normalize(String value) {
        return value.strip().toLowerCase(Locale.ROOT);
    }
}
