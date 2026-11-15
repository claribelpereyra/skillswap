package com.uade.skillswap.application.usecases.publication;

import com.uade.skillswap.application.common.Page;
import com.uade.skillswap.application.common.PageRequest;
import com.uade.skillswap.application.dto.publication.PublicationSummary;
import com.uade.skillswap.application.dto.publication.SearchFilters;
import com.uade.skillswap.application.ports.in.publication.RecommendPublicationsUseCase;
import com.uade.skillswap.application.ports.in.publication.RecommendationStrategy;
import com.uade.skillswap.application.ports.in.publication.SearchPublicationsUseCase;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class RecommendPublicationsService implements RecommendPublicationsUseCase {

    private final SearchPublicationsUseCase search;

    public RecommendPublicationsService(SearchPublicationsUseCase search) {
        this.search = Objects.requireNonNull(search, "The search use case is required");
    }

    @Override
    public Page<PublicationSummary> recommend(SearchFilters filters, RecommendationStrategy strategy) {
        Objects.requireNonNull(filters, "The search filters are required");
        Objects.requireNonNull(strategy, "The recommendation strategy is required");

        PageRequest candidatePageRequest = new PageRequest(0, PageRequest.MAX_SIZE);
        Page<PublicationSummary> firstPage = search.search(filters.withPage(candidatePageRequest));
        List<PublicationSummary> candidates = new ArrayList<>(firstPage.items());
        for (int pageNumber = 1; pageNumber < firstPage.totalPages(); pageNumber++) {
            Page<PublicationSummary> page = search.search(filters.withPage(
                    new PageRequest(pageNumber, PageRequest.MAX_SIZE)));
            candidates.addAll(page.items());
        }

        List<PublicationSummary> ordered = strategy.order(List.copyOf(candidates), filters);
        long offset = filters.page().offset();
        int start = (int) Math.min(offset, ordered.size());
        int end = (int) Math.min(offset + filters.page().size(), ordered.size());
        return new Page<>(ordered.subList(start, end), filters.page(), ordered.size());
    }
}
