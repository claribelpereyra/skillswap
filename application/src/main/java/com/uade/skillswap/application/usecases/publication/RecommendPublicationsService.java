package com.uade.skillswap.application.usecases.publication;

import com.uade.skillswap.application.common.Page;
import com.uade.skillswap.application.ports.in.publication.PublicationSummary;
import com.uade.skillswap.application.ports.in.publication.RecommendPublicationsUseCase;
import com.uade.skillswap.application.ports.in.publication.RecommendationStrategy;
import com.uade.skillswap.application.ports.in.publication.SearchFilters;
import com.uade.skillswap.application.ports.in.publication.SearchPublicationsUseCase;

import java.util.List;
import java.util.Objects;

public class RecommendPublicationsService implements RecommendPublicationsUseCase {

    private final SearchPublicationsUseCase search;

    public RecommendPublicationsService(SearchPublicationsUseCase search) {
        this.search = Objects.requireNonNull(search, "The search use case is required");
    }

    @Override
    public Page<PublicationSummary> recommend(SearchFilters filters, RecommendationStrategy strategy) {
        Page<PublicationSummary> candidates = search.search(filters);
        List<PublicationSummary> ordered = strategy == null
                ? List.copyOf(candidates.items())
                : strategy.order(candidates.items(), filters);
        return new Page<>(ordered, candidates.request(), candidates.totalItems());
    }
}
